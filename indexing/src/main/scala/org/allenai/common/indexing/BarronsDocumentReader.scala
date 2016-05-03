package org.allenai.common.indexing

import java.io.File
import scala.io.Codec
import scala.io.Source

class BarronsDocumentReader(file: File, codec: Codec) {
  def read(): SegmentedDocument = {
    // Barron's isn't all that large, and this makes testing this a lot easier.
    val lines = Source.fromFile(file)(codec).getLines.toSeq
    _readLines(lines)
  }

  def _readLines(lines: Seq[String]): SegmentedDocument = {
    // At this point, I'm just going to worry about getting paragraph structure out.  Sections and
    // chapters will have to wait for another time, if someone cares about doing that some day.
    // So, this is a really simple parsing algorithm that will just group consecutive sentences
    // into paragraphs, where "consecutive" is defined by the sentence numbers in the original
    // file.
    var prevKey = ""
    var prevSentence = ""
    val builder = new SegmentedDocumentBuilder(lines.mkString("\n"))
    for (line <- lines) {
      val fields = line.split("\t")
      val longNumber = fields(0)
      val sentenceText = fields(1)
      val (key, sentenceNumber) = longNumber.splitAt(longNumber.lastIndexOf("."))
      if (key != prevKey) {
        if (prevKey != "") {
          builder.finishNonTerminalSegment()
        }
        prevKey = key
        builder.startNewNonTerminalSegment("paragraph")
      }
      // I could check that the sentence number increments by one here, but I don't think that's
      // actually necessary.  Just checking the key should be enough.
      builder.addTerminalSegment("sentence", sentenceText)
    }
    builder.finishNonTerminalSegment()
    builder.build()
  }
}
