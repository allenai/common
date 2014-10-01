package org.allenai.datastore

import org.allenai.common.Resource
import org.allenai.common.Logging

import org.apache.commons.io.FileUtils

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.{ ZipEntry, ZipOutputStream, ZipFile }

class Datastore(val s3config: S3Config) extends Logging {
  private val systemTempDir = Paths.get(System.getProperty("java.io.tmpdir"))
  private val cacheDir = systemTempDir.resolve("ai2-datastore-cache")
  Files.createDirectories(cacheDir)

  /** Identifies a single version of a file or directory in the datastore */
  case class Locator(group: String, name: String, version: Int) {
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

  // If the process dies for any reason, we have to be ready to remove all the
  // locks and temporary files we're still holding. This stores which files we
  // created, so they can be cleaned up at the end.
  private val leftOverFiles =
    new java.util.concurrent.ConcurrentSkipListSet[Path]
  private val cleanupThread = new Thread() {
    override def run(): Unit = {
      while (!leftOverFiles.isEmpty) {
        val leftOverFile = leftOverFiles.pollLast()
        try {
          try {
            val deleted = Files.deleteIfExists(leftOverFile)
            if (deleted)
              logger.info(s"Cleaning up file at $leftOverFile")
          } catch {
            case _: DirectoryNotEmptyException =>
              FileUtils.deleteDirectory(leftOverFile.toFile)
              logger.info(s"Cleaning up non-empty directory at $leftOverFile")
          }
        } catch {
          case e: Throwable =>
            logger.warn(s"Could not clean up file at $leftOverFile", e)
        }
      }
    }
  }
  Runtime.getRuntime.addShutdownHook(cleanupThread)

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
      val created = tryCreateFile(locator.lockfilePath)
      if (!created) {
        // someone else started creating this in the meantime
        filePath(locator)
      } else {
        leftOverFiles.add(locator.lockfilePath)
        try {
          // We're downloading to a temp file first. If we were downloading into
          // the file directly, and we died half-way through the download, we'd
          // leave half a file, and that's not good.
          val tempFile =
            Files.createTempFile("ai2-datastore-" + locator.flatLocalCacheKey, ".tmp")
          leftOverFiles.add(tempFile)
          Resource.using(getS3Object(locator.s3key))(Files.copy(_, tempFile))
          Files.move(tempFile, locator.localCachePath)
          leftOverFiles.remove(tempFile)
        } finally {
          Files.delete(locator.lockfilePath)
          leftOverFiles.remove(locator.lockfilePath)
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
        leftOverFiles.add(locator.lockfilePath)
        try {
          val tempDir =
            Files.createTempDirectory("ai2-datastore-" + locator.flatLocalCacheKey)
          leftOverFiles.add(tempDir)

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
          leftOverFiles.remove(tempDir)
        } finally {
          Files.delete(locator.lockfilePath)
          leftOverFiles.remove(locator.lockfilePath)
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
    leftOverFiles.add(zipFile)
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
      leftOverFiles.remove(zipFile)
    }
  }
}

object Datastore extends Datastore(S3Config()) {

}
