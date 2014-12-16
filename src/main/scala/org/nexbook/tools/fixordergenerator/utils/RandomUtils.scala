package org.nexbook.tools.fixordergenerator.utils

import scala.util.Random

/**
 * Created by milczu on 16.12.14.
 */
object RandomUtils {

  def random(start: Double, end: Double): Double = {
    Random.nextDouble() * (end - start) + start
  }
}
