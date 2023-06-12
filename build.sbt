import Dependencies._

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "1.0"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "sample",
    libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2",
    libraryDependencies += "org.json4s" %% "json4s-native" % "4.0.6",
    libraryDependencies += "org.apache.spark" %% "spark-core" % "3.4.0" % "provided",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.4.0" % "provided",
    libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.5.4",
    libraryDependencies += "org.jsoup" % "jsoup" % "1.15.4",
    libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "3.1.0",
    libraryDependencies += "me.tongfei" % "progressbar" % "0.9.5",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.7",
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}