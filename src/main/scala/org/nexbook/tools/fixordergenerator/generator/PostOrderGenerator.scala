package org.nexbook.tools.fixordergenerator.generator

import quickfix.Session
import quickfix.fix44.NewOrderSingle

/**
 * Created by milczu on 08.12.15.
 */
trait PostOrderGenerator {

  def afterOrderGenerated(order: NewOrderSingle, session: Session)
}
