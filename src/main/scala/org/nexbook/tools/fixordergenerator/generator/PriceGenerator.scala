package org.nexbook.tools.fixordergenerator.generator

import org.nexbook.tools.fixordergenerator.repository.PriceRepository
import quickfix.field.Side

import scala.math.BigDecimal.RoundingMode
import scala.util.Random

/**
  * Created by milczu on 10.12.15
  */
class PriceGenerator(repository: PriceRepository) {

  var allowedPrices: Map[String, (List[Double], List[Double])] = generateAllowedPrices(repository.findAll)

  def updatePrices(prices: Map[String, (Double, Double)]) = allowedPrices = generateAllowedPrices(prices)

  private def generateAllowedPrices(prices: Map[String, (Double, Double)]): Map[String, (List[Double], List[Double])] = {
	val spreads = prices.map(e => e._1 -> BigDecimal(e._2._2 - e._2._1).setScale(5, RoundingMode.HALF_UP))
	val rangeWidth = spreads.map(e => e._1 -> e._2 * 5)
	val steps = rangeWidth.map(e => e._1 -> BigDecimal(e._2.toDouble / 30.00).setScale(5, RoundingMode.HALF_UP))

	def allowedPrices(symbol: String): (List[Double], List[Double]) = {
	  val price = prices(symbol)
	  val step = steps(symbol)

	  val startSellRange = price._1
	  val endSellRange = (startSellRange + (step * 30)).toDouble
	  val sellPrices = Range.Double(startSellRange, endSellRange, step.toDouble).toList

	  val endBuyRange = price._2
	  val startBuyRange = (endBuyRange - (step * 30)).toDouble
	  val buyPrices = Range.Double(startBuyRange, endBuyRange, step.toDouble).toList
	  (buyPrices, sellPrices)
	}
	prices.map(e => e._1 -> allowedPrices(e._1))
  }

  def generatePrice(side: Char, symbol: String): Double = {
	val pricesSideSymbol = if (Side.BUY == side) allowedPrices(symbol)._1 else allowedPrices(symbol)._2
	Random.shuffle(pricesSideSymbol).head
  }


}
