package org.nexbook.tools.fixordergenerator.generator

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import quickfix.field._
import quickfix.fix44.NewOrderSingle

import scala.util.Random

/**
 * Created by milczu on 15.12.14.
 */
object OrderGenerator {

  def next(count: Int): List[NewOrderSingle] = Seq.fill(count)(next).toList

  def next(): NewOrderSingle = {
    val order = new NewOrderSingle(clOrdId, side, currentTransactTime, ordType)
    order.set(new OrderQty(orderQty))
    order.set(new Symbol(SymbolGenerator.randomSymbol))
    order
  }

  private def clOrdId = new ClOrdID(UUID.randomUUID().toString)

  private def side = if (Random.nextInt() == 0) new Side(Side.BUY) else new Side(Side.SELL)

  private def currentTransactTime = new TransactTime(DateTime.now(DateTimeZone.UTC).toDate)

  private def ordType = if (Random.nextInt() == 0) new OrdType(OrdType.MARKET) else new OrdType(OrdType.LIMIT)

  private def orderQty = ((Random.nextInt(100) + 1) * 1000).toDouble

}
