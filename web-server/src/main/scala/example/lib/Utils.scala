package example.lib

import akka.actor.ActorSystem
import org.json4s._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.JsonMethods._
import scala.concurrent.Future
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.stream.Materializer

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
    method: HttpMethod,
    body: String = ""
  ): Future[HttpResponse] = {
    val request = HttpRequest(
      method = method,
      uri = url,
      entity = HttpEntity(ContentTypes.`application/json`, body)
    )

    Http().singleRequest(request)
  }
}
