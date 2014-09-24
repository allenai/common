package org.allenai.datastore

import org.allenai.common.Resource
import org.allenai.common.Logging

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

class Datastore(val s3config: S3Config) extends Logging {
  private val systemTempDir = new File(System.getProperty("java.io.tmpdir"))
  private val cacheDir = new File(systemTempDir, "ai2-datastore-cache")
  cacheDir.mkdirs()

  /** Identifies a single version of a file or directory in the datastore */
  case class Locator(group: String, name: String, version: Int) {
    def nameWithVersion = {
      val lastDotIndex = name.lastIndexOf('.')
      if(lastDotIndex < 0) {
        s"name-v$version"
      } else {
        name.substring(0, lastDotIndex) + s"-v$version" + name.substring(lastDotIndex)
      }
    }
    def s3key = s"$group/$nameWithVersion"
    def localCacheKey = s3key
    def localCachePath = new File(cacheDir, localCacheKey)
    def lockfilePath = new File(cacheDir, localCacheKey + ".lock")
  }

  // If the process dies for any reason, we have to be ready to remove all the
  // locks we're still holding. This stores which lock files we created, so they
  // can be cleaned up at the end.
  private val openLockfiles =
    new java.util.concurrent.ConcurrentSkipListSet[File]
  private val cleanupThread = new Thread() {
    override def run(): Unit = {
      while(!openLockfiles.isEmpty) {
        val lockfile = openLockfiles.pollLast()
        logger.info(s"Cleaning up lockfile at ${lockfile.getAbsolutePath}")
        lockfile.delete()
      }
    }
  }
  Runtime.getRuntime.addShutdownHook(cleanupThread)

  private def getS3Object(key: String) =
    s3config.service.getObject(s3config.bucket, key).getObjectContent
  private def waitForLockfile(lockfile: File): Unit = {
    val start = System.currentTimeMillis()
    while(lockfile.exists) {
      val message = s"Waiting for lockfile at ${ lockfile.getAbsolutePath }"
      if (System.currentTimeMillis() - start > 60 * 1000)
        logger.warn(message)
      else
        logger.info(message)
      Thread.sleep(1000)
    }
  }

  def filePath(group: String, name: String, version: Int): File =
    filePath(Locator(group, name, version))
  def filePath(locator: Locator): File = {
    waitForLockfile(locator.lockfilePath)

    if(!locator.localCachePath.isFile) {
      val created = locator.lockfilePath.createNewFile()
      if(!created) {
        // someone else started creating this in the meantime
        filePath(locator)
      } else {
        openLockfiles.add(locator.lockfilePath)
        try {
          // We're downloading to a temp file first. If we were downloading into
          // the file directly, and we died half-way through the download, we'd
          // leave half a file, and that's not good.
          val tempFile =
            Files.createTempFile("ai2-datastore-" + locator.localCacheKey, ".tmp")
          Resource.using(getS3Object(locator.s3key))(Files.copy(_, tempFile))
          Files.move(tempFile, locator.localCachePath.toPath)
        } finally {
          locator.lockfilePath.delete()
          openLockfiles.remove(locator.lockfilePath)
        }
        locator.localCachePath
      }
    } else {
      locator.localCachePath
    }
  }

  def directoryPath(group: String, name: String, version: Int): File =
    directoryPath(Locator(group, name, version))
  def directoryPath(locator: Locator): File = {
    waitForLockfile(locator.lockfilePath)

    if(locator.localCachePath.isDirectory) {
      locator.localCachePath
    } else {
      val created = locator.lockfilePath.createNewFile()
      if(!created) {
        directoryPath(locator)
      } else {
        openLockfiles.add(locator.lockfilePath)
        try {
          val tempDir =
            Files.createTempDirectory("ai2-datastore-" + locator.localCacheKey)

          // download and extract the zip file to the directory
          val zipLocator = locator.copy(name = locator.name + ".zip")
          Resource.using(new ZipFile(filePath(zipLocator))){ zipFile =>
            val entries = zipFile.entries()
            while (entries.hasMoreElements) {
              val entry = entries.nextElement()
              val pathForEntry = tempDir.resolve(entry.getName)
              Resource.using(zipFile.getInputStream(entry))(Files.copy(_, pathForEntry))
            }
          }

          // move the directory where it belongs
          Files.move(tempDir, locator.localCachePath.toPath)
        } finally {
          locator.lockfilePath.delete()
          openLockfiles.remove(locator.lockfilePath)
        }

        locator.localCachePath
      }
    }
  }

  def publishFile(file: String, group: String, name: String, version: Int) =
    publishFile(new File(file), group, name, version)
  def publishFile(file: File, group: String, name: String, version: Int) =
    publishFile(file, Locator(group, name, version))
  def publishFile(file: File, locator: Locator): Unit = {
    s3config.service.putObject(s3config.bucket, locator.s3key, file)
  }

  def publishDirectory(path: File, group: String, name: String, version: Int) =
    publishDirectory(path, Locator(group, name, version))
  def publishDirectory(path: File, locator: Locator): Unit = {

  }
}

object Datastore extends Datastore(S3Config()) {

}
