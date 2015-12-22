package org.nexbook.tools.fixordergenerator.fix

import org.slf4j.{Logger, LoggerFactory}
import quickfix._


/**
  * Created by milczu on 15.12.14
  */
class FixApplication extends Application {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[FixApplication])

  override def onCreate(sessionId: SessionID) {
	LOGGER.info(s"FixApplication Session Created with SessionID = $sessionId")
  }

  override def onLogon(sessionId: SessionID) {
	LOGGER.info(s"Logon: $sessionId")
  }

  override def onLogout(sessionId: SessionID) {
	LOGGER.info(s"Logout: $sessionId")
  }

  override def toAdmin(message: Message, sessionId: SessionID) {
	LOGGER.debug(s"ToAdmin: $message")
  }

  @throws(classOf[RejectLogon])
  @throws(classOf[IncorrectTagValue])
  @throws(classOf[IncorrectDataFormat])
  @throws(classOf[FieldNotFound])
  override def fromAdmin(message: Message, sessionId: SessionID) {
	LOGGER.debug(s"FromAdmin: $message")
  }

  @throws(classOf[DoNotSend])
  override def toApp(message: Message, sessionId: SessionID) {
	LOGGER.trace(s"ToApp: $message")
  }

  override def fromApp(message: Message, sessionId: SessionID) {
	LOGGER.trace(s"FromApp: $message")
  }
}
