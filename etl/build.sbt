import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "1.0"
ThisBuild / organization     := "com.etl"
ThisBuild / organizationName := "etl"

lazy val root = (project in file("."))
  .settings(
    name := "etl",
    libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2",
    libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.7",
    libraryDependencies += "org.apache.spark" %% "spark-core" % "3.4.0" % "provided",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.4.0" % "provided",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.6.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.4",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.17",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.17"
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case x => MergeStrategy.first
}
