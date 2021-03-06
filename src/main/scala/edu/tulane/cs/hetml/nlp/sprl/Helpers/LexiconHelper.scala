package edu.tulane.cs.hetml.nlp.sprl.Helpers

import java.io.{File, IOException, PrintWriter}

import scala.io.Source
import scala.collection.JavaConversions._

/**
  * Created by taher on 2017-04-07.
  */
object LexiconHelper {
  var path = ""
  def createSpatialIndicatorLexicon(xmlReader: SpRLXmlReader, minFreq: Int = 1): Unit = {
    val lexFile = new File(path)
    xmlReader.reader.setPhraseTagName("SPATIALINDICATOR")
    val indicators = xmlReader.reader.getPhrases()
    val sps = indicators.groupBy(_.getText.toLowerCase.trim)
      .map { case (key, list) => (key, list.size, list) }
      .filter(_._2 >= minFreq)
      .map(_._1).toList

    val writer = new PrintWriter(lexFile)
    sps.sorted.foreach(p => writer.println(p))
    writer.close()
  }

  lazy val spatialIndicatorLexicon: List[String] = {

    val lexFile = new File(path)
    if (!lexFile.exists())
      throw new IOException(s"cannot find ${lexFile.getAbsolutePath} file")
    Source.fromFile(lexFile).getLines().toList
  }

}
