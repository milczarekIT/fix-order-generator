package org.nexbook.tools.fixordergenerator.fix

import akka.actor.ActorRef
import akka.routing.{ActorRefRoutee, Routee, RoutingLogic, SeveralRoutees}
import org.nexbook.tools.fixordergenerator.fix.FixMessageSender.FixMessageWithSession
import quickfix.SessionID

import scala.collection.immutable.IndexedSeq

/**
  * Created by milczu on 1/1/16.
  */
class FixMessageRoutingLogic(senders: Map[SessionID, ActorRef]) extends RoutingLogic {
  val senderRoutees = senders.map(e => e._1 -> new ActorRefRoutee(e._2))

  override def select(message: Any, routees: IndexedSeq[Routee]): Routee = message match {
	case m: FixMessageWithSession => senderRoutees(m.session.getSessionID)
	case _ => new SeveralRoutees(senderRoutees.values.toIndexedSeq)
  }
}
