package example

import scalaj.http.Http
import scala.io.Source
import org.json4s.{DefaultFormats, Formats, JValue}
import org.json4s.native.JsonMethods.parse
import org.apache.spark.sql.{SparkSession, DataFrame}

import example.utils.Utils

object Hello extends App {
  implicit val formats: Formats = DefaultFormats

  val response: String = Http("https://dummyjson.com/products/1").asString.body
  val json: JValue = parse(response)

  val title: String = (json \ "title").extract[String]
  val firstImage: String = (json \ "images")(0).extract[String]

  println(s"The title is $title")
  println(s"The first image is $firstImage")

  val images: List[String] = (json \ "images").extract[List[String]]

  println(s"The second image is ${images(1)}")
  println(s"The last image is ${images.last}")

  for (image <- images) {
    println(s"Image: $image")
  }

  val spark: SparkSession = SparkSession.builder()
    .appName("Example")
    .master("local[*]")
    .getOrCreate()

  val data: Seq[(Int, String)] = Seq((1, "a"), (2, "b"), (3, "c"))
  val df: DataFrame = spark.createDataFrame(data).toDF("id", "code")

  df.show()

  Utils.test()

  println("hello world")

  val a = spark.read
    .format("jdbc")
    .option("driver", "org.postgresql.Driver")
    .option("url", "jdbc:postgresql://db.igdnlrrqfnwivrfldsyy.supabase.co:5432/postgres")
    .option("dbtable", "properties")
    .option("user", "postgres")
    .option("password", "+?gZMK.KFtxC@3x")
    .load()

  a.show()
}
