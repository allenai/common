package org.allenai.common.testkit

import org.scalatest.BeforeAndAfterAll

import java.io.File

/** Provides a scratch directory for writing unit-test output */
trait ScratchDirectory extends BeforeAndAfterAll {
  this: UnitSpec =>

  val scratchDir: File = createScratchDirectory()

  def createScratchDirectory(): File = {
    val dir = File.createTempFile(this.getClass.getSimpleName, "scratch")
    if (dir.exists && !dir.isDirectory) {
      dir.delete()
    }
    if (!dir.exists) {
      dir.mkdirs()
    }
    dir
  }

  override def beforeAll: Unit = {
    require(scratchDir.exists && scratchDir.isDirectory,
      s"Unable to create scratch directory $scratchDir")
  }

  override def afterAll: Unit = {
    def delete(f: File) {
      if (f.isDirectory()) {
        f.listFiles.foreach(delete)
      }
      f.delete()
    }
    delete(scratchDir)
  }
}
