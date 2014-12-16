package org.nexbook.tools.fixordergenerator.repository

/**
 * Created by milczu on 16.12.14.
 */
object PriceRepository {

  val prices = Map("EUR/USD" -> 1.24885, "AUD/USD" -> 0.821075, "GBP/USD" -> 1.57265, "USD/JPY" -> 117.205813, "EUR/JPY" -> 146.37248, "EUR/GBP" -> 0.794105491, "USD/CAD" -> 1.16375011, "USD/CHF" -> 0.961675315)

  def priceForSymbol(symbol: String): Double = prices(symbol)
}
