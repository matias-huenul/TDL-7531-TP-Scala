import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import example.lib.Telegram
import example.lib.Commands

object Main extends App {
  implicit val system = ActorSystem("main")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val port = 8080

  val route = path("") {
    post {
      entity(as[String]) { body =>
        complete {
          try {
            println(s"Received request with body $body")
            val (chatId, text) = Telegram.parseMessage(body)
            val responseMessage = Commands.handleMessage(text)
            println(s"Sending message: $responseMessage")

            Telegram
              .sendMessage(chatId, responseMessage)
              .map { response =>
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

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", port)
  println(s"Server started at http://localhost:$port/")

  scala.sys.addShutdownHook {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
