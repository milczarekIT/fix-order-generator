package org.nexbook.tools.fixordergenerator.fix

import org.slf4j.{Logger, LoggerFactory}
import quickfix._
import quickfix.field.{BeginString, Password, ResetSeqNumFlag}
import quickfix.fix44.Logon


/**
 * Created by milczu on 15.12.14.
 */
class FixApplication extends Application {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[FixApplication])

  var sessionId: SessionID = null


  override def onCreate(sessionId: SessionID) {
    LOGGER.info("FixApplication Session Created with SessionID = " + sessionId)
    this.sessionId = sessionId
    sendLogonRequest(sessionId)
  }

  def sendLogonRequest(sessionId: SessionID) = {
    val logon = new Logon
    val header = logon.getHeader();
    header.setField(new BeginString("FIX.4.4"))
    logon.set(new ResetSeqNumFlag(true))
    logon.set(new Password("n/a"))
    Session.sendToTarget(logon, sessionId)
    LOGGER.info("Logon Message Sent")
  }


  override def onLogon(sessionId: SessionID) {
    LOGGER.info("Logon: {}", sessionId)
  }

  override def onLogout(sessionId: SessionID) {
    LOGGER.info("Logout: {}", sessionId)
  }

  override def toAdmin(message: Message, sessionId: SessionID) {
    LOGGER.debug("ToAdmin: {}", message)
  }

  @throws(classOf[RejectLogon])
  @throws(classOf[IncorrectTagValue])
  @throws(classOf[IncorrectDataFormat])
  @throws(classOf[FieldNotFound])
  override def fromAdmin(message: Message, sessionId: SessionID) {
    LOGGER.debug("FromAdmin: {}", message)
  }

  @throws(classOf[DoNotSend])
  override def toApp(message: Message, sessionId: SessionID) {
    LOGGER.debug("ToApp: {}", message)
  }

  override def fromApp(message: Message, sessionId: SessionID) {
    LOGGER.info("FromApp: {}", message)

  }
}
