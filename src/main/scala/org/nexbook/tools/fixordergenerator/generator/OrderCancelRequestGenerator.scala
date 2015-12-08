package org.nexbook.tools.fixordergenerator.generator

import java.util.UUID

import org.joda.time.{DateTimeZone, DateTime}
import quickfix.field.{TransactTime, ClOrdID, OrigClOrdID}
import quickfix.fix44.{OrderCancelRequest, NewOrderSingle}

/**
 * Created by milczu on 08.12.15.
 */
object OrderCancelRequestGenerator {

  def generate(order: NewOrderSingle): OrderCancelRequest = new OrderCancelRequest(new OrigClOrdID(order.getClOrdID.getValue), clOrdId, order.getSide, currentTransactTime)

  private def clOrdId = new ClOrdID(UUID.randomUUID().toString)

  private def currentTransactTime = new TransactTime(DateTime.now(DateTimeZone.UTC).toDate)
}
