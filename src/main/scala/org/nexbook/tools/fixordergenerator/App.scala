package org.nexbook.tools.fixordergenerator

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import org.nexbook.tools.fixordergenerator.fix.{FixMessageToSend, FixMessageSenderActor, FixApplication}
import org.nexbook.tools.fixordergenerator.generator._
import org.nexbook.tools.fixordergenerator.repository.{PriceRepository, PricesLoader}
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import org.slf4j.LoggerFactory
import quickfix._

import scala.collection.JavaConverters._

object App {

  val LOGGER = LoggerFactory.getLogger(classOf[App])
  val config = ConfigFactory.load().getConfig("org.nexbook.generator")

  val actorSystem = ActorSystem("FixMessageSenderSystem")
  val fixMessageSenderActor = actorSystem.actorOf(Props[FixMessageSenderActor], name = "listener")

  val minDelay = config.getInt("minDelayInMillis")
  val maxDelay = config.getInt("maxDelayInMillis")
  val threadsPerFixSession = 3

  def main(args: Array[String]) = {
    val prices = new PricesLoader(SymbolGenerator.all).loadCurrentPrices
    LOGGER.info("Loaded prices: {}", prices)
    PriceRepository.updatePrices(prices)
    PriceGenerator.updatePrices(prices)
    val fixInitiator = initFixInitiator
    val fixSessions = fixInitiator.getManagedSessions.asScala
    val orderCancelExecutor = new OrderCancelExecutor(actorSystem, fixMessageSenderActor)
    val postOrderGenerators: List[PostOrderGenerator] = List(orderCancelExecutor)

    def loggedSessions = fixSessions.filter(_.isLoggedOn)

    LOGGER.info("OrderGenerator started. Order generating with delay: {}-{}", minDelay, maxDelay)


    def startWork: Unit = {
      def waitForLogon = {
        while (loggedSessions.size < fixSessions.size) {
          LOGGER.trace("Waiting for logging to FIX Session")
          Thread.sleep(1000)
        }
        LOGGER.info("Logged to fix sessions: {}", loggedSessions)
      }
      waitForLogon
      var threads: List[Thread] = List()

      def allThreadsDead = threads.filter(_.isAlive).isEmpty

      for (session <- loggedSessions) {
        for (no <- 1 to threadsPerFixSession) {
          val threadName = session.getSessionID.getSenderCompID + "_" + no
          val thread = new Thread(new AsyncOrderGeneratorSender(session, postOrderGenerators), threadName)
          thread.start
          threads = thread :: threads
        }
      }

      while (!allThreadsDead) {
        Thread.sleep(1000)
      }
      startWork
    }

    startWork
  }

  def initFixInitiator(): SocketInitiator = {
    val fixOrderHandlerSettings = new SessionSettings("config/fix_connection.config")
    val application = new FixApplication
    val fileStoreFactory = new FileStoreFactory(fixOrderHandlerSettings)
    val messageFactory = new DefaultMessageFactory
    val fileLogFactory = new FileLogFactory(fixOrderHandlerSettings)
    val socketInitiator = new SocketInitiator(application, fileStoreFactory, fixOrderHandlerSettings, fileLogFactory, messageFactory)
    socketInitiator.start
    socketInitiator
  }

  class AsyncOrderGeneratorSender(session: Session, postOrderGenerators: List[PostOrderGenerator]) extends Runnable {

    override def run(): Unit = {
      while (session.isLoggedOn) {
        val order = OrderGenerator.generate
        fixMessageSenderActor ! FixMessageToSend(order, session)
        postOrderGenerators.foreach(_.afterOrderGenerated(order, session))
        Thread.sleep(RandomUtils.random(minDelay, maxDelay))
      }
    }
  }

}


