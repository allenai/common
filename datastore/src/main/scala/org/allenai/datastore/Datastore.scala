package org.allenai.datastore

import org.allenai.common.Resource
import org.allenai.common.Logging

import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ ListObjectsRequest, ObjectListing, AmazonS3Exception }
import org.apache.commons.io.FileUtils

import scala.collection.JavaConversions._

import java.net.URL
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.{ ZipEntry, ZipOutputStream, ZipFile }

class Datastore(val name: String, val s3: AmazonS3Client) extends Logging {
  private val systemTempDir = Paths.get(System.getProperty("java.io.tmpdir"))
  private val cacheDir = systemTempDir.resolve("ai2-datastore-cache").resolve(name)

  def bucketName: String = s"$name.store.dev.allenai.org"

  /** Identifies a single version of a file or directory in the datastore */
  case class Locator(group: String, name: String, version: Int, directory: Boolean) {
    require(!group.contains("/"))
    require(!name.contains("/"))
    require(version > 0)

    private[Datastore] def nameWithVersion: String = {
      if (directory) {
        s"$name-d$version.zip"
      } else {
        val lastDotIndex = name.lastIndexOf('.')
        if (lastDotIndex < 0) {
          s"$name-v$version"
        } else {
          name.substring(0, lastDotIndex) + s"-v$version" + name.substring(lastDotIndex)
        }
      }
    }
    private[Datastore] def s3key: String = s"$group/$nameWithVersion"
    private[Datastore] def localCacheKey: String =
      if (directory) s3key.stripSuffix(".zip") else s3key
    private[Datastore] def flatLocalCacheKey: String = localCacheKey.replace('/', '%')
    private[Datastore] def localCachePath: Path = cacheDir.resolve(localCacheKey)
    private[Datastore] def lockfilePath: Path = cacheDir.resolve(localCacheKey + ".lock")
  }

  object Locator {
    private[Datastore] def fromKey(key: String) = {
      val withExtension = """([^/]*)/(.*)-(.)(\d*)\.(.*)""".r
      val withoutExtension = """([^/]*)/(.*)-(.)(\d*)""".r

      // pattern matching on Int
      object Int {
        def unapply(s: String): Option[Int] = try {
          Some(s.toInt)
        } catch {
          case _: java.lang.NumberFormatException => None
        }
      }

      key match {
        case withExtension(group, name, "v", Int(version), ext) =>
          Locator(group, s"$name.$ext", version, false)
        case withoutExtension(group, name, "v", Int(version)) =>
          Locator(group, name, version, false)
        case withExtension(group, name, "d", Int(version), "zip") =>
          Locator(group, name, version, true)
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
    path(Locator(group, name, version, false))
  def directoryPath(group: String, name: String, version: Int): Path =
    path(Locator(group, name, version, true))

  def path(locator: Locator): Path = {
    Files.createDirectories(cacheDir)
    Files.createDirectories(locator.lockfilePath.getParent)
    waitForLockfile(locator.lockfilePath)

    if ((locator.directory && Files.isDirectory(locator.localCachePath)) ||
      (!locator.directory && Files.isRegularFile(locator.localCachePath))) {
      locator.localCachePath
    } else {
      val created = tryCreateFile(locator.lockfilePath)
      if (!created) {
        path(locator)
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

          if (locator.directory) {
            val tempDir =
              Files.createTempDirectory("ai2-datastore-" + locator.flatLocalCacheKey)
            TempCleanup.remember(tempDir)

            // download and extract the zip file to the directory
            Resource.using(new ZipFile(tempFile.toFile)) { zipFile =>
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
            Files.delete(tempFile)
            TempCleanup.forget(tempFile)

            // move the directory where it belongs
            Files.move(tempDir, locator.localCachePath)
            TempCleanup.forget(tempDir)
          } else {
            Files.createDirectories(locator.localCachePath.getParent)
            Files.move(tempFile, locator.localCachePath)
            TempCleanup.forget(tempFile)
          }

          locator.localCachePath
        } finally {
          Files.delete(locator.lockfilePath)
          TempCleanup.forget(locator.lockfilePath)
        }
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
    publish(file, Locator(group, name, version, false), overwrite)

  def publishDirectory(
    path: String,
    group: String,
    name: String,
    version: Int,
    overwrite: Boolean): Unit =
    publishDirectory(Paths.get(path), group, name, version, overwrite)
  def publishDirectory(
    path: Path,
    group: String,
    name: String,
    version: Int,
    overwrite: Boolean): Unit =
    publish(path, Locator(group, name, version, true), overwrite)

  def publish(path: Path, locator: Locator, overwrite: Boolean): Unit = {
    if (!overwrite && exists(locator)) {
      throw new AlreadyExistsException(locator)
    }

    if (locator.directory) {
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

        s3.putObject(bucketName, locator.s3key, zipFile.toFile)
      } finally {
        Files.deleteIfExists(zipFile)
        TempCleanup.forget(zipFile)
      }
    } else {
      s3.putObject(bucketName, locator.s3key, path.toFile)
    }
  }

  def fileExists(group: String, name: String, version: Int): Boolean =
    exists(Locator(group, name, version, false))
  def directoryExists(group: String, name: String, version: Int): Boolean =
    exists(Locator(group, name, version, true))

  def exists(locator: Locator): Boolean = {
    try {
      s3.getObjectMetadata(bucketName, locator.s3key)
      true
    } catch {
      case e: AmazonServiceException if e.getStatusCode == 404 =>
        false
    }
  }

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

  def fileUrl(group: String, name: String, version: Int): URL =
    url(Locator(group, name, version, false))

  def directoryUrl(group: String, name: String, version: Int): URL =
    url(Locator(group, name, version, true))

  def url(locator: Locator): URL =
    new URL("http", bucketName, locator.s3key)

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
