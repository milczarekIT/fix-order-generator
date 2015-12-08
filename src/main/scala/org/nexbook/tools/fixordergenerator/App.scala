package org.nexbook.tools.fixordergenerator

import org.nexbook.tools.fixordergenerator.fix.FixApplication
import org.nexbook.tools.fixordergenerator.generator.{OrderGenerator, SymbolGenerator}
import org.nexbook.tools.fixordergenerator.repository.{PriceRepository, PricesLoader}
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import org.slf4j.LoggerFactory
import quickfix._

import scala.collection.JavaConverters._

object App {

  val LOGGER = LoggerFactory.getLogger(classOf[App])

  val minDelay = 300
  val maxDelay = 5000
  val threadsPerFixSession = 3

  def main(args: Array[String]) = {
    val prices = new PricesLoader(SymbolGenerator.all).loadCurrentPrices
    LOGGER.info("Loaded prices: {}", prices)
    PriceRepository.updatePrices(prices)
    val fixInitiator = initFixInitiator
    val fixSessions = fixInitiator.getManagedSessions.asScala

    def loggedSessions = fixSessions.filter(_.isLoggedOn)

    LOGGER.info("OrderGenerator started. Order generating with delay: {}-{}", minDelay, maxDelay)


    def startWork: Unit = {
      def waitForLogon = {
        while (loggedSessions.size < fixSessions.size) {
          LOGGER.debug("Waiting for logging to FIX Session")
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
          val thread = new Thread(new AsyncOrderGeneratorSender(session), threadName)
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

  class AsyncOrderGeneratorSender(session: Session) extends Runnable {
    override def run(): Unit = {
      while (session.isLoggedOn) {
        val order = OrderGenerator.next
        val result = session.send(order)
        LOGGER.debug("Order send: " + result + " ClOrdID: " + order.getClOrdID.getValue + ", symbol: " + order.getSymbol.getValue + ", orderQty: " + order.getOrderQty.getValue + ", ordType: " + order.getOrdType.getValue + ", account: " + order.getAccount.getValue)
        Thread.sleep(RandomUtils.random(minDelay, maxDelay))
      }
    }
  }

}


