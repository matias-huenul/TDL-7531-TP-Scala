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

  def makeQueryString(args: Map[String, String]): String = {
    args.map { case (key, value) => s"$key=eq.$value" }.mkString("&")
  }

  /** Search properties in the database.
    *
    * @param args The filter arguments of the search.
    * @return The response to the search.
    */
  def searchProperties(
    args: Map[String, String]
  )(implicit ec: ExecutionContext): Future[List[Map[String, String]]] = {
    val apiKey = sys.env("SUPABASE_API_KEY")
    val baseUrl = sys.env("SUPABASE_API_URL") + "/rest/v1/properties?select=l3,rooms,price,currency,property_type,operation_type&limit=3&"
    val url = baseUrl + makeQueryString(args)

    println(s"URL: $url")

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
        println(s"JSON body: $jsonBody")
        val results = jsonBody.extract[List[Map[String, String]]]
        println(s"Results: $results")
        results
      }
    }
  }
}
