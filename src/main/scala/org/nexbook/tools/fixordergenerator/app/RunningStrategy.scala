package org.nexbook.tools.fixordergenerator.app

import akka.actor.{ActorSystem, Props}
import org.nexbook.tools.fixordergenerator.AppConfig
import org.nexbook.tools.fixordergenerator.fix.{FixMessageSenderActor, FixMessageWithSession}
import org.nexbook.tools.fixordergenerator.generator.{OrderCancelExecutor, OrderGenerator, SymbolGenerator}
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

  def areAllSessionsLogged = loggedSessions.size == fixSessions.size

  def appConfig: AppConfig

  def waitForLogon() = {
    while (!areAllSessionsLogged) {
      logger.trace("Waiting for logging to FIX Session")
      Thread.sleep(1000)
    }
    logger.info("Logged to fix sessions: {}", loggedSessions)
  }
}

trait OrderGeneratingRunningStrategy extends RunningStrategy {
  def appConfig: AppConfig

  val orderGenerator = new OrderGenerator(new SymbolGenerator(appConfig.supportedSymbols))
  val orderCancelExecutor = actorSystem.actorOf(Props(new OrderCancelExecutor(actorSystem, fixMessageSenderActor, appConfig.generatorConfig.getConfig("cancelOrder"))), "orderCancelExecutor")
  //val postOrderGenerators: List[PostOrderGenerator] = List(orderCancelExecutor)
  var orderCounter: Int = 0

  val isLimitRestriction = appConfig.generatorConfig.getBoolean("limit.limited")
  val orderLimit = appConfig.generatorConfig.getInt("limit.maxOrderCount")

  def canBeGeneratedNextOrder = !isLimitRestriction || orderCounter < orderLimit

  def generateAndPublishOrder(session: Session) = {
    val order = orderGenerator.generate()
    orderCounter = orderCounter + 1
    val msg = FixMessageWithSession(order, session)
    fixMessageSenderActor ! msg
    //postOrderGenerators.foreach(_.afterOrderGenerated(order, session))
    orderCancelExecutor ! msg
  }

  def waitForSendMessagesOverFix = Thread.sleep(1200000)
}

class NewOrderGeneratingThreadBasedStrategy(val fixSessions: List[Session], val appConfig: AppConfig) extends OrderGeneratingRunningStrategy {

  val logger = LoggerFactory.getLogger(classOf[NewOrderGeneratingThreadBasedStrategy])
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
        val thread = new Thread(new AsyncOrderGeneratorSender(session), threadName)
        thread.start()
        threads = thread :: threads
      }
    }

    while (!allThreadsDead) {
      Thread.sleep(10000)
    }
    if (canBeGeneratedNextOrder) {
      startWork()
    } else {
      logger.debug("Waiting for send messages over FIX")
      waitForSendMessagesOverFix
      logger.debug("Waiting for send messages over FIX - FINISHED")
    }
  }

  class AsyncOrderGeneratorSender(session: Session) extends Runnable {

    override def run(): Unit = {
      while (canBeGeneratedNextOrder) {
        generateAndPublishOrder(session)
        if (delay) {
          Thread.sleep(RandomUtils.random(minDelay, maxDelay))
        }
      }
    }
  }

}

class NewOrderGeneratingSingleThreadStrategy(val fixSessions: List[Session], val appConfig: AppConfig) extends OrderGeneratingRunningStrategy {
  override val logger: Logger = LoggerFactory.getLogger(classOf[NewOrderGeneratingSingleThreadStrategy])

  override def startWork(): Unit = {
    waitForLogon()

    while (canBeGeneratedNextOrder) {
      for (session <- loggedSessions) {
        if (canBeGeneratedNextOrder) {
          generateAndPublishOrder(session)
        }
      }
    }
    if (canBeGeneratedNextOrder) {
      startWork()
    } else {
      logger.debug("Waiting for send messages over FIX")
      waitForSendMessagesOverFix
      logger.debug("Waiting for send messages over FIX - FINISHED")
    }
  }
}
