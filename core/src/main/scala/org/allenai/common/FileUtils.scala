package org.allenai.common

import au.com.bytecode.opencsv.CSVReader

import java.io._

import scala.collection.JavaConverters._
import scala.io.{ BufferedSource, Codec, Source }

import java.nio.charset.Charset

/** Various convenient utilities for reading files and resources. */
object FileUtils extends Logging {

  /** Get a file as a non-lazy sequence of lines. */
  def getFileAsLines(file: File)(codec: Codec): Seq[String] = {
    logger.debug(s"Loading file ${file.getName}")
    // use toVector to force stream to be processed
    Resource.using(Source.fromFile(file)(codec))(_.getLines().toVector)
  }

  /** Read a CSV file as a non-lazy sequence (rows) of sequence (columns) of strings. */
  def getCSVContentFromFile(file: File): Seq[Seq[String]] = {
    logger.debug(s"Loading CSV file ${file.getName}")
    val csvReader = new CSVReader(new FileReader(file))
    Resource.using(csvReader)(_.readAll.asScala.map(_.toVector))
  }

  /** Get a resource file for a given class as a Stream. Caller is responsible for closing this
    * stream.
    */
  def getResourceAsStream(name: String, c: Class[_]): BufferedInputStream = {
    new BufferedInputStream(c.getClassLoader.getResourceAsStream(name))
  }

  /** Get a resource file for a given class as a Reader. Caller is responsible for closing this
    * reader.
    */
  def getResourceAsReader(name: String, c: Class[_])(codec: Codec): BufferedReader = {
    val localCharset = codec.asInstanceOf[Charset]
    new BufferedReader(new InputStreamReader(getResourceAsStream(name, c), localCharset))
  }

  /** Get a resource file for a given class as a buffered Source. Caller is responsible for closing
    * this source.
    */
  def getResourceAsSource(name: String, c: Class[_])(codec: Codec): BufferedSource = {
    Source.fromInputStream(getResourceAsStream(name, c))(codec)
  }

  /** Get a resource file for a given class as a non-lazy sequence of lines. */
  def getResourceAsLines(name: String, c: Class[_])(codec: Codec): Seq[String] = {
    logger.debug(s"Loading resource $name")
    // use toVector to force stream to be processed
    Resource.using(getResourceAsSource(name, c)(codec))(_.getLines().toVector)
  }

  /** Read a CSV resource file for a given class as a non-lazy sequence (rows) of sequence (columns)
    * of strings.
    */
  def getCSVContentFromResource(name: String, c: Class[_])(codec: Codec): Seq[Seq[String]] = {
    logger.debug(s"Loading CSV resource $name")
    val csvReader = new CSVReader(getResourceAsReader(name, c)(codec))
    Resource.using(csvReader)(_.readAll.asScala.map(_.toVector))
  }
}
