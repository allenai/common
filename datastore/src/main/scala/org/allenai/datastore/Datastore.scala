package org.allenai.datastore

import org.allenai.common.Resource
import org.allenai.common.Logging

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.model.{ ListObjectsRequest, ObjectListing, AmazonS3Exception }
import org.apache.commons.io.FileUtils

import scala.collection.JavaConversions._

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.{ ZipEntry, ZipOutputStream, ZipFile }

class Datastore(val s3config: S3Config) extends Logging {
  def this(datastore: String) = this(new S3Config(datastore))

  private val systemTempDir = Paths.get(System.getProperty("java.io.tmpdir"))
  private val cacheDir = systemTempDir.resolve("ai2-datastore-cache").resolve(s3config.bucket)

  def name: String = s3config.bucket

  /** Identifies a single version of a file or directory in the datastore */
  case class Locator(group: String, name: String, version: Int) {
    require(version > 0)

    def nameWithVersion: String = {
      val lastDotIndex = name.lastIndexOf('.')
      if (lastDotIndex < 0) {
        s"$name-v$version"
      } else {
        name.substring(0, lastDotIndex) + s"-v$version" + name.substring(lastDotIndex)
      }
    }
    def s3key: String = s"$group/$nameWithVersion"
    def localCacheKey: String = s3key
    def flatLocalCacheKey: String = localCacheKey.replace('/', '%')
    def localCachePath: Path = cacheDir.resolve(localCacheKey)
    def lockfilePath: Path = cacheDir.resolve(localCacheKey + ".lock")
    def zipLocator: Locator = copy(name = name + ".zip")
  }

  class DoesNotExistException(
    locator: Locator,
    cause: Throwable = null) extends Exception(
    s"${locator.s3key} does not exist in the $name datastore",
    cause)

  class AlreadyExistsException(
    locator: Locator,
    cause: Throwable = null) extends Exception(
    s"${locator.s3key} already exists in the $name datastore",
    cause)

  private def getS3Object(key: String) =
    s3config.service.getObject(s3config.bucket, key).getObjectContent

  private def waitForLockfile(lockfile: Path): Unit = {
    // TODO: Use watch interfaces instead of busy wait
    val start = System.currentTimeMillis()
    while (Files.exists(lockfile)) {
      val message = s"Waiting for lockfile at $lockfile}"
      if (System.currentTimeMillis() - start > 60 * 1000) {
        logger.warn(message)
      } else {
        logger.info(message)
      }
      val oneSecond = 1000
      Thread.sleep(oneSecond)
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
    Files.createDirectories(cacheDir)

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
    Files.createDirectories(cacheDir)

    Files.createDirectories(locator.lockfilePath.getParent)
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
              if (entry.getName != "/") {
                val pathForEntry = tempDir.resolve(entry.getName)
                if (entry.isDirectory) {
                  Files.createDirectories(pathForEntry)
                } else {
                  Files.createDirectories(pathForEntry.getParent)
                  Resource.using(zipFile.getInputStream(entry))(Files.copy(_, pathForEntry))
                }
              }
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

  def publishFile(
    file: String,
    group: String,
    name: String,
    version: Int,
    overwrite: Boolean): Unit =
    publishFile(Paths.get(file), group, name, version, overwrite)
  def publishFile(
    file: Path,
    group: String,
    name: String,
    version: Int,
    overwrite: Boolean): Unit =
    publishFile(file, Locator(group, name, version), overwrite)
  def publishFile(file: Path, locator: Locator, overwrite: Boolean): Unit = {
    if (!overwrite && fileExists(locator)) {
      throw new AlreadyExistsException(locator)
    }
    s3config.service.putObject(s3config.bucket, locator.s3key, file.toFile)
  }

  def publishDirectory(
    path: Path,
    group: String,
    name: String,
    version: Int,
    overwrite: Boolean): Unit =
    publishDirectory(path, Locator(group, name, version), overwrite)
  def publishDirectory(path: Path, locator: Locator, overwrite: Boolean): Unit = {
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

          override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
            if (dir != path) {
              zip.putNextEntry(new ZipEntry(path.relativize(dir).toString + "/"))
            }
            FileVisitResult.CONTINUE
          }
        })
      }

      publishFile(zipFile, locator.zipLocator, overwrite)
    } finally {
      Files.deleteIfExists(zipFile)
      TempCleanup.forget(zipFile)
    }
  }

  def fileExists(group: String, name: String, version: Int): Boolean =
    fileExists(Locator(group, name, version))
  def fileExists(locator: Locator): Boolean = {
    try {
      s3config.service.getObjectMetadata(s3config.bucket, locator.s3key)
      true
    } catch {
      case e: AmazonServiceException if e.getStatusCode == 404 =>
        false
    }
  }

  def directoryExists(group: String, name: String, version: Int): Boolean =
    directoryExists(Locator(group, name, version))
  def directoryExists(locator: Locator): Boolean =
    fileExists(locator.zipLocator)

  def listGroups: Set[String] = {
    def getAllListings(listing: ObjectListing): Set[String] = {
      val prefixes = listing.getCommonPrefixes.map(s => s.stripSuffix("/"))
      prefixes.toSet ++ (if (listing.isTruncated) {
        getAllListings(s3config.service.listNextBatchOfObjects(listing))
      } else {
        Set.empty
      })
    }

    val listObjectsRequest =
      new ListObjectsRequest().withBucketName(s3config.bucket).withPrefix("").withDelimiter("/")
    val firstListing = s3config.service.listObjects(listObjectsRequest)
    getAllListings(firstListing)
  }

  def wipeCache(): Unit = {
    FileUtils.deleteDirectory(cacheDir.toFile)
  }
}

object Datastore extends Datastore(new S3Config) {

}
