package org.nexbook.tools.fixordergenerator.generator

import scala.util.Random

/**
 * Created by milczu on 15.12.14.
 */
object SymbolGenerator {

  private val symbols = List("EUR/USD", "AUD/USD", "GBP/USD", "USD/JPY", "EUR/JPY", "EUR/GBP", "USD/CAD", "USD/CHF")

  def randomSymbol = Random.shuffle(symbols.toList).head

  def all = symbols
}
