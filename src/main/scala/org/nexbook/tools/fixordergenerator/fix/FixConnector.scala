package org.nexbook.tools.fixordergenerator.fix

import org.slf4j.Logger
import quickfix.Session

/**
  * Created by milczu on 1/1/16.
  */
trait FixConnector {

  val logger: Logger
  val fixSessions: List[Session]

  def waitForLogon() = {
	while (!areAllSessionsLogged) {
	  logger.trace("Waiting for logging to FIX Session")
	  Thread.sleep(1000)
	}
	logger.info(s"Logged to fix sessions: $loggedSessions")
  }

  def areAllSessionsLogged = loggedSessions.size == fixSessions.size

  def loggedSessions = fixSessions.filter(_.isLoggedOn)

  def waitForSendMessagesOverFix() = Thread.sleep(12000000)
}
