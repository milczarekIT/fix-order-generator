package org.nexbook.tools.fixordergenerator.generator


import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import quickfix.field._
import quickfix.fix44.NewOrderSingle

import scala.util.Random

/**
  * Created by milczu on 15.12.14.
  */
class OrderGenerator(symbolGenerator: SymbolGenerator) {

  val availableSides: List[Side] = List(new Side(Side.BUY), new Side(Side.SELL))
  val availableOrderTypes: List[(Int, OrdType)] = List(26 -> new OrdType(OrdType.MARKET), 107 -> new OrdType(OrdType.LIMIT))

  def generate(count: Int): List[NewOrderSingle] = Seq.fill(count)(generate()).toList

  def generate(): NewOrderSingle = {
	val order = new NewOrderSingle(clOrdId, side, currentTransactTime, ordType)
	order.set(new OrderQty(orderQty))
	order.set(new Symbol(symbolGenerator.randomSymbol))
	order.set(account)
	ordTypeDependent(order)
	order
  }

  private def clOrdId = new ClOrdID(UUID.randomUUID().toString)

  private def side = Random.shuffle(availableSides.toList).head

  private def currentTransactTime = new TransactTime(DateTime.now(DateTimeZone.UTC).toDate)

  private def ordType = RandomUtils.weightedRandom(availableOrderTypes)

  private def orderQty = ((Random.nextInt(100) + 1) * 1000).toDouble

  private def account = new Account("%05d".format(Random.nextInt(500) + 1))

  private def ordTypeDependent(order: NewOrderSingle) = order.getOrdType.getValue match {
	case OrdType.LIMIT =>
	  order.set(new Price(PriceGenerator.generatePrice(order.getSide.getValue, order.getSymbol.getValue)))
	  order
	case _ => order
  }

}
