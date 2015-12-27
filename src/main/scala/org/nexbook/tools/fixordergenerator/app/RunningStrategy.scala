package org.nexbook.tools.fixordergenerator.app

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorSystem, Props}
import akka.routing.RoundRobinRouter
import com.typesafe.config.Config
import org.joda.time.{DateTime, DateTimeZone}
import org.nexbook.tools.fixordergenerator.fix.{FixMessageSenderActor, FixMessageWithSession}
import org.nexbook.tools.fixordergenerator.generator.{OrderCancelExecutor, OrderGenerator, SymbolGenerator}
import org.slf4j.{Logger, LoggerFactory}
import quickfix.field.{MsgType, TransactTime}
import quickfix.fix44.{NewOrderSingle, OrderCancelRequest}
import quickfix.{DataDictionary, Message, Session}

import scala.io.Source

/**
  * Created by milczu on 13.12.15
  */
trait RunningStrategy {

  val actorSystem = ActorSystem("FixMessageSenderSystem")
  val fixMessageSenderActor = actorSystem.actorOf(Props[FixMessageSenderActor].withRouter(RoundRobinRouter(8)), name = "listener")

  def startWork(): Unit

  def logger: Logger

  def fixSessions: List[Session]

  def appConfig: AppConfig

  def waitForLogon() = {
	while (!areAllSessionsLogged) {
	  logger.trace("Waiting for logging to FIX Session")
	  Thread.sleep(1000)
	}
	logger.info(s"Logged to fix sessions: $loggedSessions")
  }

  def areAllSessionsLogged = loggedSessions.size == fixSessions.size

  def loggedSessions = fixSessions.filter(_.isLoggedOn)

  def waitForSendMessagesOverFix() = Thread.sleep(1200000)
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
	  waitForSendMessagesOverFix()
	  logger.debug("Waiting for send messages over FIX - FINISHED")
	}
  }
}

class FileBasedPublisherStrategy(val fixSessions: List[Session], val appConfig: AppConfig) extends RunningStrategy {

  override val logger: Logger = LoggerFactory.getLogger(classOf[FileBasedPublisherStrategy])

  val fileName = appConfig.fileBasedStrategyConfig.getString("msgFileName")
  val dataDictionary = new DataDictionary("config/FIX44.xml")

  override def startWork(): Unit = {
	def toFixMessage(line: String): Message = new Message(line, dataDictionary, false)

	def fixMsgToSpecializedMsg(msg: Message): Message = {
	  msg.getHeader.getField(new MsgType()).getValue match {
		case NewOrderSingle.MSGTYPE =>
		  val newOrderSingle = new NewOrderSingle
		  newOrderSingle.fromString(msg.toString, dataDictionary, false)
		  newOrderSingle
		case OrderCancelRequest.MSGTYPE =>
		  val orderCancelRequest = new OrderCancelRequest
		  orderCancelRequest.fromString(msg.toString, dataDictionary, false)
		  orderCancelRequest
		case _ => msg
	  }
	}

	def withUpdatedFields(msg: Message): Message = msg.getHeader.getField(new MsgType()).getValue match {
	  case NewOrderSingle.MSGTYPE =>
		val newOrderSingle: NewOrderSingle = msg.asInstanceOf[NewOrderSingle]
		newOrderSingle.set(new TransactTime(DateTime.now(DateTimeZone.UTC).toDate))
		newOrderSingle
	  case OrderCancelRequest.MSGTYPE =>
		val orderCancelRequest: OrderCancelRequest = msg.asInstanceOf[OrderCancelRequest]
		orderCancelRequest.set(new TransactTime(DateTime.now(DateTimeZone.UTC).toDate))
		orderCancelRequest
	}

	waitForLogon()
	logger.info("All sessions logged. Reading FIX msgs from file")
	val lines: List[String] = Source.fromFile(fileName).getLines.toList
	logger.info(s"All sessions logged. Readed msgs: ${lines.size}")

	val fixMsgs: List[Message] = lines.map(toFixMessage).map(fixMsgToSpecializedMsg).map(withUpdatedFields)
	for (fixMsg <- fixMsgs) {
	  fixMessageSenderActor ! fixMsg
	}
	waitForSendMessagesOverFix()
  }
}