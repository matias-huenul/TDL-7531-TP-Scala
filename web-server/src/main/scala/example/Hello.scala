import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.json4s.{DefaultFormats, Formats, JValue}
import org.json4s.native.JsonMethods.parse

import example.utils.Utils.{sendMessage, parseMessage, handleCommand}

object WebServer {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("web-server")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val formats: Formats = DefaultFormats

    val route =
      path("") {
        post {
          entity(as[String]) { body =>
            complete {
              try {
                println(s"Received request with body $body")

                val jsonBody = parse(body)

                val chatId = (jsonBody \ "message" \ "chat" \ "id").extract[String]
                val userName = (jsonBody \ "message" \ "from" \ "first_name").extract[String]
                val text = (jsonBody \ "message" \ "text").extract[String]

                val (command, args) = parseMessage(text)
                val message = handleCommand(command, args)
                println(s"Sending message: $message")

                sendMessage(chatId, message).map { response =>
                  println(s"Telegram API response: ${response.status}")
                  response.discardEntityBytes()
                  StatusCodes.OK
                }
              } catch {
                case e: Exception =>
                  println(s"An error ocurred: $e")
                  StatusCodes.OK
              }
            }
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
    println("Server started at http://localhost:8080/")

    scala.sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    }
  }
}
