package org.nexbook.tools.fixordergenerator.generator

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.util.Random

/**
 * Created by milczu on 15.12.14.
 */
object SymbolGenerator {

  private val symbols = ConfigFactory.load().getStringList("org.nexbook.symbols").asScala.toList

  def randomSymbol = Random.shuffle(symbols).head

  def all = symbols
}
