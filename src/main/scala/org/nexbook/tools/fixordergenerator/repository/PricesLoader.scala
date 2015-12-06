package org.nexbook.tools.fixordergenerator.repository

import com.typesafe.config.ConfigFactory
import dispatch.Defaults._
import dispatch._
import net.liftweb.json.JsonAST.{JNull, JString}
import net.liftweb.json._
import org.joda.time.DateTime

/**
 * Created by milczu on 04.01.15.
 */
class PricesLoader(symbols: List[String]) {

  implicit val formats = Serialization.formats(NoTypeHints) ++ List(DateTimeSerializer)
  val config = ConfigFactory.load()
  val OANDA_API_URL = "https://api-fxpractice.oanda.com/v1/"
  val pricesUrl = OANDA_API_URL + "/prices?instruments=" + toOandaUrlEncodedSymbolsFormat

  def loadCurrentPrices: Map[String, (Double, Double)] = {
    val strResponse = Http(request OK as.String).apply
    val prices = JsonParser.parse(strResponse) \\ "prices"
    toMap(prices.extract[List[OandaApiPriceDTO]])
  }

  def request = url(pricesUrl).addHeader("Authorization", "Bearer " + config.getString("org.nexbook.oandaApiKey"))

  private def toMap(dtos: List[OandaApiPriceDTO]): Map[String, (Double, Double)] = dtos.map(dto => (dto.instrument.replace("_", "/") ->(dto.bid, dto.ask))).toMap

  private def toOandaUrlEncodedSymbolsFormat: String = symbols.map(_.replace("/", "_")).toList.mkString("%2C")

  object DateParser {
    def parse(s: String, format: Formats) =
      format.dateFormat.parse(s).map(_.getTime).getOrElse(throw new MappingException("Invalid date format " + s))
  }

  case object DateTimeSerializer extends CustomSerializer[DateTime](format => ( {
    case JString(s) => new DateTime(DateParser.parse(s, format))
    case JNull => null
  }, {
    case d: DateTime => JString(format.dateFormat.format(d.toDate))
  }
    ))

}


