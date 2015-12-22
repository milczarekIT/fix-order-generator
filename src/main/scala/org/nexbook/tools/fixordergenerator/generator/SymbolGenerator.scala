package org.nexbook.tools.fixordergenerator.generator

import scala.util.Random

/**
  * Created by milczu on 15.12.14.
  */
class SymbolGenerator(symbols: List[String]) {

  def randomSymbol = Random.shuffle(symbols).head

}
