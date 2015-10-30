package org.allenai.common.indexing

import scala.io.{Source, Codec}
import scala.util.matching.Regex
import java.io.File

object ParsingUtils {

  /** Splits a file based on tags of the form <$splitString> ... </$splitString> and performs a
   *  function on each segment. If a tag is missing for whatever reason will treat the next tag
   *  (whether it is <$splitString> or </$splitString> as the delimiter of this segemnt.
   *  @param inputFile file to be segmented
   *  @param splitString string that defines doc delimiting tags
   *  @param splitRegex passed in so that the regex does not have to be built with each call (should
    *                    look like """</?splitString>""")
   *  @param segmentFunction function to be called on each segment
   *  @param bufferSize size of readingBuffer
   */
  def splitOnTag(inputFile: File, splitString: String, splitRegex: Regex,
    segmentFunction: String => Unit, bufferSize: Int, codec: Codec): Unit = {
    val lines = Source.fromFile(inputFile, bufferSize = bufferSize)(codec).getLines()
    val endOfLastLine = new StringBuilder("")
    var inDocFlag = false
    for (currentLine <- lines if !currentLine.trim.equals("")) {
      val docs = splitRegex.split(currentLine)
      if (docs.nonEmpty) {
        if (docs.length == 1) {
          endOfLastLine.append("\n" + docs.head)
        } else {
          processIfValid(endOfLastLine.append("\n" + docs.head).toString())
          endOfLastLine.setLength(0)
          if (docs.tail.length >= 2) {
            docs.tail.init.foreach(doc => processIfValid(doc))
          }
          if (!(currentLine.endsWith(s"<$splitString>") |
            currentLine.endsWith(s"</$splitString>"))) {
            endOfLastLine.setLength(0)
            endOfLastLine.append(docs.last)
            inDocFlag = true
          } else {
            processIfValid(docs.last)
          }
        }
      }
    }

    /** Ignores tag fragments created by parser above.*/
    def processIfValid(input: String): Unit = {
      val trimmed = input.trim
      if (!trimmed.equals("") && !trimmed.equals(s"""</$splitString><$splitString>""") &&
        !trimmed.equals(s"""<$splitString>""") && !trimmed.equals(s"""</$splitString>""")) {
        segmentFunction(input)
      }
    }

  }

}
