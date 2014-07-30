package org.allenai.common

/** Functions to help with statistical analysis of results.
  */
object Analysis {

  /** Compute the area under curve from yield-precision points.
    *
    * @return  a yield, precision point
    */
  def areaUnderCurve(points: Seq[(Int, Double)]): Double = {
    val it = points.iterator.buffered
    var cur = (0, 1.0)
    var area = 0.0
    while (it.hasNext) {
      // save last point
      val (lastYld, lastPrc) = cur

      // increment iterator
      cur = it.next
      val (yld, prc) = cur

      area += 0.5 * (yld - lastYld) * (prc + lastPrc)
    }

    area
  }

  /** Compute precision yield points for each yield value.
    * Scores should be ordered by confidence, descending.
    *
    * @return  a sequence of (yield, precision) points
    */
  def precisionYield(scores: Seq[Boolean]): Seq[(Int, Double)] = {
    var correct = 0
    var incorrect = 0
    var points = List.empty[(Int, Double)]

    for (score <- scores) {
      if (score) {
        correct = correct + 1

        val point = (correct, precision(correct, incorrect))
        points ::= point
      } else {
        incorrect = incorrect + 1
      }
    }

    points.reverse
  }

  /** Compute precision yield points for each change in the first part of the tuple.
    * For example, if the first part of the tuple is a confidence value we would have
    * precision yield points for each change in confidence.
    *
    * Scored examples are presumed to be in sorted order by T, descending.
    *
    * @return  a triple of T, the yield, and the precision
    */
  def precisionYieldMeta[T](scores: Seq[(T, Boolean)]): Seq[(T, Int, Double)] = {
    if (scores.length == 0) {
      List()
    } else {
      var correct = 0
      var incorrect = 0
      var points = List[(T, Int, Double)]()
      var previous = scores.head._1

      var i = 0
      for ((meta, score) <- scores) {
        if (score) correct = correct + 1
        else incorrect = incorrect + 1

        if (meta != previous || i == scores.length - 1) {
          previous = meta

          val point = (meta, correct, precision(correct, incorrect))
          points ::= point
        }

        i = i + 1
      }

      points.reverse
    }
  }

  /** Compute precision from counts of correct and incorrect examples.
    */
  def precision(correct: Int, incorrect: Int): Double = {
    require(correct >= 0 && incorrect >= 0, "arguments must not be negative.")
    require(correct + incorrect > 0, "There must be at least one example.")

    correct.toDouble / (correct + incorrect).toDouble
  }

  /** Compute precision from a series of evaluations.
    */
  def precision(scores: Seq[Boolean]): Double = {
    require(!scores.isEmpty, "scores cannot be empty")

    val correct = scores.count(_ == true)
    precision(correct, scores.size - correct)
  }
}
