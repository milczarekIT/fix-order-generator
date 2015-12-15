package org.nexbook.tools.fixordergenerator.generator

import akka.actor.{Actor, ActorRef, ActorSystem}
import com.typesafe.config.Config
import org.nexbook.tools.fixordergenerator.fix.FixMessageWithSession
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import org.slf4j.LoggerFactory
import quickfix.Session
import quickfix.fix44.NewOrderSingle

import scala.concurrent.duration._

/**
 * Created by milczu on 08.12.15.
 */
class OrderCancelExecutor(system: ActorSystem, fixMessageSenderActor: ActorRef, config: Config) extends Actor {

  val logger = LoggerFactory.getLogger(classOf[OrderCancelExecutor])

  val cancelOrderRate = config.getInt("rate")
  val minDelay = config.getInt("minDelayInMillis")
  val maxDelay = config.getInt("maxDelayInMillis")

  logger.info("OrderCancelExecutor initialized. cancelOrderRate: {}, Delay: {}-{}", cancelOrderRate.toString, minDelay.toString, maxDelay.toString)


  override def receive: Receive = {
    case p: FixMessageWithSession => scheduleOrderCancelIfNeeded(p.message.asInstanceOf[NewOrderSingle], p.session)
  }

  def scheduleOrderCancelIfNeeded(newOrderSingle: NewOrderSingle, session: Session): Unit = {
    def shouldBeCancelled = RandomUtils.random(0, 100) <= cancelOrderRate

    if (shouldBeCancelled) {
      val orderCancel = OrderCancelRequestGenerator generate newOrderSingle
      val delay = RandomUtils.random(minDelay, maxDelay).toLong
      logger.info("Order with clOrdId: {} will be cancelled with clOrdId: {} in {} secs", orderCancel.getOrigClOrdID.getValue, orderCancel.getClOrdID.getValue, delay.toString)

      import system.dispatcher
      system.scheduler.scheduleOnce(delay millis, fixMessageSenderActor, FixMessageWithSession(orderCancel, session))
    }
  }

}
