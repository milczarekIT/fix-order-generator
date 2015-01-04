package org.nexbook.tools.fixordergenerator.repository

import quickfix.field.Side

/**
 * Created by milczu on 16.12.14.
 */
object PriceRepository {

  val defaultPrices = Map("EUR/USD" -> (1.24885, 1.24888), "AUD/USD" -> (0.821075, 0.821092), "GBP/USD" -> (1.57265, 1.57278), "USD/JPY" -> (117.205813, 117.205828), "EUR/JPY" -> (146.37248, 146.37258), "EUR/GBP" -> (0.794105491, 0.794105499), "USD/CAD" -> (1.16375011, 1.16375019), "USD/CHF" -> (0.961675315, 0.961675478))

  var prices: Map[String, (Double, Double)] = defaultPrices

  def priceForSymbol(symbol: String, side: Side): Double = side.getValue match {
    case Side.BUY => prices.getOrElse(symbol, defaultPrices(symbol))._1
    case Side.SELL => prices.getOrElse(symbol, defaultPrices(symbol))._2
  }

  def updatePrices(newPrices: Map[String, (Double, Double)]) = if(!newPrices.isEmpty) prices = newPrices


}
