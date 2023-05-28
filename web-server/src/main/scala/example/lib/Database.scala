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
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken, RawHeader}
import akka.util.ByteString
import scala.concurrent.ExecutionContext

object Database {
  implicit val system = ActorSystem("database")
  implicit val formats: Formats = DefaultFormats

  def searchProperties(args: Map[String, String])(implicit ec: ExecutionContext): Future[List[Map[String, String]]] = {
    val url = sys.env("SUPABASE_API_URL") + "/rest/v1/properties?select=l3,rooms&limit=3"
    val apiKey = sys.env("SUPABASE_API_KEY")

    val request = HttpRequest(
      uri = url,
      headers = List(
        RawHeader("apikey", apiKey),
        Authorization(OAuth2BearerToken(apiKey))
      )
    )

    Http().singleRequest(request).flatMap { response =>
      response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
        val jsonBody = parse(body.utf8String)
        jsonBody.extract[List[Map[String, String]]]

        // val responseMessage = properties
        //   .map { property =>
        //     val l3 = property("l3")
        //     val rooms = property("rooms")
        //     s"$l3 - $rooms"
        //   }
        //   .mkString("\n")
      }
    }
  }
}
