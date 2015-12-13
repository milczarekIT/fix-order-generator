package org.nexbook.tools.fixordergenerator.app

import akka.actor.{ActorSystem, Props}
import org.nexbook.tools.fixordergenerator.AppConfig
import org.nexbook.tools.fixordergenerator.fix.{FixMessageSenderActor, FixMessageToSend}
import org.nexbook.tools.fixordergenerator.generator.{OrderCancelExecutor, OrderGenerator, PostOrderGenerator, SymbolGenerator}
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import org.slf4j.{Logger, LoggerFactory}
import quickfix.Session

/**
 * Created by milczu on 13.12.15
 */
trait RunningStrategy {

  def startWork(): Unit

  val actorSystem = ActorSystem("FixMessageSenderSystem")
  val fixMessageSenderActor = actorSystem.actorOf(Props[FixMessageSenderActor], name = "listener")

  def logger: Logger

  def fixSessions: List[Session]

  def loggedSessions = fixSessions.filter(_.isLoggedOn)

  def waitForLogon() = {
    while (loggedSessions.size < fixSessions.size) {
      logger.trace("Waiting for logging to FIX Session")
      Thread.sleep(1000)
    }
    logger.info("Logged to fix sessions: {}", loggedSessions)
  }
}

class NewOrderGeneratingStrategy(val fixSessions: List[Session], appConfig: AppConfig) extends RunningStrategy {

  val logger = LoggerFactory.getLogger(classOf[NewOrderGeneratingStrategy])
  val orderCancelExecutor = new OrderCancelExecutor(actorSystem, fixMessageSenderActor, appConfig.generatorConfig.getConfig("cancelOrder"))
  val postOrderGenerators: List[PostOrderGenerator] = List(orderCancelExecutor)
  val orderGenerator = new OrderGenerator(new SymbolGenerator(appConfig.supportedSymbols))
  val threadsPerFixSession = 5

  val minDelay = appConfig.generatorConfig.getInt("minDelayInMillis")
  val maxDelay = appConfig.generatorConfig.getInt("maxDelayInMillis")
  val delay = appConfig.generatorConfig.getBoolean("delay")

  def startWork(): Unit = {
    waitForLogon()
    var threads: List[Thread] = List()

    def allThreadsDead = threads.filter(_.isAlive).isEmpty

    for (session <- loggedSessions) {
      for (no <- 1 to threadsPerFixSession) {
        val threadName = session.getSessionID.getSenderCompID + "_" + no
        val thread = new Thread(new AsyncOrderGeneratorSender(session, postOrderGenerators), threadName)
        thread.start()
        threads = thread :: threads
      }
    }

    while (!allThreadsDead) {
      Thread.sleep(10000)
    }
    startWork()
  }

  class AsyncOrderGeneratorSender(session: Session, postOrderGenerators: List[PostOrderGenerator]) extends Runnable {

    override def run(): Unit = {
      while (true) {
        val order = orderGenerator.generate()
        fixMessageSenderActor ! FixMessageToSend(order, session)
        postOrderGenerators.foreach(_.afterOrderGenerated(order, session))
        if (delay) {
          Thread.sleep(RandomUtils.random(minDelay, maxDelay))
        }
      }
    }
  }

}
