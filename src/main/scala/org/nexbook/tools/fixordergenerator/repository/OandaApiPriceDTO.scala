package org.nexbook.tools.fixordergenerator.repository

import org.joda.time.DateTime

/**
 * Created by milczu on 04.01.15.
 */
case class OandaApiPriceDTO(instrument: String, time: DateTime, bid: Double, ask: Double)