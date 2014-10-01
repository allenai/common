package org.allenai.datastore

import org.allenai.common.Resource
import org.allenai.common.Logging

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.commons.io.FileUtils

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.{ ZipEntry, ZipOutputStream, ZipFile }

class Datastore(val s3config: S3Config) extends Logging {
  private val systemTempDir = Paths.get(System.getProperty("java.io.tmpdir"))
  private val cacheDir = systemTempDir.resolve("ai2-datastore-cache").resolve(s3config.bucket)
  Files.createDirectories(cacheDir)

  def name = s3config.bucket

  /** Identifies a single version of a file or directory in the datastore */
  case class Locator(group: String, name: String, version: Int) {
    require(version > 0)

    def nameWithVersion = {
      val lastDotIndex = name.lastIndexOf('.')
      if (lastDotIndex < 0) {
        s"name-v$version"
      } else {
        name.substring(0, lastDotIndex) + s"-v$version" + name.substring(lastDotIndex)
      }
    }
    def s3key = s"$group/$nameWithVersion"
    def localCacheKey = s3key
    def flatLocalCacheKey = localCacheKey.replace('/', '%')
    def localCachePath = cacheDir.resolve(localCacheKey)
    def lockfilePath = cacheDir.resolve(localCacheKey + ".lock")
    def zipLocator = copy(name = name + ".zip")
  }

  class DoesNotExistException(
    locator: Locator,
    cause: Throwable) extends Exception(
    s"${locator.s3key} does not exist in the $name datastore",
    cause)

  private def getS3Object(key: String) =
    s3config.service.getObject(s3config.bucket, key).getObjectContent

  private def waitForLockfile(lockfile: Path): Unit = {
    // TODO: Use watch interfaces instead of busy wait
    val start = System.currentTimeMillis()
    while (Files.exists(lockfile)) {
      val message = s"Waiting for lockfile at $lockfile}"
      if (System.currentTimeMillis() - start > 60 * 1000)
        logger.warn(message)
      else
        logger.info(message)
      Thread.sleep(1000)
    }
  }

  private def tryCreateFile(file: Path): Boolean = {
    try {
      Files.createFile(file)
      true
    } catch {
      case _: FileAlreadyExistsException => false
    }
  }

  def filePath(group: String, name: String, version: Int): Path =
    filePath(Locator(group, name, version))
  def filePath(locator: Locator): Path = {
    waitForLockfile(locator.lockfilePath)

    if (!Files.isRegularFile(locator.localCachePath)) {
      Files.createDirectories(locator.lockfilePath.getParent)
      val created = tryCreateFile(locator.lockfilePath)
      if (!created) {
        // someone else started creating this in the meantime
        filePath(locator)
      } else {
        TempCleanup.remember(locator.lockfilePath)
        try {
          // We're downloading to a temp file first. If we were downloading into
          // the file directly, and we died half-way through the download, we'd
          // leave half a file, and that's not good.
          val tempFile =
            Files.createTempFile("ai2-datastore-" + locator.flatLocalCacheKey, ".tmp")
          TempCleanup.remember(tempFile)
          try {
            Resource.using(getS3Object(locator.s3key)) { s3object =>
              Files.copy(s3object, tempFile, StandardCopyOption.REPLACE_EXISTING)
            }
          } catch {
            case e: AmazonS3Exception if e.getErrorCode == "NoSuchKey" =>
              throw new DoesNotExistException(locator, e)
          }
          Files.createDirectories(locator.localCachePath.getParent)
          Files.move(tempFile, locator.localCachePath)
          TempCleanup.forget(tempFile)
        } finally {
          Files.delete(locator.lockfilePath)
          TempCleanup.forget(locator.lockfilePath)
        }
        locator.localCachePath
      }
    } else {
      locator.localCachePath
    }
  }

  def directoryPath(group: String, name: String, version: Int): Path =
    directoryPath(Locator(group, name, version))
  def directoryPath(locator: Locator): Path = {
    waitForLockfile(locator.lockfilePath)

    if (Files.isDirectory(locator.localCachePath)) {
      locator.localCachePath
    } else {
      val created = tryCreateFile(locator.lockfilePath)
      if (!created) {
        directoryPath(locator)
      } else {
        TempCleanup.remember(locator.lockfilePath)
        try {
          val tempDir =
            Files.createTempDirectory("ai2-datastore-" + locator.flatLocalCacheKey)
          TempCleanup.remember(tempDir)

          // download and extract the zip file to the directory
          Resource.using(new ZipFile(filePath(locator.zipLocator).toFile)) { zipFile =>
            val entries = zipFile.entries()
            while (entries.hasMoreElements) {
              val entry = entries.nextElement()
              val pathForEntry = tempDir.resolve(entry.getName)
              Files.createDirectories(pathForEntry.getParent)
              Resource.using(zipFile.getInputStream(entry))(Files.copy(_, pathForEntry))
            }
          }

          // move the directory where it belongs
          Files.move(tempDir, locator.localCachePath)
          TempCleanup.forget(tempDir)
        } finally {
          Files.delete(locator.lockfilePath)
          TempCleanup.forget(locator.lockfilePath)
        }

        locator.localCachePath
      }
    }
  }

  def publishFile(file: String, group: String, name: String, version: Int): Unit =
    publishFile(Paths.get(file), group, name, version)
  def publishFile(file: Path, group: String, name: String, version: Int): Unit =
    publishFile(file, Locator(group, name, version))
  def publishFile(file: Path, locator: Locator): Unit = {
    s3config.service.putObject(s3config.bucket, locator.s3key, file.toFile)
  }

  def publishDirectory(path: Path, group: String, name: String, version: Int): Unit =
    publishDirectory(path, Locator(group, name, version))
  def publishDirectory(path: Path, locator: Locator): Unit = {
    val zipFile =
      Files.createTempFile(
        locator.flatLocalCacheKey,
        ".ai2-datastore.upload.zip")
    TempCleanup.remember(zipFile)
    try {
      Resource.using(new ZipOutputStream(Files.newOutputStream(zipFile))) { zip =>
        Files.walkFileTree(path, new SimpleFileVisitor[Path] {
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            zip.putNextEntry(new ZipEntry(path.relativize(file).toString))
            Files.copy(file, zip)
            FileVisitResult.CONTINUE
          }
        })
      }

      publishFile(zipFile, locator.zipLocator)
    } finally {
      Files.deleteIfExists(zipFile)
      TempCleanup.forget(zipFile)
    }
  }

  def wipeCache(): Unit = {
    FileUtils.deleteDirectory(cacheDir.toFile)
  }
}

object Datastore extends Datastore(new S3Config) {

}
