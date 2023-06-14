package example

import scalaj.http.Http
import scala.io.Source
import org.json4s.{DefaultFormats, Formats, JValue}
import org.json4s.native.JsonMethods.parse
import org.apache.spark.sql.{SparkSession, DataFrame}

import example.utils.Utils
import example.PropertiesETL

object Hello extends App {
  println("Starting ETL")
  PropertiesETL.propertiesDataUpdate()

  Utils.test()

  println("Finished ETL")
}
