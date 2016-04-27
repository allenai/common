package org.allenai.common

import _root_.au.com.bytecode.opencsv.CSVReader

import java.io._

import scala.collection.JavaConverters._
import scala.io.{ BufferedSource, Codec, Source }

/** Various convenient utilities for reading files and resources. */
object FileUtils extends Logging {

  /** Get a file as a buffered source. */
  def getFileAsSource(file: File, codecOpt: Option[Codec] = None): BufferedSource = {
    codecOpt match {
      case Some(codec) => Source.fromFile(file)(codec)
      case None => Source.fromFile(file)
    }
  }

  /** Get a file as a sequence of lines. */
  def getFileAsLines(file: File, codecOpt: Option[Codec] = None): Seq[String] = {
    logger.debug(s"Loading file ${file.getName}")
    Resource.using(getFileAsSource(file, codecOpt)) { input =>
      val lines = input.getLines().toVector // convert to vector to force stream to be processed
      logger.trace(s"lines:\n\t${lines.mkString("\n\t")}")
      lines
    }
  }

  /** Read a CSV file into a sequence (rows) of sequence (columns) of strings. */
  def getCSVContentFromFile(file: File): Seq[Seq[String]] = {
    logger.debug(s"Loading CSV file ${file.getName}")
    val csvReader = new CSVReader(new FileReader(file))
    Resource.using(csvReader)(_.readAll.asScala.map(_.toSeq))
  }

  /** Get a resource file as a Stream. Caller is responsible for closing this stream. */
  def getResourceAsStream(name: String): BufferedInputStream = {
    new BufferedInputStream(getClass.getClassLoader.getResourceAsStream(name))
  }

  /** Get a resource file as a Reader. Caller is responsible for closing this reader. */
  def getResourceAsReader(name: String): BufferedReader = {
    new BufferedReader(new InputStreamReader(getResourceAsStream(name)))
  }

  /** Get a resource file as a buffered Source. Caller is responsible for closing this stream. */
  def getResourceAsSource(name: String, codecOpt: Option[Codec] = None): BufferedSource = {
    codecOpt match {
      case Some(codec) => Source.fromInputStream(getResourceAsStream(name))(codec)
      case None => Source.fromInputStream(getResourceAsStream(name))
    }
  }

  /** Get a resource file as a sequence of lines. */
  def getResourceAsLines(name: String, codecOpt: Option[Codec] = None): Seq[String] = {
    logger.debug(s"Loading resource $name")
    Resource.using(getResourceAsSource(name, codecOpt)) { input =>
      val lines = input.getLines().toVector // convert to vector to force stream to be processed
      logger.trace(s"lines:\n\t${lines.mkString("\n\t")}")
      lines
    }
  }

  /** Read a CSV resource file into a sequence (rows) of sequence (columns) of strings. */
  def getCSVContentFromResource(name: String): Seq[Seq[String]] = {
    logger.debug(s"Loading CSV resource $name")
    val csvReader = new CSVReader(getResourceAsReader(name))
    Resource.using(csvReader)(_.readAll.asScala.map(_.toSeq))
  }

}
