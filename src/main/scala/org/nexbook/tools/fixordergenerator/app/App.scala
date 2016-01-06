package org.nexbook.tools.fixordergenerator.app

import com.typesafe.config.ConfigFactory
import org.nexbook.tools.fixordergenerator.fix.{FixApplication, FixConnector}
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

	class FixRunner extends FixConnector {
	  val logger = LoggerFactory.getLogger(classOf[FixRunner])

	  val fixInitiator: SocketInitiator = {
		val fixOrderHandlerSettings = new SessionSettings(appConfig.fixConfig.getString("config.path"))
		val application = new FixApplication
		val storeFactory = new MemoryStoreFactory
		val messageFactory = new DefaultMessageFactory
		val fileLogFactory = new FileLogFactory(fixOrderHandlerSettings)
		val socketInitiator = new SocketInitiator(application, storeFactory, fixOrderHandlerSettings, fileLogFactory, messageFactory)
		socketInitiator.start()
		socketInitiator
	  }

	  val fixSessions = fixInitiator.getManagedSessions.asScala.toList

	  def stop() = fixInitiator.stop()
	}

	val fixRunner = new FixRunner
	fixRunner.waitForLogon()

	def runningStrategy: RunningStrategy = new AkkaNewOrderGeneratingStrategy(fixRunner.fixSessions, appConfig) with FixConnector

	runningStrategy.startWork()

	fixRunner.stop()
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


