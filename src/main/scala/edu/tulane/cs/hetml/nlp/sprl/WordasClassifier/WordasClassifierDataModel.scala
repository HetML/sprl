package edu.tulane.cs.hetml.nlp.sprl.WordasClassifier

import java.io._

import edu.illinois.cs.cogcomp.saul.datamodel.DataModel
import edu.tulane.cs.hetml.nlp.BaseTypes.{Phrase, Sentence}
import edu.tulane.cs.hetml.nlp.sprl.WordasClassifier.WordasClassifierClassifiers.{ExpressionasClassifer, SingleWordasClassifer}
import edu.tulane.cs.hetml.vision._
import edu.tulane.cs.hetml.nlp.sprl.WordasClassifier.WordasClassifierSensors._
import edu.tulane.cs.hetml.nlp.sprl.WordasClassifier.WordasClassifierConfigurator._

import scala.collection.JavaConversions._
import scala.collection.mutable
import edu.tulane.cs.hetml.nlp.BaseTypes._
/** Created by Umar on 2017-10-20.
  */
object WordasClassifierDataModel extends DataModel {

  val documents = node[Document]
  val sentences = node[Sentence]((s: Sentence) => s.getId)
  val images = node[Image]((i: Image) => i.getId)
  val segments = node[Segment]((s: Segment) => s.getUniqueId)
  val wordsegments = node[WordSegment]
  val allCombinationsWordSegments = node[WordSegment]
  val expressionSegmentPairs = node[Relation]((r: Relation) => r.getId)

  val trainedWordClassifier = new mutable.HashMap[String, SingleWordasClassifer]()
  val trainedWordClassifierIndexMap = new mutable.HashMap[String, Integer]()
  val classifierDirectory = "models/mSpRL/wordclassifer/"

  /*
  Edges
   */
  val imageToSegment = edge(images, segments)
  imageToSegment.addSensor(imageToSegmentMatching _)

  val documentToSentence = edge(documents, sentences)
  documentToSentence.addSensor(documentToSentenceMatching _)

  val documentToImage = edge(documents, images)
  documentToImage.addSensor(documentToImageMatching _)

  val sentenceToSegmentSentencePairs = edge(sentences, expressionSegmentPairs)
  sentenceToSegmentSentencePairs.addSensor(sentenceToSegmentSentencePairsGenerating _)

  val expsegpairToFirstArg = edge(expressionSegmentPairs, sentences)
    expsegpairToFirstArg.addSensor(relationToFirstArgumentMatching _)

  val expsegpairToSecondArg = edge(expressionSegmentPairs, segments)
    expsegpairToSecondArg.addSensor(relationToSecondArgumentMatching _)

  val wordLabel = property(wordsegments) {
    w: WordSegment =>
      if (w.isWordAndSegmentMatching)
        w.getWord
      else
        "None"
  }

  val wordSegFeatures = property(wordsegments, ordered = true) {
    w: WordSegment =>
      w.getSegment.getSegmentFeatures.split(" ").toList.map(_.toDouble)
  }

  val expressionLabel = property(expressionSegmentPairs) {
    r: Relation =>
        if(r.getArgumentId(2)=="isRel") "true" else "false"
  }

  val expressionSegFeatures = property(expressionSegmentPairs, ordered = true) {
    r: Relation => val s = (expressionSegmentPairs(r) ~> expsegpairToSecondArg).head
        s.getSegmentFeatures.split(" ").toList.map(_.toDouble)
  }

  val expressionScore = property(expressionSegmentPairs) {
    r: Relation =>
        r.getArgumentId(3).toDouble
  }

  val expressionActualRelation = property(expressionSegmentPairs) {
    r: Relation =>
      if(r.getArgumentId(2)=="isRel") true else false
  }

  val expressionPredictedRelation = property(expressionSegmentPairs) {
    r: Relation =>
      if(getPredictedSegId(r)==r.getArgumentId(1).split("_")(1)) true else false
  }

  val expressionPredictedScoreVector = property(expressionSegmentPairs) {
    r: Relation =>
      getWordClassifierScoreVector(r)
  }

  def loadWordClassifiers(): Unit = {
    val trainedWordsReader = new WordasClassifierTrainedWordsReader()
    trainedWordsReader.loadTrainedWords(imageDataPath)
    val refexpTrainedWords = (trainedWordsReader.filteredWords ++ trainedWordsReader.clefWords).toList.sorted

    var i = 0
    refexpTrainedWords.foreach(word => {
      val c = new SingleWordasClassifer(word)
      c.modelSuffix = word
      c.modelDir = classifierDirectory
      c.load()
      trainedWordClassifier.put(word, c)
      trainedWordClassifierIndexMap.put(word, i)
      i+=1
    })
  }

  def getPredictedWordSegId(w : WordSegment) : Int = {
    val imgSegs = allCombinationsWordSegments().filter(t => t.getSegment.getAssociatedImageID==w.getSegment.getAssociatedImageID)
      .toList.sortBy(t=> t.getSegment.getSegmentId)
    computeMatrix(imgSegs)
    0
  }

  def getActualWordSegId(w : WordSegment) : Int = {
    w.getSegment.getSegmentId
  }

  def computeMatrix(instances: List[WordSegment]): Int = {

    val scoresMatrix = instances.groupBy(i => i.getWord).map(w => {
      computeScore(w._1, w._2)
    }).toList
    val norm = normalizeScores(scoresMatrix)
    val vector = combineScores(norm)
    val regionId = vector.indexOf(vector.max) + 1
    regionId
  }
  def computeScore(word: String, instances: List[WordSegment]): List[Double] = {
    instances.map(i => {
      getWordClassifierScore(word, i)
    })
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

  def getPredictedSegId(r: Relation) : String = {

    val allSenSegPairs = expressionSegmentPairs().filter(i => i.getArgumentId(0)==r.getArgumentId(0)).toList.sortWith(sortBySegId)

    val scoresVector = allSenSegPairs.map(i => {
      getExpressionWordClassifierScore(i)
    }).toList
    val predictedSegId = scoresVector.indexOf(scoresVector.max) + 1
    predictedSegId.toString
  }

  def getWordClassifierScoreVector(r: Relation) : List[Double] = {

    val sen = expressionSegmentPairs(r) ~> expsegpairToFirstArg head
    val seg = expressionSegmentPairs(r) ~> expsegpairToSecondArg head
    val scoreFeatureArray = Array.fill(trainedWordClassifier.size){0.0}

    sen.getText.split(" ").foreach(w => {
      val ws = new WordSegment(w, seg, false, false, "")
      val (index, score) = getWordClassifierScoreforExpression(w, ws)
      if(index != -1)
        scoreFeatureArray(index) = score
    })
    scoreFeatureArray.toList
  }
  def getExpressionWordClassifierScore(r: Relation) : Double = {
    r.getArgumentId(3).toDouble
  }

  def sortBySegId(r1: Relation, r2: Relation) = {
    Integer.parseInt(r1.getArgumentId(1).split("_")(1)) < Integer.parseInt(r2.getArgumentId(1).split("_")(1))
  }

  def getWordClassifierScoreforExpression(word: String, i: WordSegment) : (Int, Double) = {
    if(trainedWordClassifier.contains(word)) {
      val c = trainedWordClassifier(word)
      val scores = c.classifier.scores(i)
      if(scores.size()>0) {
        val orgValue = scores.toArray.filter(s => s.value.equalsIgnoreCase("true"))
        val index = trainedWordClassifierIndexMap(word)
        (index, orgValue(0).score)
      }
      else {
        (-1, 0.0)
      }
    }
    else
      (-1, 0.0)
  }
}
