package org.nexbook.tools.fixordergenerator.fix

import quickfix.{Session, Message}

/**
 * Created by milczu on 08.12.15.
 */
case class FixMessageToSend(message: Message, session: Session)
