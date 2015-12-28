package org.nexbook.tools.fixordergenerator.generator

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorRef, ActorSystem}
import com.typesafe.config.Config
import org.nexbook.tools.fixordergenerator.app.OrderCountingGenerator
import org.nexbook.tools.fixordergenerator.fix.FixMessageSender.FixMessageWithSession
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import org.slf4j.LoggerFactory
import quickfix.Session
import quickfix.fix44.NewOrderSingle

import scala.concurrent.duration._

/**
  * Created by milczu on 08.12.15.
  */
class OrderCancelExecutor(system: ActorSystem, fixMessageSenderActor: ActorRef, val generatorConfig: Config, val orderCounter: AtomicLong) extends Actor with OrderCountingGenerator {

  val logger = LoggerFactory.getLogger(classOf[OrderCancelExecutor])
  val cancelConfig = generatorConfig.getConfig("cancelOrder")
  val cancelOrderRate = cancelConfig.getInt("rate")
  val minDelay = cancelConfig.getInt("minDelayInMillis")
  val maxDelay = cancelConfig.getInt("maxDelayInMillis")

  logger.info(s"OrderCancelExecutor initialized. cancelOrderRate: $cancelOrderRate, Delay: $minDelay-$maxDelay")


  override def receive: Receive = {
	case p: FixMessageWithSession => scheduleOrderCancelIfNeeded(p.message.asInstanceOf[NewOrderSingle], p.session)
  }

  def scheduleOrderCancelIfNeeded(newOrderSingle: NewOrderSingle, session: Session) = {
	def shouldBeCancelled = RandomUtils.random(0, 100) <= cancelOrderRate

	if (shouldBeCancelled && canBeGeneratedNextOrder) {
	  val orderCancel = OrderCancelRequestGenerator generate newOrderSingle
	  orderCounter.incrementAndGet
	  val delay = RandomUtils.random(minDelay, maxDelay).toLong
	  logger.info("Order with clOrdId: {} will be cancelled with clOrdId: {} in {} secs", orderCancel.getOrigClOrdID.getValue, orderCancel.getClOrdID.getValue, delay.toString)

	  import system.dispatcher
	  system.scheduler.scheduleOnce(delay millis, fixMessageSenderActor, FixMessageWithSession(orderCancel, session))
	}
  }

}
