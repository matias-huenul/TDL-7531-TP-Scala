package example.lib

import akka.actor.ActorSystem
import org.json4s._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.JsonMethods._
import org.json4s.native.JsonMethods.parse
import scala.concurrent.Future
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.stream.Materializer

object Database {
  implicit val system = ActorSystem("database")
  implicit val formats: Formats = DefaultFormats
}
