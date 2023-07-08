package etl.lib

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken, RawHeader }
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http

import org.json4s._
import org.json4s.native.Serialization._
import org.json4s.native.Serialization

import scala.concurrent.{ ExecutionContextExecutor, Future }

import scala.util.{ Failure, Success }

object Utils {
  implicit val formats: Formats = Serialization.formats(NoTypeHints)
  implicit val system: ActorSystem = ActorSystem("properties-etl")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def updateLoadedColumn(url: String, fileName: String, value: String): Unit = {
    val apiUrl: String = sys.env("SUPABASE_API_URL")
    val baseUrl: String = s"$apiUrl/rest/v1/rpc/update_loaded_col"
    val supabaseApiKey: String = sys.env("SUPABASE_API_KEY")
    val newValues: Map[String, String] = Map("_url" -> s"$url", "_value" -> s"$value")

    val headers: List[HttpHeader] = List(
      RawHeader("apikey", supabaseApiKey),
      Authorization(OAuth2BearerToken(supabaseApiKey)),
    )

    makeHttpRequest(baseUrl, HttpMethods.POST, write(newValues), headers, fileName)
  }

  def makeHttpRequest(
    url: String,
    method: HttpMethod,
    body: String,
    headers: List[HttpHeader],
    fileName: String
  ): Unit = {
    val request = HttpRequest(
      method = method,
      uri = url,
      entity = HttpEntity(ContentTypes.`application/json`, body),
      headers = headers
    )

    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

    responseFuture.onComplete {
      case Success(response) => 
        val statusCode: StatusCode = response.status
        val code: Int = statusCode.intValue
        if (code == 204) {
          println(s"Success on updating loaded column for $fileName")
        } else {
          println(s"Error on updating loaded column for $fileName")
        }
        system.terminate()
      case Failure(ex) => 
        println(s"Request failed with exception: $ex")
        system.terminate()
    }
  }
}
