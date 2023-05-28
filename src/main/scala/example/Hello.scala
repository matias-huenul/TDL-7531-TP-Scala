package example

import scalaj.http.Http
import scala.io.Source
import org.json4s.{DefaultFormats, Formats, JValue}
import org.json4s.native.JsonMethods.parse
import org.apache.spark.sql.{SparkSession, DataFrame}

import example.utils.Utils

object Hello extends App {

  //println("hello world")

  WebScrapper.zonaprop()

  Utils.test()

  val prop = new Propiedad()

}
