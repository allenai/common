package org.allenai.vault

import org.allenai.common.Resource

import java.io.{FileInputStream, File, InputStream}
import java.net.URI
import java.nio.file.Files
import java.util.zip.ZipFile

class Vault(val s3config: S3Config) {
  private val systemTempDir = new File(System.getProperty("java.io.tmpdir"))
  private val cacheDir = new File(systemTempDir, "ai2-vault-cache")

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
  }

  private def getS3Object(key: String) =
    s3config.service.getObject(s3config.bucket, key).getObjectContent

  def filePath(group: String, name: String, version: Int): File =
    filePath(Locator(group, name, version))
  def filePath(locator: Locator): File = {
    if(!locator.localCachePath.isFile) {
      // We're downloading to a temp file first. If we were downloading into the
      // file directly, and two threads were trying to access the same file, the
      // first thread would start the download, and the second one would return
      // with a half-downloaded file. This way, we download the same file twice,
      // but at least we don't deliver broken data.
      val tempFile = File.createTempFile(locator.localCacheKey, ".ai2-vault.tmp")
      Resource.using(getS3Object(locator.s3key))(Files.copy(_, tempFile.toPath))
      tempFile.renameTo(locator.localCachePath)
    }

    locator.localCachePath
  }

  def directoryPath(group: String, name: String, version: Int): File =
    directoryPath(Locator(group, name, version))
  def directoryPath(locator: Locator): File = {
    if(!locator.localCachePath.isDirectory) {
      locator.localCachePath.mkdirs()

      // download and extract the zip file to the directory
      val zipLocator = locator.copy(name = locator.name + ".zip")
      Resource.using(new ZipFile(filePath(zipLocator))){ zipFile =>
        val entries = zipFile.entries()
        while (entries.hasMoreElements) {
          val entry = entries.nextElement()
          val pathForEntry = new File(locator.localCachePath, entry.getName)
          Resource.using(zipFile.getInputStream(entry))(Files.copy(_, pathForEntry.toPath))
        }
      }
    }

    locator.localCachePath
  }


  def publishFile(file: InputStream, group: String, name: String, version: Int)
  def publishFile(file: File, group: String, name: String, version: Int) =
    Resource.using(new FileInputStream(file)) { is =>
      publishFile(is, group, name, version)
    }
  def publishFile(file: String, group: String, name: String, version: Int) =
    Resource.using(new FileInputStream(file)) { is =>
      publishFile(is, group, name, version)
    }

  def publishDirectory(path: File, group: String, name: String, version: Int)
}

object Vault extends Vault(S3Config()) {

}
