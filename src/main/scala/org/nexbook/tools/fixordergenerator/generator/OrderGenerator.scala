package org.nexbook.tools.fixordergenerator.generator


import java.util.UUID

import org.joda.time.{DateTimeZone, DateTime}
import org.nexbook.tools.fixordergenerator.repository.PriceRepository
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import quickfix.field._
import quickfix.fix44.NewOrderSingle

import scala.util.Random

/**
 * Created by milczu on 15.12.14.
 */
object OrderGenerator {

  val availableSides: List[Side] = List(new Side(Side.BUY), new Side(Side.SELL))
  val availableOrderTypes: List[OrdType] = List(new OrdType(OrdType.MARKET), new OrdType(OrdType.LIMIT))

  def next(count: Int): List[NewOrderSingle] = Seq.fill(count)(next).toList

  def next(): NewOrderSingle = {
    val order = new NewOrderSingle(clOrdId, side, currentTransactTime, ordType)
    order.set(new OrderQty(orderQty))
    order.set(new Symbol(SymbolGenerator.randomSymbol))
    order.set(account)
    ordTypeDependent(order)
    order
  }

  private def clOrdId = new ClOrdID(UUID.randomUUID().toString)

  private def side = Random.shuffle(availableSides.toList).head

  private def currentTransactTime = new TransactTime(DateTime.now(DateTimeZone.UTC).toDate)

  private def ordType = Random.shuffle(availableOrderTypes).head

  private def orderQty = ((Random.nextInt(100) + 1) * 1000).toDouble

  private def account =  new Account("%05d".format((Random.nextInt(500) + 1)))

  private def ordTypeDependent(order: NewOrderSingle) = order.getOrdType.getValue match {
    case OrdType.LIMIT => {
      order.set(new Price(PriceRepository.priceForSymbol(order.getSymbol.getValue) * RandomUtils.random(0.96, 1.04)))
      order
    }
    case _ => order
  }

}
