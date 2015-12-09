package org.nexbook.tools.fixordergenerator.generator

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import quickfix.field.{ClOrdID, OrigClOrdID, TransactTime}
import quickfix.fix44.component.{Instrument, OrderQtyData}
import quickfix.fix44.{NewOrderSingle, OrderCancelRequest}

/**
 * Created by milczu on 08.12.15.
 */
object OrderCancelRequestGenerator {

  def generate(order: NewOrderSingle): OrderCancelRequest = {
    val orderCancel = new OrderCancelRequest(new OrigClOrdID(order.getClOrdID.getValue), clOrdId, order.getSide, currentTransactTime)
    orderCancel.set(new Instrument(order.getSymbol))
    val qtyData = new OrderQtyData()
    qtyData.set(order.getOrderQty)
    orderCancel.set(qtyData)
    orderCancel
  }

  private def clOrdId = new ClOrdID(UUID.randomUUID().toString)

  private def currentTransactTime = new TransactTime(DateTime.now(DateTimeZone.UTC).toDate)
}
