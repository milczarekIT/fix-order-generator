package org.nexbook.tools.fixordergenerator.fix

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.Router
import org.slf4j.LoggerFactory
import quickfix.{Session, SessionID}

/**
  * Created by milczu on 1/1/16.
  */
class FixMessageRouter(val fixSessions: List[Session]) extends Actor {
  this: FixConnector =>

  val logger = LoggerFactory.getLogger(classOf[FixMessageRouter])

  val router = {
	if (!areAllSessionsLogged) {
	  waitForLogon()
	}
	val senders: Map[SessionID, ActorRef] = loggedSessions.map(s => s.getSessionID -> context.actorOf(Props[FixMessageSender], "Sender_" + s.getSessionID.getSenderCompID)).toMap
	new Router(new FixMessageRoutingLogic(senders))
  }

  override def receive: Receive = {
	case msg => router.route(msg, sender)
  }
}
