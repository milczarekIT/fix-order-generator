package org.nexbook.tools.fixordergenerator.fix

import akka.actor.Actor
import org.slf4j.LoggerFactory
import quickfix.{Message, Session}

/**
  * Created by milczu on 08.12.15.
  */
class FixMessageSenderActor extends Actor {

  val logger = LoggerFactory.getLogger(classOf[FixMessageSenderActor])
  val messagesLogger = LoggerFactory.getLogger("MESSAGES")

  override def receive = {
	case p: FixMessageWithSession =>
	  logger.debug(s"Sending message: ${p.message}")
	  p.session.send(p.message)
	  messagesLogger.debug(p.message.toString)
	case m: Message =>
	  logger.debug(s"Sending message: $m")
	  Session.sendToTarget(m)
  }
}
