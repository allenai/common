package org.allenai.common.indexing

import org.allenai.common.testkit.UnitSpec

class SegmentedDocumentSpec extends UnitSpec {

  val sentence1 = TerminalSegment("sentence", "sentence1")
  val sentence2 = TerminalSegment("sentence", "sentence2")
  val sentence3 = TerminalSegment("sentence", "sentence3")
  val sentence4 = TerminalSegment("sentence", "sentence4")
  val paragraph1 = NonTerminalSegment("paragraph", Seq(sentence1, sentence2))
  val paragraph2 = NonTerminalSegment("paragraph", Seq(sentence3))
  val paragraph3 = NonTerminalSegment("paragraph", Seq(sentence4))
  val section1 = NonTerminalSegment("section", Seq(paragraph1))
  val section2 = NonTerminalSegment("section", Seq(paragraph2, paragraph3))
  val chapter1 = NonTerminalSegment("chapter", Seq(section1, section2))
  val document = new SegmentedDocument("dummy text", Seq(chapter1))

  "Segment.getSegmentsOfType" should "recursively get all segments of the correct type" in {
    chapter1.getSegmentsOfType("chapter") should be(Seq(chapter1))
    chapter1.getSegmentsOfType("section") should be(Seq(section1, section2))
    chapter1.getSegmentsOfType("paragraph") should be(Seq(paragraph1, paragraph2, paragraph3))
    chapter1.getSegmentsOfType("sentence") should be(Seq(sentence1, sentence2, sentence3, sentence4))
  }

  "Segment.getTextSegments" should "recursively get text from all terminal segments" in {
    chapter1.getTextSegments() should be(Seq("sentence1", "sentence2", "sentence3", "sentence4"))
    section1.getTextSegments() should be(Seq("sentence1", "sentence2"))
    section2.getTextSegments() should be(Seq("sentence3", "sentence4"))
    paragraph1.getTextSegments() should be(Seq("sentence1", "sentence2"))
    paragraph2.getTextSegments() should be(Seq("sentence3"))
    paragraph3.getTextSegments() should be(Seq("sentence4"))
    sentence1.getTextSegments() should be(Seq("sentence1"))
    sentence2.getTextSegments() should be(Seq("sentence2"))
    sentence3.getTextSegments() should be(Seq("sentence3"))
    sentence4.getTextSegments() should be(Seq("sentence4"))
  }

  "SegmentedDocument.getSegmentsOfType" should "get all segments of the correct type" in {
    document.getSegmentsOfType("chapter") should be(Seq(chapter1))
    document.getSegmentsOfType("section") should be(Seq(section1, section2))
    document.getSegmentsOfType("paragraph") should be(Seq(paragraph1, paragraph2, paragraph3))
    document.getSegmentsOfType("sentence") should be(Seq(sentence1, sentence2, sentence3, sentence4))
  }

  "SegmentedDocumentBuilder" should "correctly build a segmented document" in {
    val builder = new SegmentedDocumentBuilder("dummy text")
    builder.startNewNonTerminalSegment("chapter")
    builder.startNewNonTerminalSegment("section")
    builder.startNewNonTerminalSegment("paragraph")
    builder.addTerminalSegment("sentence", "sentence1")
    builder.addTerminalSegment("sentence", "sentence2")
    builder.finishNonTerminalSegment()
    builder.finishNonTerminalSegment()
    builder.startNewNonTerminalSegment("section")
    builder.startNewNonTerminalSegment("paragraph")
    builder.addTerminalSegment("sentence", "sentence3")
    builder.finishNonTerminalSegment()
    builder.startNewNonTerminalSegment("paragraph")
    builder.addTerminalSegment("sentence", "sentence4")
    builder.finishNonTerminalSegment()
    builder.finishNonTerminalSegment()
    builder.finishNonTerminalSegment()

    builder.build() should be(document)
  }
}
