package org.nexbook.tools.fixordergenerator.app

import com.typesafe.config.ConfigFactory
import org.nexbook.tools.fixordergenerator.fix.FixApplication
import org.nexbook.tools.fixordergenerator.generator._
import org.nexbook.tools.fixordergenerator.repository.{PriceRepository, PricesLoader}
import org.slf4j.LoggerFactory
import quickfix._

import scala.collection.JavaConverters._

object App {

  val logger = LoggerFactory.getLogger(classOf[App])

  def main(args: Array[String]) = {
	val configName = resolveConfigName

	val configPath = s"config/$configName.conf"
	logger.info(s"Starting with config: $configPath")
	val appConfig = new AppConfig(ConfigFactory.load(configPath).getConfig("org.nexbook"))

	val prices = new PricesLoader(appConfig.supportedSymbols).loadCurrentPrices
	logger.info(s"Loaded prices: $prices")
	PriceRepository.updatePrices(prices)
	PriceGenerator.updatePrices(prices)

	def initFixInitiator(): SocketInitiator = {
	  val fixOrderHandlerSettings = new SessionSettings(appConfig.fixConfig.getString("config.path"))
	  val application = new FixApplication
	  val storeFactory = new MemoryStoreFactory
	  val messageFactory = new DefaultMessageFactory
	  val fileLogFactory = new FileLogFactory(fixOrderHandlerSettings)
	  val socketInitiator = new SocketInitiator(application, storeFactory, fixOrderHandlerSettings, fileLogFactory, messageFactory)
	  socketInitiator.start()
	  socketInitiator
	}

	val fixInitiator = initFixInitiator()
	val fixSessions = fixInitiator.getManagedSessions.asScala.toList

	def runningStrategy: RunningStrategy = new FileBasedPublisherStrategy(fixSessions, appConfig)

	runningStrategy.startWork()

	fixInitiator.stop()
  }

  def resolveConfigName: String = {
	Option[String](System.getProperty("config.name")) match {
	  case None =>
		logger.warn("No VM property defined config.name. Running config: general")
		"general"
	  case Some(c) => c
	}
  }

}


