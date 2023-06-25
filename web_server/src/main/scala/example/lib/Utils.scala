package example.lib

import akka.actor.ActorSystem
import org.json4s._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.JsonMethods._
import scala.concurrent.Future
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.stream.Materializer
import scala.concurrent.ExecutionContext
import akka.util.ByteString

object Utils {
  implicit val system = ActorSystem("utils")
  implicit val formats: Formats = DefaultFormats

  /** Make a HTTP request.
    *
    * @param url The URL of the request.
    * @param method The HTTP method of the request.
    * @param body The body of the request.
    * @return The response to the request.
    */
  def makeHttpRequest(
    url: String,
    method: HttpMethod = HttpMethods.GET,
    body: String = "",
    headers: List[HttpHeader] = List.empty[HttpHeader]
  )(implicit ec: ExecutionContext): Future[JValue] = {
    val request = HttpRequest(
      method = method,
      uri = url,
      entity = HttpEntity(ContentTypes.`application/json`, body),
      headers = headers
    )

    Http().singleRequest(request).flatMap { response => 
      response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
        parse(body.utf8String)
      }
    }
  }
  
  /** Get the current USD to ARS conversion.
    *
    * @return The USD to ARS conversion.
    */
  def getUsdToArsConversion()(implicit ec: ExecutionContext): Future[Double] = {
    val url = "https://api.bluelytics.com.ar/v2/latest"
    makeHttpRequest(url, HttpMethods.GET).map { body =>
      (body \ "blue" \ "value_sell").extract[Double]
    }
  }
}
