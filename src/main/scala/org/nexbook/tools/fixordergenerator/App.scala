package org.nexbook.tools.fixordergenerator

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.nexbook.tools.fixordergenerator.fix.{FixApplication, FixMessageSenderActor, FixMessageToSend}
import org.nexbook.tools.fixordergenerator.generator._
import org.nexbook.tools.fixordergenerator.repository.{PriceRepository, PricesLoader}
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import org.slf4j.LoggerFactory
import quickfix._

import scala.collection.JavaConverters._

object App {

  val logger = LoggerFactory.getLogger(classOf[App])

  val actorSystem = ActorSystem("FixMessageSenderSystem")
  val fixMessageSenderActor = actorSystem.actorOf(Props[FixMessageSenderActor], name = "listener")

  def main(args: Array[String]) = {
    val configPath = if (args.length == 0) "config/application.conf" else args(0)
    logger.info("Starting with config: {}", configPath)
    val appConfig = new AppConfig(ConfigFactory.load(configPath).getConfig("org.nexbook"))

    val minDelay = appConfig.generatorConfig.getInt("minDelayInMillis")
    val maxDelay = appConfig.generatorConfig.getInt("maxDelayInMillis")
    val delay = appConfig.generatorConfig.getBoolean("delay")
    val threadsPerFixSession = 4

    val symbolGenerator = new SymbolGenerator(appConfig.supportedSymbols)
    val orderGenerator = new OrderGenerator(symbolGenerator)

    val prices = new PricesLoader(appConfig.supportedSymbols).loadCurrentPrices
    logger.info("Loaded prices: {}", prices)
    PriceRepository.updatePrices(prices)
    PriceGenerator.updatePrices(prices)

    def initFixInitiator(): SocketInitiator = {
      val fixOrderHandlerSettings = new SessionSettings(appConfig.fixConfig.getString("config.path"))
      val application = new FixApplication
      val fileStoreFactory = new FileStoreFactory(fixOrderHandlerSettings)
      val messageFactory = new DefaultMessageFactory
      val fileLogFactory = new FileLogFactory(fixOrderHandlerSettings)
      val socketInitiator = new SocketInitiator(application, fileStoreFactory, fixOrderHandlerSettings, fileLogFactory, messageFactory)
      socketInitiator.start
      socketInitiator
    }

    val fixInitiator = initFixInitiator
    val fixSessions = fixInitiator.getManagedSessions.asScala
    val orderCancelExecutor = new OrderCancelExecutor(actorSystem, fixMessageSenderActor, appConfig.generatorConfig.getConfig("cancelOrder"))
    val postOrderGenerators: List[PostOrderGenerator] = List(orderCancelExecutor)

    def loggedSessions = fixSessions.filter(_.isLoggedOn)

    logger.info("OrderGenerator started. Order generating with delay: {}-{}", minDelay, maxDelay)

    def startWork: Unit = {
      def waitForLogon = {
        while (loggedSessions.size < fixSessions.size) {
          logger.trace("Waiting for logging to FIX Session")
          Thread.sleep(1000)
        }
        logger.info("Logged to fix sessions: {}", loggedSessions)
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
        Thread.sleep(10000)
      }
      startWork
    }

    startWork

    class AsyncOrderGeneratorSender(session: Session, postOrderGenerators: List[PostOrderGenerator]) extends Runnable {

      override def run(): Unit = {
        while (session.isLoggedOn) {
          val order = orderGenerator.generate
          fixMessageSenderActor ! FixMessageToSend(order, session)
          postOrderGenerators.foreach(_.afterOrderGenerated(order, session))
          if (delay) {
            Thread.sleep(RandomUtils.random(minDelay, maxDelay))
          } else {
            //Thread.sleep(1)
          }
        }
      }
    }
  }

}


