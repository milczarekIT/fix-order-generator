package org.nexbook.tools.fixordergenerator.generator

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.nexbook.tools.fixordergenerator.fix.{FixMessageToSend, FixMessageSenderActor}
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import org.slf4j.LoggerFactory
import quickfix.fix44.NewOrderSingle
import quickfix.{Message, Session}

import scala.concurrent.duration._

/**
 * Created by milczu on 08.12.15.
 */
class OrderCancelExecutor extends PostOrderGenerator {

  val logger = LoggerFactory.getLogger(classOf[OrderCancelExecutor])

  val system = ActorSystem("OrderCancelExecutorSystem")
  val orderCancelFixSenderActor = system.actorOf(Props[FixMessageSenderActor], name = "listener")

  val config = ConfigFactory.load().getConfig("org.nexbook.cancelOrder")
  val cancelOrderRate = config.getInt("rate")
  val minDelay = config.getInt("minDelayInSecs")
  val maxDelay = config.getInt("minDelayInSecs")

  logger.info("OrderCancelExecutor initialized. cancelOrderRate: {}, Delay: {}-{}", cancelOrderRate.toString, minDelay.toString, maxDelay.toString)

  override def afterOrderGenerated(order: NewOrderSingle, session: Session): Unit = scheduleOrderCancelIfNeeded(order, session)

  def scheduleOrderCancelIfNeeded(newOrderSingle: NewOrderSingle, session: Session): Unit = {
    def shouldBeCancelled = RandomUtils.random(0, cancelOrderRate) == 0

    if (shouldBeCancelled) {
      val orderCancel = OrderCancelRequestGenerator generate newOrderSingle
      val delay = RandomUtils.random(minDelay, maxDelay).toLong
      logger.info("Order with clOrdId: {} will be cancelled with clOrdId: {} in {} secs", orderCancel.getOrigClOrdID.getValue, orderCancel.getClOrdID.getValue, delay.toString)

      import system.dispatcher
      system.scheduler.scheduleOnce(delay seconds, orderCancelFixSenderActor, new FixMessageToSend(orderCancel, session))
    }
  }

}