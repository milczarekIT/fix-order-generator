package org.nexbook.tools.fixordergenerator

import com.typesafe.config.ConfigFactory
import org.nexbook.tools.fixordergenerator.app.{NewOrderGeneratingStrategy, RunningStrategy}
import org.nexbook.tools.fixordergenerator.fix.FixApplication
import org.nexbook.tools.fixordergenerator.generator._
import org.nexbook.tools.fixordergenerator.repository.{PriceRepository, PricesLoader}
import org.slf4j.LoggerFactory
import quickfix._

import scala.collection.JavaConverters._

object App {

  val logger = LoggerFactory.getLogger(classOf[App])

  def main(args: Array[String]) = {
    val configPath = if (args.length == 0) "config/application.conf" else args(0)
    logger.info("Starting with config: {}", configPath)
    val appConfig = new AppConfig(ConfigFactory.load(configPath).getConfig("org.nexbook"))

    val prices = new PricesLoader(appConfig.supportedSymbols).loadCurrentPrices
    logger.info("Loaded prices: {}", prices)
    PriceRepository.updatePrices(prices)
    PriceGenerator.updatePrices(prices)

    def initFixInitiator(): SocketInitiator = {
      val fixOrderHandlerSettings = new SessionSettings(appConfig.fixConfig.getString("config.path"))
      val application = new FixApplication
      val fileStoreFactory = new FileStoreFactory(fixOrderHandlerSettings)
      val messageFactory = new DefaultMessageFactory
      val fileLogFactory = new FileLogFactory(fixOrderHandlerSettings)
      val socketInitiator = new SocketInitiator(application, fileStoreFactory, fixOrderHandlerSettings, fileLogFactory, messageFactory)
      socketInitiator.start()
      socketInitiator
    }

    val fixInitiator = initFixInitiator()
    val fixSessions = fixInitiator.getManagedSessions.asScala.toList

    def runningStrategy: RunningStrategy = new NewOrderGeneratingStrategy(fixSessions, appConfig)

    runningStrategy.startWork()


  }

}


