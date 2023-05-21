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
    //ad dependencies for java.io.FileOutputStream and java.net.URL
    libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2",
    //mysql
    libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.32",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.5.4",

  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
