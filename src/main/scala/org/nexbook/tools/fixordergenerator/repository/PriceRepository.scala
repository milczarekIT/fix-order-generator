package org.nexbook.tools.fixordergenerator.repository

import quickfix.field.Side

/**
  * Created by milczu on 16.12.14.
  */
class PriceRepository(private val prices: Map[String, (Double, Double)]) {

  val defaultPrices = Map("EUR/USD" ->(1.24885, 1.24988), "AUD/USD" ->(0.82107, 0.82119),
	"GBP/USD" ->(1.57265, 1.57378), "USD/JPY" ->(117.20581, 117.20592),
	"EUR/JPY" ->(146.37248, 146.37358), "EUR/GBP" ->(0.79410, 0.79421),
	"USD/CAD" ->(1.16361, 1.16379), "USD/CHF" ->(0.96167, 0.96179),
	"USD/RUB" ->(74.4121, 74.4493), "USD/MXN" ->(17.4701, 17.4901),
	"EUR/CHF" ->(1.08531, 1.08601), "NZD/USD" ->(0.66280, 0.66319)
  )

  def priceForSymbol(symbol: String, side: Side): Double = side.getValue match {
	case Side.BUY => prices.getOrElse(symbol, defaultPrices(symbol))._1
	case Side.SELL => prices.getOrElse(symbol, defaultPrices(symbol))._2
  }

  def findAll: Map[String, (Double, Double)] = if (prices.isEmpty) defaultPrices else prices

}
