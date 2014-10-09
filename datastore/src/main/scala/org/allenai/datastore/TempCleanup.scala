package org.allenai.datastore

import org.allenai.common.Logging

import org.apache.commons.io.FileUtils

import java.nio.file.{ DirectoryNotEmptyException, Files, Path }
import java.util.concurrent.ConcurrentSkipListSet

/** Remembers temporary files and directories that have to be cleaned up before
  * the JVM exits, and cleans them up when the JVM exits. As opposed to Java's File.deleteonexit(),
  * this can clean up non-empty directories.
  */
object TempCleanup extends Logging {
  private val rememberedPaths = new ConcurrentSkipListSet[Path]

  /**
   * Add a path to the list of things to clean up.
   *
   * Adding the same path twice has no effect.
   *
   * @param path the path to clean up
   */
  def remember(path: Path): Unit = {
    rememberedPaths.add(path)
  }

  /**
   * Removes a path from the list of things to clean up.
   *
   * Removing a path that's not there has no effect.
   *
   * @param path the path to no longer clean up
   */
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
