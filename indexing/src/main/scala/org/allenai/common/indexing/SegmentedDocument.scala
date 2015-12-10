package org.allenai.common.indexing

import org.allenai.nlpstack.core.repr.Document

import scala.collection.mutable

/** A document that has been broken up into (potentially nested) segments.  Note that there's a
  * notion of a segment and segmenter in the nlpstack, but those are used exclusively for sentences.
  * This class aims to capture higher-level document structure than sentences.
  */
class SegmentedDocument(text: String, val segments: Seq[Segment]) extends Document(text) {
  def getSegmentsOfType(segmentType: String): Seq[Segment] = {
    segments.flatMap(_.getSegmentsOfType(segmentType))
  }

  override def equals(that: Any) = that match {
    case that: SegmentedDocument => { that.text == this.text && that.segments == this.segments }
    case _ => false
  }
}

class SegmentedDocumentBuilder(text: String) {
  val finishedTopLevelSegments = new mutable.ListBuffer[Segment]
  val segmentStack = new mutable.Stack[(String, mutable.ListBuffer[Segment])]

  def startNewNonTerminalSegment(segmentType: String) {
    segmentStack.push((segmentType, new mutable.ListBuffer[Segment]))
  }

  def finishNonTerminalSegment() {
    val segmentToFinish = segmentStack.pop()
    val finishedSegment = NonTerminalSegment(segmentToFinish._1, segmentToFinish._2.toSeq)
    if (segmentStack.size == 0) {
      finishedTopLevelSegments.append(finishedSegment)
    } else {
      segmentStack.top._2.append(finishedSegment)
    }
  }

  def addTerminalSegment(segmentType: String, text: String) {
    segmentStack.top._2.append(TerminalSegment(segmentType, text))
  }

  def build() = new SegmentedDocument(text, finishedTopLevelSegments.toSeq)
}

sealed abstract class Segment(segmentType: String) {
  def getSegmentsOfType(requestedType: String): Seq[Segment] = {
    this match {
      case NonTerminalSegment(sType, segments) => {
        val matchingSegmentsBelowMe = segments.flatMap(_.getSegmentsOfType(requestedType))
        if (requestedType == sType) Seq(this) ++ matchingSegmentsBelowMe else matchingSegmentsBelowMe
      }
      case TerminalSegment(sType, text) => {
        if (requestedType == sType) Seq(this) else Seq.empty[Segment]
      }
    }
  }

  def getTextSegments(): Seq[String] = {
    this match {
      case NonTerminalSegment(sType, segments) => segments.flatMap(_.getTextSegments)
      case TerminalSegment(sType, text) => Seq(text)
    }
  }
}

case class NonTerminalSegment(segmentType: String, segments: Seq[Segment])
  extends Segment(segmentType)

case class TerminalSegment(segmentType: String, text: String) extends Segment(segmentType)
