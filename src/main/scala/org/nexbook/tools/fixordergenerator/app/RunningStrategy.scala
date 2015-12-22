package org.nexbook.tools.fixordergenerator.app

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorSystem, Props}
import akka.routing.RoundRobinRouter
import com.typesafe.config.Config
import org.nexbook.tools.fixordergenerator.fix.{FixMessageSenderActor, FixMessageWithSession}
import org.nexbook.tools.fixordergenerator.generator.{OrderCancelExecutor, OrderGenerator, SymbolGenerator}
import org.nexbook.tools.fixordergenerator.utils.RandomUtils
import org.slf4j.{Logger, LoggerFactory}
import quickfix.Session

/**
  * Created by milczu on 13.12.15
  */
trait RunningStrategy {

  val actorSystem = ActorSystem("FixMessageSenderSystem")
  val fixMessageSenderActor = actorSystem.actorOf(Props[FixMessageSenderActor].withRouter(RoundRobinRouter(8)), name = "listener")

  def startWork(): Unit

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
	logger.info(s"Logged to fix sessions: $loggedSessions")
  }
}

trait OrderCountingGenerator {
  val isLimitRestriction = generatorConfig.getBoolean("limit.limited")
  val orderLimit = generatorConfig.getInt("limit.maxOrderCount")

  def generatorConfig: Config

  def orderCounter: AtomicLong

  def canBeGeneratedNextOrder = !isLimitRestriction || orderCounter.get < orderLimit
}

trait OrderGeneratingRunningStrategy extends RunningStrategy with OrderCountingGenerator {
  val orderGenerator = new OrderGenerator(new SymbolGenerator(appConfig.supportedSymbols))
  val orderCounter: AtomicLong = new AtomicLong
  val orderCancelExecutor = actorSystem.actorOf(Props(new OrderCancelExecutor(actorSystem, fixMessageSenderActor, appConfig.generatorConfig, orderCounter)), "orderCancelExecutor")

  def appConfig: AppConfig

  def generateAndPublishOrder(session: Session) = {
	val order = orderGenerator.generate()
	orderCounter.incrementAndGet
	val msg = FixMessageWithSession(order, session)
	fixMessageSenderActor ! msg
	orderCancelExecutor ! msg
  }

  def waitForSendMessagesOverFix = Thread.sleep(1200000)
}

class NewOrderGeneratingThreadBasedStrategy(val fixSessions: List[Session], val appConfig: AppConfig) extends OrderGeneratingRunningStrategy {

  val logger = LoggerFactory.getLogger(classOf[NewOrderGeneratingThreadBasedStrategy])
  val threadsPerFixSession = 5

  val generatorConfig = appConfig.generatorConfig
  val minDelay = generatorConfig.getInt("minDelayInMillis")
  val maxDelay = generatorConfig.getInt("maxDelayInMillis")
  val delay = generatorConfig.getBoolean("delay")

  def startWork(): Unit = {
	waitForLogon()
	var threads: List[Thread] = List()

	def allThreadsDead = !threads.exists(_.isAlive)

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

  def generatorConfig = appConfig.generatorConfig

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
