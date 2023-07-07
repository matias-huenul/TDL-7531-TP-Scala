package server.lib

import akka.actor.ActorSystem
import org.json4s._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.JsonMethods._
import org.json4s.native.JsonMethods.parse
import scala.concurrent.Future
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken, RawHeader}
import scala.concurrent.ExecutionContext

object Database {
  implicit val system = ActorSystem("database")
  implicit val formats: Formats = DefaultFormats

  val supabaseApiKey = sys.env("SUPABASE_API_KEY")
  val supabaseApiUrl = sys.env("SUPABASE_API_URL")

  /** Make a Supabase query string from a map of arguments.
    *
    * @param args The arguments of the query.
    * @param isFunctionCall Whether the query is a Supabase function call or not.
    * @return The query string.
    */
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
    val table = args("operacion").toLowerCase match {
      case "venta" => "properties_sale"
      case "alquiler" => "properties_rent"
      case _ => null
    }

    if (table == null) {
      return Future.successful(List.empty[Map[String, String]])
    }
  
    val baseUrl = s"$supabaseApiUrl/rest/v1/$table?select=neighborhood,rooms,price,currency,property_type,url&limit=10&"

    val argsMapping = Map(
      "ubicacion" -> "neighborhood",
      "ambientes" -> "rooms",
      "precio" -> "price",
      "moneda" -> "currency",
      "tipo" -> "property_type",
    )

    val argsMapped = args.collect {
      case (key, value) if argsMapping.contains(key) => {
        if (key == "tipo" || key == "moneda") {
          (argsMapping(key), value.toUpperCase)
        } else {
          (argsMapping(key), value)
        }
      }
    }

    val url = baseUrl + makeQueryString(argsMapped, false)

    val headers: List[HttpHeader] = List(
      RawHeader("apikey", supabaseApiKey),
      Authorization(OAuth2BearerToken(supabaseApiKey)),
    )

    Utils.makeHttpRequest(url, headers = headers).map { body =>
      body.extract[List[Map[String, String]]]
    }
  }

  /** Estimate property value.
    *
    * @param args The filter arguments of the search.
    * @return The estimated value in ARS and USD.
    */
  def estimatePropertyValue(
    args: Map[String, String]
  )(implicit ec: ExecutionContext): Future[List[Map[String, String]]] = {
    val baseUrl = s"$supabaseApiUrl/rest/v1/rpc/estimate_property_value?"

    val argsMapping = Map(
      "ubicacion" -> "_location",
      "ambientes" -> "_rooms",
      "tipo" -> "_property_type",
      "superficie" -> "_surface_total",
    )
    val argsMapped = args.map { case (key, value) => (argsMapping(key), value) }

    val url = baseUrl + makeQueryString(argsMapped, true)

    val headers: List[HttpHeader] = List(
      RawHeader("apikey", supabaseApiKey),
      Authorization(OAuth2BearerToken(supabaseApiKey)),
    )

    Utils.makeHttpRequest(url, headers = headers).map { body =>
      body.extract[List[Map[String, String]]]
    }
  }
}
