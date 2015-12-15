package org.nexbook.tools.fixordergenerator.fix

import quickfix.{Message, Session}

/**
 * Created by milczu on 08.12.15.
 */
case class FixMessageWithSession(message: Message, session: Session)
