package org.allenai.common

import au.com.bytecode.opencsv.CSVReader

import java.io.{ BufferedInputStream, BufferedReader, File, FileInputStream, InputStreamReader }

import scala.collection.JavaConverters._
import scala.io.{ BufferedSource, Codec, Source }

/** Various convenient utilities for reading files and resources. */
object FileUtils extends Logging {

  /** Get a file as a non-lazy sequence of lines. */
  def getFileAsLines(file: File)(implicit codec: Codec): Seq[String] = {
    logger.debug(s"Loading file ${file.getName}")
    // use toVector to force stream to be processed
    Resource.using(Source.fromFile(file)(codec))(_.getLines().toVector)
  }

  /** Read a CSV file as a non-lazy sequence (rows) of sequence (columns) of strings. */
  def getCSVContentFromFile(file: File)(implicit codec: Codec): Seq[Seq[String]] = {
    logger.debug(s"Loading CSV file ${file.getName}")
    val csvReader = new CSVReader(new InputStreamReader(new FileInputStream(file), codec.charSet))
    Resource.using(csvReader)(_.readAll.asScala.map(_.toVector))
  }

  /** Get a resource file for a given class as a Stream. Caller is responsible for closing this
    * stream.
    */
  def getResourceAsStream(clazz: Class[_], name: String): BufferedInputStream = {
    val inputStream = clazz.getResourceAsStream(name)
    require(inputStream != null, s"Could not find file $name as resource for $clazz")
    new BufferedInputStream(inputStream)
  }

  /** Get a resource file for a given class as a Reader. Caller is responsible for closing this
    * reader.
    */
  def getResourceAsReader(clazz: Class[_], name: String)(implicit codec: Codec): BufferedReader = {
    new BufferedReader(new InputStreamReader(getResourceAsStream(clazz, name), codec.charSet))
  }

  /** Get a resource file for a given class as a buffered Source. Caller is responsible for closing
    * this source.
    */
  def getResourceAsSource(clazz: Class[_], name: String)(implicit codec: Codec): BufferedSource = {
    Source.fromInputStream(getResourceAsStream(clazz, name))(codec)
  }

  /** Get a resource file for a given class as a non-lazy sequence of lines. */
  def getResourceAsLines(clazz: Class[_], name: String)(implicit codec: Codec): Seq[String] = {
    logger.debug(s"Loading resource $name")
    // use toVector to force stream to be processed
    Resource.using(getResourceAsSource(clazz, name)(codec))(_.getLines().toVector)
  }

  /** Read a CSV resource file for a given class as a non-lazy sequence (rows) of sequence (columns)
    * of strings.
    */
  def getCSVContentFromResource(
    clazz: Class[_],
    name: String
  )(implicit codec: Codec): Seq[Seq[String]] = {
    logger.debug(s"Loading CSV resource $name")
    val csvReader = new CSVReader(getResourceAsReader(clazz, name)(codec))
    Resource.using(csvReader)(_.readAll.asScala.map(_.toVector))
  }
}
