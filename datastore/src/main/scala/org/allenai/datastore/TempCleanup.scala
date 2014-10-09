package org.allenai.datastore

import org.allenai.common.Logging

import org.apache.commons.io.FileUtils

import java.nio.file.{ DirectoryNotEmptyException, Files, Path }
import java.util.concurrent.ConcurrentSkipListSet

/** Remembers temporary files and directories that have to be cleaned up before
  * the JVM exits. As opposed to Java's File.deleteonexit(), this can clean up
  * non-empty directories.
  */
object TempCleanup extends Logging {
  private val rememberedPaths = new ConcurrentSkipListSet[Path]

  def remember(path: Path): Unit = {
    rememberedPaths.add(path)
  }

  def forget(path: Path): Unit = {
    rememberedPaths.remove(path)
  }

  private val cleanupThread = new Thread() {
    override def run(): Unit = {
      while (!rememberedPaths.isEmpty) {
        val rememberedPath = rememberedPaths.pollLast()
        try {
          try {
            val deleted = Files.deleteIfExists(rememberedPath)
            if (deleted) {
              logger.info(s"Cleaning up file at $rememberedPath")
            }
          } catch {
            case _: DirectoryNotEmptyException =>
              FileUtils.deleteDirectory(rememberedPath.toFile)
              logger.info(s"Cleaning up non-empty directory at $rememberedPath")
          }
        } catch {
          case e: Throwable =>
            logger.warn(s"Could not clean up file at $rememberedPath", e)
        }
      }
    }
  }

  Runtime.getRuntime.addShutdownHook(cleanupThread)
}
