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

import example.lib.Utils

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

    val table = args("operacion").toLowerCase match {
      case "venta" => "properties_sale"
      case "alquiler" => "properties_rent"
      case _ => null
    }

    if (table == null) {
      return Future.successful(List.empty[Map[String, String]])
    }
  
    val baseUrl = sys.env("SUPABASE_API_URL") + s"/rest/v1/$table?select=neighborhood,rooms,price,currency,property_type,url&limit=10&"

    val argsMapping = Map(
      "ubicacion" -> "neighborhood",
      "ambientes" -> "rooms",
      "precio" -> "price",
      "moneda" -> "currency",
      "tipo" -> "property_type",
    )

    val argsMapped = args.collect {
      case (key, value) if argsMapping.contains(key) => {
        if (key == "tipo") {
          (argsMapping(key), value.toUpperCase)
        } else {
          (argsMapping(key), value)
        }
      }
    }

    val url = baseUrl + makeQueryString(argsMapped, false).replace(" ", "%20")

    val headers: List[HttpHeader] = List(
      RawHeader("apikey", apiKey),
      Authorization(OAuth2BearerToken(apiKey)),
    )

    Utils.makeHttpRequest(url, headers = headers).map { body =>
      body.extract[List[Map[String, String]]]
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
    val apiKey = sys.env("SUPABASE_API_KEY")
    val baseUrl = sys.env("SUPABASE_API_URL") + "/rest/v1/rpc/estimate_property_value?"

    val argsMapping = Map(
      "ubicacion" -> "_location",
      "ambientes" -> "_rooms",
      "tipo" -> "_property_type",
      "superficie" -> "_surface_total",
    )
    val argsMapped = args.map { case (key, value) => (argsMapping(key), value) }

    val url = baseUrl + makeQueryString(argsMapped, true).replace(" ", "%20")

    val headers: List[HttpHeader] = List(
      RawHeader("apikey", apiKey),
      Authorization(OAuth2BearerToken(apiKey)),
    )

    Utils.makeHttpRequest(url, headers = headers).map { body =>
      body.extract[List[Map[String, String]]]
    }
  }
}
