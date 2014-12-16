package org.nexbook.tools.fixordergenerator

import org.nexbook.tools.fixordergenerator.fix.FixApplication
import org.nexbook.tools.fixordergenerator.generator.OrderGenerator
import org.slf4j.LoggerFactory
import quickfix._

object App {

  val LOGGER = LoggerFactory.getLogger(classOf[App])

  def main(args: Array[String]) = {
    val delay = 2000;

    LOGGER.info("OrderGenerator started. Order generating with delay: {}", delay)
    val fixSession = initFixInitiator.getManagedSessions.iterator.next
    waitForLogon(fixSession)

    while (true) {
      Thread.sleep(delay)
      val order = OrderGenerator.next
      val result = fixSession.send(order)
      LOGGER.debug("Order send: " + result + " ClOrdID: " + order.getClOrdID.getValue + ", symbol: " + order.getSymbol.getValue + ", orderQty: " + order.getOrderQty.getValue + ", ordType: " + order.getOrdType.getValue + ", account: " + order.getAccount.getValue)
    }
  }

  def initFixInitiator(): SocketInitiator = {
    val fixOrderHandlerSettings = new SessionSettings("config/fix_connection.config")
    val application = new FixApplication
    val fileStoreFactory = new FileStoreFactory(fixOrderHandlerSettings)
    val messageFactory = new DefaultMessageFactory
    val fileLogFactory = new FileLogFactory(fixOrderHandlerSettings)
    val socketInitiator = new SocketInitiator(application, fileStoreFactory, fixOrderHandlerSettings, fileLogFactory, messageFactory)
    socketInitiator.start
    return socketInitiator
  }

  def waitForLogon(session: Session) = {
    while (!session.isLoggedOn) {
      LOGGER.debug("Waiting for logging to FIX Session")
      Thread.sleep(100)
    }
    LOGGER.info("Logged to fix session: {}", session.getSessionID)
  }
}
