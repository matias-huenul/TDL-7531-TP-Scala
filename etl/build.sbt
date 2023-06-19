import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "1.0"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "sample",
    libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2",
    libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.7",
    libraryDependencies += "org.apache.spark" %% "spark-core" % "3.4.0" % "provided",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.4.0" % "provided",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.6.0",
    libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.2",
    libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.7",
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
