package org.allenai.datastore

import org.allenai.common.Resource
import org.allenai.common.Logging

import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ ListObjectsRequest, ObjectListing, AmazonS3Exception }
import org.apache.commons.io.FileUtils

import scala.collection.JavaConversions._

import java.io.InputStream
import java.net.URL
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.{ ZipEntry, ZipOutputStream, ZipFile }

/** Represents a datastore
  *
  * This is a thin layer over an S3 bucket that stores the data. Data is identified by group
  * ("org.allenai.something"), name ("WordNet"), and version (an integer). It supports files as well
  * as directories.
  *
  * Items are published to the datastore, and then referred to with the *path() methods. All data is
  * cached, so access to all items should be very fast, except for the first time.
  *
  * It might make more sense to get Datastore objects from the companion object, rather than
  * creating them here.
  *
  * @param name name of the datastore. Corresponds to the name of the bucket in S3. Currently we
  *            have "public" and "private".
  * @param s3   properly authenticated S3 client.
  */
class Datastore(val name: String, val s3: AmazonS3Client) extends Logging {
  private val systemTempDir = Paths.get(System.getProperty("java.io.tmpdir"))
  private val cacheDir = systemTempDir.resolve("ai2-datastore-cache").resolve(name)

  /** Returns the name of the bucket backing this datastore
    */
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

  /** Exception indicating that we tried to access an item in the datastore that wasn't there.
    *
    * @param locator Locator of the object that wasn't there
    * @param cause   More detailed reason, or null
    */
  class DoesNotExistException(
    locator: Locator,
    cause: Throwable = null) extends Exception(
    s"${locator.s3key} does not exist in the $name datastore",
    cause)

  /** Exception indicating that we tried to upload an item to the datastore that already exists.
    *
    * Data in the datastore is (mostly) immutable. Replacing an item is possible, but you have to
    * set a flag. If you don't set the flag, and you're replacing something, this exception gets
    * thrown.
    *
    * @param locator Locator of the object that's already there
    * @param cause   More detailed reason, or null
    */
  class AlreadyExistsException(
    locator: Locator,
    cause: Throwable = null) extends Exception(
    s"${locator.s3key} already exists in the $name datastore",
    cause)

  /** Utility function for getting an InputStream for an object in S3
    * @param key the key of the object
    * @return an InputStream with the contents of the object
    */
  private def getS3Object(key: String): InputStream =
    s3.getObject(bucketName, key).getObjectContent

  /** Waits until the given lockfile no longer exists
    *
    * @param lockfile path to the lockfile
    */
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

  /** Tries to create an empty file.
    *
    * @param file path to the file to be created
    *
    * @return true if the file was created. false otherwise.
    */
  private def tryCreateFile(file: Path): Boolean = {
    try {
      Files.createFile(file)
      true
    } catch {
      case _: FileAlreadyExistsException => false
    }
  }

  //
  // Getting data out of the datastore
  //

  /** Gets a local path for a file in the datastore
    *
    * Downloads the file from S3 if necessary
    *
    * @param group   the group of the file
    * @param name    the name of the file
    * @param version the version of the file
    * @return path to the file on the local filesystem
    */
  def filePath(group: String, name: String, version: Int): Path =
    path(Locator(group, name, version, false))

  /** Gets a local path for a directory in the datastore
    *
    * Downloads the directory from S3 if necessary
    *
    * @param group   the group of the directory
    * @param name    the name of the directory
    * @param version the version of the directory
    * @return path to the directory on the local filesystem
    */
  def directoryPath(group: String, name: String, version: Int): Path =
    path(Locator(group, name, version, true))

  /** Gets a local path for an item in the datastore
    *
    * Downloads the item from S3 if necessary
    *
    * @param locator locator for the item in the datastore
    * @return path to the item on the local filesystem
    */
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

  //
  // Putting data into the datastore
  //

  /** Publishes a file to the datastore
    *
    * @param file      name of the file to be published
    * @param group     group to publish the file under
    * @param name      name to publish the file under
    * @param version   version to publish the file under
    * @param overwrite if true, overwrites possible existing items in the datastore
    */
  def publishFile(
    file: String,
    group: String,
    name: String,
    version: Int,
    overwrite: Boolean): Unit =
    publishFile(Paths.get(file), group, name, version, overwrite)

  /** Publishes a file to the datastore
    *
    * @param file      path to the file to be published
    * @param group     group to publish the file under
    * @param name      name to publish the file under
    * @param version   version to publish the file under
    * @param overwrite if true, overwrites possible existing items in the datastore
    */
  def publishFile(
    file: Path,
    group: String,
    name: String,
    version: Int,
    overwrite: Boolean): Unit =
    publish(file, Locator(group, name, version, false), overwrite)

  /** Publishes a directory to the datastore
    *
    * @param path      name of the directory to be published
    * @param group     group to publish the directory under
    * @param name      name to publish the directory under
    * @param version   version to publish the directory under
    * @param overwrite if true, overwrites possible existing items in the datastore
    */
  def publishDirectory(
    path: String,
    group: String,
    name: String,
    version: Int,
    overwrite: Boolean): Unit =
    publishDirectory(Paths.get(path), group, name, version, overwrite)

  /** Publishes a directory to the datastore
    *
    * @param path      path to the directory to be published
    * @param group     group to publish the directory under
    * @param name      name to publish the directory under
    * @param version   version to publish the directory under
    * @param overwrite if true, overwrites possible existing items in the datastore
    */
  def publishDirectory(
    path: Path,
    group: String,
    name: String,
    version: Int,
    overwrite: Boolean): Unit =
    publish(path, Locator(group, name, version, true), overwrite)

  /** Publishes an item to the datastore
    *
    * @param path      path to the item to be published
    * @param locator   locator to publish the item under
    * @param overwrite if true, overwrites possible existing items in the datastore
    */
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

  //
  // Checking what's in the datastore
  //

  /** Checks whether a file exists in the datastore
    *
    * @param group   group of the file in the datastore
    * @param name    name of the file in the datastore
    * @param version version of the file in the datastore
    * @return true if the file exists, false otherwise
    */
  def fileExists(group: String, name: String, version: Int): Boolean =
    exists(Locator(group, name, version, false))

  /** Checks whether a directory exists in the datastore
    *
    * @param group   group of the directory in the datastore
    * @param name    name of the directory in the datastore
    * @param version version of the directory in the datastore
    * @return true if the directory exists, false otherwise
    */
  def directoryExists(group: String, name: String, version: Int): Boolean =
    exists(Locator(group, name, version, true))

  /** Checks whether an item exists in the datastore
    *
    * @param locator locator of the item in the datastore
    * @return true if the item exists, false otherwise
    */
  def exists(locator: Locator): Boolean = {
    try {
      s3.getObjectMetadata(bucketName, locator.s3key)
      true
    } catch {
      case e: AmazonServiceException if e.getStatusCode == 404 =>
        false
    }
  }

  //
  // Listing the datastore
  //

  /** Rolls up all the listings in a paged object listing from S3
    * @param request object listing request to send to S3
    * @return a sequence of object listings from S3
    */
  private def getAllListings(request: ListObjectsRequest): Seq[ObjectListing] = {
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

  /** Lists all groups in the datastore
    * @return a set of all groups in the datastore
    */
  def listGroups: Set[String] = {
    val listObjectsRequest =
      new ListObjectsRequest().
        withBucketName(bucketName).
        withPrefix("").
        withDelimiter("/")
    getAllListings(listObjectsRequest).flatMap(_.getCommonPrefixes).map(_.stripSuffix("/")).toSet
  }

  /** Lists all items in a group
    * @param group group to search over
    * @return a set of locators, one for each item in the group. Multiple versions are multiple
    *       locators.
    */
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

  //
  // Getting URLs for datastore items
  //

  /** Gets a URL for a file in the datastore
    * @param group   group of the file
    * @param name    name of the file
    * @param version version of the file
    * @return URL pointing to the file
    */
  def fileUrl(group: String, name: String, version: Int): URL =
    url(Locator(group, name, version, false))

  /** Gets a URL for a directory in the datastore
    * @param group   group of the directory
    * @param name    name of the directory
    * @param version version of the directory
    * @return URL pointing to the directory. This URL will always point to a zip file containing the
    *       directory's contents.
    */
  def directoryUrl(group: String, name: String, version: Int): URL =
    url(Locator(group, name, version, true))

  /** Gets the URL for an item in the datastore
    * @param locator locator of the item
    * @return URL pointing to the locator
    */
  def url(locator: Locator): URL =
    new URL("http", bucketName, locator.s3key)

  //
  // Assorted stuff
  //

  /** Wipes the cache for this datastore
    */
  def wipeCache(): Unit = {
    FileUtils.deleteDirectory(cacheDir.toFile)
  }

  /** Creates the buckey backing this datastore if necessary
    */
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
