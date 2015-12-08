package org.nexbook.tools.fixordergenerator.fix

import akka.actor.Actor
import org.slf4j.LoggerFactory
import quickfix.Session

/**
 * Created by milczu on 08.12.15.
 */
class FixMessageSenderActor extends Actor {

  val logger = LoggerFactory.getLogger(classOf[FixMessageSenderActor])

  override def receive = {
    case p: FixMessageToSend =>
      if (p.session.isLoggedOn) {
        logger.info("Sending message: {}", p.message)
        Session.sendToTarget(p.message, p.session.getSessionID)
      } else {
        logger.info("Trying to send message: {}, but session {} is logged out", p.message.asInstanceOf[Any], p.session.asInstanceOf[Any])
      }
  }
}
