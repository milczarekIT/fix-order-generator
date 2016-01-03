package org.nexbook.tools.fixordergenerator.app

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.Config
import org.joda.time.{DateTime, DateTimeZone}
import org.nexbook.tools.fixordergenerator.fix.FixMessageSender.FixMessageWithSession
import org.nexbook.tools.fixordergenerator.fix.{FixConnector, FixMessageRouter}
import org.nexbook.tools.fixordergenerator.generator.{OrderCancelExecutor, OrderGenerator, PriceGenerator, SymbolGenerator}
import org.nexbook.tools.fixordergenerator.repository.{PriceRepository, PricesLoader}
import org.slf4j.{Logger, LoggerFactory}
import quickfix.field.{MsgType, TransactTime}
import quickfix.fix44.{NewOrderSingle, OrderCancelRequest}
import quickfix.{DataDictionary, Message, Session, SessionID}

import scala.io.Source

/**
  * Created by milczu on 13.12.15
  */
trait RunningStrategy {

  val actorSystem = ActorSystem("FixMessageSenderSystem")
  val fixMessageSenderActor = actorSystem.actorOf(Props[FixMessageRouter](new FixMessageRouter(fixSessions) with FixConnector))

  def startWork(): Unit

  def logger: Logger

  def fixSessions: List[Session]

  def appConfig: AppConfig
}

trait OrderCountingGenerator {
  val isLimitRestriction = generatorConfig.getBoolean("limit.limited")
  val orderLimit = generatorConfig.getInt("limit.maxOrderCount")

  def generatorConfig: Config

  def orderCounter: AtomicLong

  def canBeGeneratedNextOrder = !isLimitRestriction || orderCounter.get < orderLimit
}

trait OrderGeneratingRunningStrategy extends RunningStrategy with OrderCountingGenerator {

  val priceGenerator = new PriceGenerator(new PriceRepository(new PricesLoader(appConfig.supportedSymbols).loadCurrentPrices))

  val orderGenerator = new OrderGenerator(new SymbolGenerator(appConfig.supportedSymbols), priceGenerator)
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

class AkkaNewOrderGeneratingStrategy(val fixSessions: List[Session], val appConfig: AppConfig) extends OrderGeneratingRunningStrategy {
  this: FixConnector =>

  override val logger: Logger = LoggerFactory.getLogger(classOf[AkkaNewOrderGeneratingStrategy])

  def generatorConfig = appConfig.generatorConfig

  override def startWork() = {
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
  this: FixConnector =>

  override val logger: Logger = LoggerFactory.getLogger(classOf[FileBasedPublisherStrategy])

  val fileName = appConfig.fileBasedStrategyConfig.getString("msgFileName")
  val dataDictionary = new DataDictionary("config/FIX44.xml")

  override def startWork() = {
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

	def resolveSession(message: Message): Session = {
	  def sessionID: SessionID = {
		val header = message.getHeader
		val beginString = header.getString(8)
		val senderCompID = header.getString(49)
		val targetCompID = header.getString(56)
		val qualifier = ""
		new SessionID(beginString, senderCompID, targetCompID, qualifier)
	  }
	  Session.lookupSession(sessionID)
	}

	waitForLogon()
	logger.info("All sessions logged. Reading FIX msgs from file")
	val lines: List[String] = Source.fromFile(fileName).getLines.toList
	logger.info(s"All sessions logged. Readed msgs: ${lines.size}")

	val fixMsgs: List[FixMessageWithSession] = lines.map(toFixMessage).map(fixMsgToSpecializedMsg).map(withUpdatedFields).map(msg => FixMessageWithSession(msg, resolveSession(msg)))
	for (fixMsg <- fixMsgs) {
	  fixMessageSenderActor ! fixMsg
	}

	waitForSendMessagesOverFix()
	actorSystem.shutdown()
  }
}