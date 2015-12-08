name := "fix-order-generator"

version := "1.0"

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.8",
  "org.apache.mina" % "mina-core" % "1.1.7",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "net.liftweb" % "lift-json_2.11" % "2.6.2",
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.akka" % "akka-actor_2.11" % "2.3.12"
)

resolvers += Resolver.mavenLocal