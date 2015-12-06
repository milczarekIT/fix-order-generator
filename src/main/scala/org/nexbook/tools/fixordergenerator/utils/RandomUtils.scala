package org.nexbook.tools.fixordergenerator.utils

import scala.util.Random

/**
 * Created by milczu on 16.12.14.
 */
object RandomUtils {

  /**
   * @param start inclusive
   * @param end inclusive
   */
  def random(start: Int, end: Int):Int = Random.nextInt(end - start + 1) + start

  def random(start: Double, end: Double): Double = Random.nextDouble() * (end - start) + start

  def weightedRandom[T](list: List[(Int, T)]): T = {
    val weightSum = list.foldLeft(0)(_ + _._1)
    val randomVal = RandomUtils.random(0.0, weightSum)
    val weightedMap = list.view.zipWithIndex.map(zipped => (list.take(zipped._2 + 1).foldLeft(0.0)(_ + _._1), list(zipped._2)._2)).toMap
    weightedMap.filterKeys(randomVal < _).values.head
  }
}
