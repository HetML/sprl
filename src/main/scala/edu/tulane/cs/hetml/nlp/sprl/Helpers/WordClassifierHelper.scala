package edu.tulane.cs.hetml.nlp.sprl.Helpers

import edu.tulane.cs.hetml.vision._

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import edu.tulane.cs.hetml.nlp.sprl.WordasClassifier.WordasClassifierClassifiers._
import edu.tulane.cs.hetml.nlp.sprl.mSpRLConfigurator._
import edu.tulane.cs.hetml.nlp.sprl.MultiModalSpRLSensors._
import scala.collection.mutable
import scala.io.Source

class WordClassifierHelper {

  val trainedWordClassifier = new HashMap[String, SingleWordasClassifer]()
  val classifierDirectory = s"models/mSpRL/wordclassifer/"

  val CLEFGoogleNETReaderHelper = new CLEFGoogleNETReader(imageDataPath)
  val refexpTrainedWords = new RefExpTrainedWordReader(imageDataPath).filteredWords

  val allsegments = getSegmentFeatures()
  val wordToClosetClassifier = new mutable.HashMap[String, String]()
  val useWord2Vec = true
  val missedTrainedWords = new CLEFAnnotationReader(imageDataPath).missingWords

  //load Trained classifiers
  loadAllTrainedClassifiers(true)

  def getSegmentFeatures() : List[Segment] = {
    val filename = imageDataPath + "/ImageSegmentsNewFeatures.txt"
    Source.fromFile(filename).getLines.map{
      l =>
        val f = l.split(",").toList
        new Segment(f(0), f(1).toInt,  f.takeRight(2).mkString(" "), "", false)
    }.toList
  }

  def getScore(phrase: String, segment: Segment) : Double = {

    val testInstances = new ListBuffer[WordSegment]()
    val imgSegs = allsegments.filter(s=> s.getAssociatedImageID == segment.getAssociatedImageID)

    //Create all possible combinations M x N
    val segPairs = phrase.split(" ").distinct.flatMap(tok => {
      imgSegs.map(is => {
        new WordSegment(tok, is, false, false, "")
      })
    }).toList
    val scoreVector = computeMatrix(segPairs)
    if (segment.getSegmentId <= scoreVector.size) {
      scoreVector(segment.getSegmentId - 1)
    } else {
      println("Warning: Mismatched Segment Id")
      0.0
    }
  }

  def computeMatrix(instances: List[WordSegment]): List[Double] = {
    val scoresMatrix = instances.groupBy(i => i.getWord).map(w => {
      computeScore(w._1, w._2, useWord2Vec)
    }).toList
    val norm = normalizeScores(scoresMatrix)
    val vector = combineScores(norm)
    vector
  }

  def computeScore(word: String, instances: List[WordSegment], useWord2Vec: Boolean): List[Double] = {
    val w = instances.map(i => {
      if(useWord2Vec) {
        val score = getWordClassifierScore(word, i)
        if(score!=0.0)
          score
        else {
          val predictedWord = getClosetClassifier(word)
          if(predictedWord!="") {
            getWordClassifierScore(predictedWord, i)
          }
          else
            0.0
        }
      }
      else
        getWordClassifierScore(word, i)
    })
    w
  }

  def loadAllTrainedClassifiers(loadMissedTrained: Boolean): Unit ={
    refexpTrainedWords.foreach(word => {
      val c = new SingleWordasClassifer(word)
      c.modelSuffix = word
      c.modelDir = classifierDirectory
      c.load()
      trainedWordClassifier.put(word, c)
    })
    if(loadMissedTrained) {
      missedTrainedWords.foreach(word => {
        val c = new SingleWordasClassifer(word)
        c.modelSuffix = word
        c.modelDir = classifierDirectory
        c.load()
        trainedWordClassifier.put(word, c)
      })
    }
  }

  def getClosetClassifier(word: String) : String = {
    // RefExp Trained words
    if(wordToClosetClassifier.contains(word))
      return wordToClosetClassifier(word)

    val threshold = 0.3

    val scoreVector = refexpTrainedWords.map(r => {
      getGoogleSimilarity(r, word)
    })

    if(scoreVector.max > threshold) {
      val index = scoreVector.indexOf(scoreVector.max)
      val predictedWord = refexpTrainedWords.get(index)
      wordToClosetClassifier.put(word, predictedWord)
      predictedWord
    }
    else {
      wordToClosetClassifier.put(word, "")
      ""
    }
  }

  def getWordClassifierScore(word: String, i: WordSegment) : Double ={
    if(trainedWordClassifier.contains(word)) {
      val c = trainedWordClassifier(word)
      val scores = c.classifier.scores(i)
      if(scores.size()>0) {
        val orgValue = scores.toArray.filter(s => s.value.equalsIgnoreCase("true"))
        orgValue(0).score
      }
      else {
        0.0
      }
    }
    else
      0.0
  }
  def normalizeScores(scoreMatrix: List[List[Double]]):List[List[Double]] = {
      scoreMatrix.map(w=> w.map(s=>if(s == 0) 0.0 else s/w.map(Math.abs).sum))
  }

  def combineScores(scoreMatrix: List[List[Double]]): List[Double] = {
      scoreMatrix.transpose.map(_.sum)
  }
}

