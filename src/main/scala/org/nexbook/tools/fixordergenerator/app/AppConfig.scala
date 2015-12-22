package org.nexbook.tools.fixordergenerator.app

import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
  * Created by milczu on 11.12.15
  */
class AppConfig(config: Config) {

  lazy val supportedSymbols = config.getStringList("symbols").asScala.toList

  lazy val fixConfig = config.getConfig("fix")

  lazy val generatorConfig = config.getConfig("generator")
}
