package org.nexbook.tools.fixordergenerator.generator

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.nexbook.tools.fixordergenerator.fix.{FixMessageSenderActor, FixMessageToSend}
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import org.slf4j.LoggerFactory
import quickfix.Session
import quickfix.fix44.NewOrderSingle

import scala.concurrent.duration._

/**
 * Created by milczu on 08.12.15.
 */
class OrderCancelExecutor(system: ActorSystem, fixMessageSenderActor: ActorRef) extends PostOrderGenerator {

  val logger = LoggerFactory.getLogger(classOf[OrderCancelExecutor])

  val config = ConfigFactory.load().getConfig("org.nexbook.cancelOrder")
  val cancelOrderRate = config.getInt("rate")
  val minDelay = config.getInt("minDelayInSecs")
  val maxDelay = config.getInt("maxDelayInSecs")

  logger.info("OrderCancelExecutor initialized. cancelOrderRate: {}, Delay: {}-{}", cancelOrderRate.toString, minDelay.toString, maxDelay.toString)

  override def afterOrderGenerated(order: NewOrderSingle, session: Session): Unit = scheduleOrderCancelIfNeeded(order, session)

  def scheduleOrderCancelIfNeeded(newOrderSingle: NewOrderSingle, session: Session): Unit = {
    def shouldBeCancelled = RandomUtils.random(0, 100) <= cancelOrderRate

    if (shouldBeCancelled) {
      val orderCancel = OrderCancelRequestGenerator generate newOrderSingle
      val delay = RandomUtils.random(minDelay, maxDelay).toLong
      logger.info("Order with clOrdId: {} will be cancelled with clOrdId: {} in {} secs", orderCancel.getOrigClOrdID.getValue, orderCancel.getClOrdID.getValue, delay.toString)

      import system.dispatcher
      system.scheduler.scheduleOnce(delay seconds, fixMessageSenderActor, new FixMessageToSend(orderCancel, session))
    }
  }

}
