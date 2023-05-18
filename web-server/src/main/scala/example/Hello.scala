import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.json4s.{DefaultFormats, Formats, JValue}
import org.json4s.native.JsonMethods.parse

object WebServer {
  def main(args: Array[String]): Unit = {
    // Initialize the actor system and materializer
    implicit val system = ActorSystem("web-server")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val formats: Formats = DefaultFormats

    // Define the route for the GET and POST requests to the root ("/") endpoint
    val route =
      path("") {
        get {
          complete(
            HttpEntity(ContentTypes.`application/json`,
              """{"message": "Hello, world!"}"""
            )
          )
        } ~
        post {
          entity(as[String]) { body =>
            complete {
              println(s"Received request with body: $body")

              val jsonBody = parse(body)
              val chatId = (jsonBody \ "message" \ "chat" \ "id").extract[String]
              val userName = (jsonBody \ "message" \ "from" \ "first_name").extract[String]
              val message: String =
                s"""¡Hola $userName!
                  |
                  |Soy un bot diseñado para ayudarte a encontrar propiedades en venta o alquiler, así también como para estimar el valor de tus propiedades.
                  |
                  |Los comandos disponibles son:
                  |
                  |/buscar - Buscar propiedades en venta o alquiler
                  |/tasar - Obtener un valor estimado de tu propiedad
                  |/ayuda - Obtener ayuda sobre los comandos disponibles""".stripMargin

              val telegramApiKey = sys.env("TELEGRAM_API_KEY")
              // Make the POST request to the Telegram API
              val telegramRequest = HttpRequest(
                method = HttpMethods.POST,
                uri = s"https://api.telegram.org/bot$telegramApiKey/sendMessage",
                entity = HttpEntity(ContentTypes.`application/json`,
                  s"""
                    |{
                    |  "chat_id": $chatId,
                    |  "text": "$message",
                    |}
                    |""".stripMargin)
              )
              val responseFuture = Http().singleRequest(telegramRequest)

              // Handle the response from the Telegram API
              responseFuture.map { response =>
                println(s"Telegram API response: ${response.status}")
                response.discardEntityBytes() // Consume the response entity
                StatusCodes.OK
              }
            }
          }
        }
      }

    // Start the server
    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
    println("Server started at http://localhost:8080/")

    // Terminate the server when the application is shut down
    scala.sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    }
  }
}
