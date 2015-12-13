package org.nexbook.tools.fixordergenerator.fix

import akka.actor.Actor
import org.slf4j.LoggerFactory
import quickfix.{Message, Session}

/**
 * Created by milczu on 08.12.15.
 */
class FixMessageSenderActor extends Actor {

  val logger = LoggerFactory.getLogger(classOf[FixMessageSenderActor])

  override def receive = {
    case p: FixMessageToSend =>
        logger.debug("Sending message: {}", p.message)
        Session.sendToTarget(p.message, p.session.getSessionID)
    case m: Message =>
      logger.debug("Sending message: {}", m)
      Session.sendToTarget(m)
  }
}
