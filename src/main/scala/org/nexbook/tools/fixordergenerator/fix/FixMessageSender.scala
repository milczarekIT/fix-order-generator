package org.nexbook.tools.fixordergenerator.fix

import akka.actor.Actor
import org.nexbook.tools.fixordergenerator.fix.FixMessageSender._
import org.slf4j.LoggerFactory
import quickfix.{Message, Session}

/**
  * Created by milczu on 08.12.15.
  */
class FixMessageSender extends Actor {

  val logger = LoggerFactory.getLogger(classOf[FixMessageSender])
  val messagesLogger = LoggerFactory.getLogger("MESSAGES")

  override def receive = {
	case fm: FixMessageWithSession =>
	  logger.debug(s"Sending message: ${fm.message}")
	  fm.session.send(fm.message)
	  messagesLogger.debug(fm.message.toString)
	case m: Message =>
	  logger.debug(s"Sending message: $m")
	  Session.sendToTarget(m)
	  messagesLogger.debug(m.toString)
  }
}

object FixMessageSender {

  case class FixMessageWithSession(message: Message, session: Session)

}



