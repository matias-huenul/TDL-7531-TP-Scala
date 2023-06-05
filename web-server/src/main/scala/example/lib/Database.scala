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

  def makeQueryString(args: Map[String, String], isFunctionCall: Boolean): String = {
    if (isFunctionCall) {
      args.map { case (key, value) => s"$key=$value" }.mkString("&")
    } else {
      args.map { case (key, value) => s"$key=eq.$value" }.mkString("&")
    }
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
    val url = baseUrl + makeQueryString(args, false)

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

  /** Estimate property value.
    *
    * @param args The filter arguments of the search.
    * @return The estimated value in ARS.
    */
  def estimatePropertyValue(
    args: Map[String, String]
  )(implicit ec: ExecutionContext): Future[List[Map[String, String]]] = {
    // Para usar esta función, primero hay que crearla en la base de datos:

    // CREATE OR REPLACE FUNCTION estimate_property_value(location varchar, n_rooms varchar DEFAULT NULL)
    // RETURNS TABLE (estimated_property_value int, currency varchar) AS $$
    //   SELECT
    //     CAST(AVG(CAST(price AS int)) AS int) AS estimated_property_value,
    //     currency
    //   FROM
    //     properties
    //   WHERE
    //     l3 = location
    //     AND (n_rooms IS NULL OR rooms = n_rooms)
    //   GROUP BY currency
    // $$ LANGUAGE SQL IMMUTABLE

    // SELECT * from estimate_property_value('Recoleta')

    val apiKey = sys.env("SUPABASE_API_KEY")
    val baseUrl = sys.env("SUPABASE_API_URL") + "/rest/v1/rpc/estimate_property_value?"    
    val url = baseUrl + makeQueryString(args, true)

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
