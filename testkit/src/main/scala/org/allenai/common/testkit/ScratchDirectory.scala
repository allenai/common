package org.allenai.common.testkit

import org.scalatest.BeforeAndAfterAll

import java.io.File
import java.nio.file.Files

/** Provides a scratch directory for writing unit-test output */
trait ScratchDirectory extends BeforeAndAfterAll {
  this: AllenAiBaseSpec =>

  val scratchDir: File = {
    val dir = Files.createTempDirectory(this.getClass.getSimpleName).toFile
    sys.addShutdownHook(delete(dir))
    dir
  }

  override def beforeAll(): Unit = require(
    scratchDir.exists && scratchDir.isDirectory,
    s"Unable to create scratch directory $scratchDir"
  )

  override def afterAll(): Unit = delete(scratchDir)

  private def delete(f: File): Boolean = {
    if (f.isDirectory()) {
      f.listFiles.foreach(delete)
    }
    f.delete()
  }
}
