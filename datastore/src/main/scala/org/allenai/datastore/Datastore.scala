package org.allenai.datastore

import org.allenai.common.Resource
import org.allenai.common.Logging

import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ ListObjectsRequest, ObjectListing, AmazonS3Exception }
import org.apache.commons.io.FileUtils

import scala.collection.JavaConversions._

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.{ ZipEntry, ZipOutputStream, ZipFile }

class Datastore(val name: String, val s3: AmazonS3Client) extends Logging {
  private val systemTempDir = Paths.get(System.getProperty("java.io.tmpdir"))
  private val cacheDir = systemTempDir.resolve("ai2-datastore-cache").resolve(name)

  def bucketName: String = s"ai2-datastore-$name"

  /** Identifies a single version of a file or directory in the datastore */
  case class Locator(group: String, name: String, version: Int) {
    require(!group.contains("/"))
    require(!name.contains("/"))
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
    private[Datastore] def localCacheKey: String = s3key
    private[Datastore] def flatLocalCacheKey: String = localCacheKey.replace('/', '%')
    private[Datastore] def localCachePath: Path = cacheDir.resolve(localCacheKey)
    private[Datastore] def lockfilePath: Path = cacheDir.resolve(localCacheKey + ".lock")
    private[Datastore] def zipLocator: Locator = copy(name = name + ".zip")
  }

  object Locator {
    private[Datastore] def fromKey(key: String) = {
      val withExtension = """([^/]*)/(.*)-v(\d*)\.(.*)""".r
      val withoutExtension = """([^/]*)/(.*)-v(\d*)""".r

      // pattern matching on Int
      object Int {
        def unapply(s: String): Option[Int] = try {
          Some(s.toInt)
        } catch {
          case _: java.lang.NumberFormatException => None
        }
      }

      key match {
        case withExtension(group, name, Int(version), ext) =>
          Locator(group, s"$name.$ext", version)
        case withoutExtension(group, name, Int(version)) =>
          Locator(group, name, version)
        case _ =>
          throw new IllegalArgumentException(s"$key cannot be parsed as a datastore key")
      }
    }
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
    s3.getObject(bucketName, key).getObjectContent

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
    s3.putObject(bucketName, locator.s3key, file.toFile)
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
      s3.getObjectMetadata(bucketName, locator.s3key)
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

  private def getAllListings(request: ListObjectsRequest) = {
    def concatenateListings(
      listings: Seq[ObjectListing],
      newListing: ObjectListing): Seq[ObjectListing] = {
      val concatenation = listings :+ newListing
      if (newListing.isTruncated) {
        concatenateListings(concatenation, s3.listNextBatchOfObjects(newListing))
      } else {
        concatenation
      }
    }

    concatenateListings(Seq.empty, s3.listObjects(request))
  }

  def listGroups: Set[String] = {
    val listObjectsRequest =
      new ListObjectsRequest().
        withBucketName(bucketName).
        withPrefix("").
        withDelimiter("/")
    getAllListings(listObjectsRequest).flatMap(_.getCommonPrefixes).map(_.stripSuffix("/")).toSet
  }

  def listGroupContents(group: String): Set[Locator] = {
    val listObjectsRequest =
      new ListObjectsRequest().
        withBucketName(bucketName).
        withPrefix(group + "/").
        withDelimiter("/")
    getAllListings(listObjectsRequest).flatMap(_.getObjectSummaries).map { os =>
      Locator.fromKey(os.getKey)
    }.toSet
  }

  def wipeCache(): Unit = {
    FileUtils.deleteDirectory(cacheDir.toFile)
  }

  def createBucketIfNotExists(): Unit = {
    s3.createBucket(bucketName)
  }
}

object Datastore extends Datastore("public", new AmazonS3Client()) {
  val defaultName = Datastore.name

  def apply(): Datastore = Datastore(defaultName)
  def apply(name: String): Datastore = new Datastore(name, new AmazonS3Client())

  def apply(accessKey: String, secretAccessKey: String): Datastore =
    Datastore(defaultName, accessKey, secretAccessKey)
  def apply(name: String, accessKey: String, secretAccessKey: String): Datastore =
    new Datastore(
      name,
      new AmazonS3Client(new BasicAWSCredentials(accessKey, secretAccessKey)))
}