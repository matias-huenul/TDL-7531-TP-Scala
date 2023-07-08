package etl

import scalaj.http.Http
import scala.io.Source
import org.json4s.{DefaultFormats, Formats, JValue}
import org.json4s.native.JsonMethods.parse
import org.apache.spark.sql.{SparkSession, DataFrame}

import etl.lib.PropertiesETL

object Main extends App {
  println("Starting ETL")
  PropertiesETL.propertiesDataUpdate()

  println("Finished ETL")
}
