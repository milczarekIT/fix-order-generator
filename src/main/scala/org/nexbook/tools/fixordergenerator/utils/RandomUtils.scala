package org.nexbook.tools.fixordergenerator.utils

import scala.util.Random

/**
 * Created by milczu on 16.12.14.
 */
object RandomUtils {

  def random(start: Double, end: Double): Double = {
    Random.nextDouble() * (end - start) + start
  }

  def weightedRandom[T](list: List[(Int, T)]): T = {
    val weightSum = list.foldLeft(0)(_ + _._1)
    val randomVal = RandomUtils.random(0.0, weightSum)
    val weightedMap = list.view.zipWithIndex.map(ziped => (list.take(ziped._2 + 1).foldLeft(0.0)(_ + _._1), list(ziped._2)._2)).toMap
    weightedMap.filterKeys(randomVal < _).values.head
  }
}
