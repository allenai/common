package org.allenai.common.indexing

import org.allenai.common.testkit.UnitSpec

class BarronsDocumentReaderSpec extends UnitSpec {

  val sentences = (0 to 20).map("sentence " + _)
  val sampleLines = Seq(
    s"5.1.1.1.1\t${sentences(0)}",
    s"5.1.1.1.2\t${sentences(1)}",
    s"5.1.1.1.3\t${sentences(2)}",
    s"5.1.1.2.1\t${sentences(3)}",
    s"5.1.1.2.2\t${sentences(4)}",
    s"5.1.1.2.3\t${sentences(5)}",
    s"5.1.1.1.1.1\t${sentences(6)}",
    s"5.1.1.1.2.1\t${sentences(7)}",
    s"5.1.1.1.2.2\t${sentences(8)}",
    s"5.1.1.1.2.3\t${sentences(9)}",
    s"5.1.1.1.3.1\t${sentences(10)}",
    s"5.1.1.1.3.2\t${sentences(11)}")

  "read" should "get paragraphs out" in {
    val readDocument = new BarronsDocumentReader(null, "UTF-8")._readLines(sampleLines)
    val expectedDocument = new SegmentedDocument(sampleLines.mkString("\n"), Seq(
      NonTerminalSegment("paragraph", Seq(
        TerminalSegment("sentence", sentences(0)),
        TerminalSegment("sentence", sentences(1)),
        TerminalSegment("sentence", sentences(2)))),
      NonTerminalSegment("paragraph", Seq(
        TerminalSegment("sentence", sentences(3)),
        TerminalSegment("sentence", sentences(4)),
        TerminalSegment("sentence", sentences(5)))),
      NonTerminalSegment("paragraph", Seq(
        TerminalSegment("sentence", sentences(6)))),
      NonTerminalSegment("paragraph", Seq(
        TerminalSegment("sentence", sentences(7)),
        TerminalSegment("sentence", sentences(8)),
        TerminalSegment("sentence", sentences(9)))),
      NonTerminalSegment("paragraph", Seq(
        TerminalSegment("sentence", sentences(10)),
        TerminalSegment("sentence", sentences(11))))
      )
    )
    readDocument should be(expectedDocument)
  }
}

