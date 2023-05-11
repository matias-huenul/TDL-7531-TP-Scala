package example

import scalaj.http.Http
import scala.io.Source
import org.json4s.native.JsonMethods.parse

object Hello extends Greeting with App {
  println(greeting)
  val response = Http("https://dummyjson.com/products/1").asString.body
  println(parse(response).toString())
}

trait Greeting {
  lazy val greeting: String = "Hello! This is a sample project."
}
