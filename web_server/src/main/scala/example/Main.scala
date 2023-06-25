import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import example.lib.Telegram
import example.lib.Commands
import example.lib.Database

object Main extends App {
  implicit val system = ActorSystem("main")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val port = 8080

  val route = path("") {
    post {
      entity(as[String]) { body =>
        complete {
          Telegram.parseMessage(body) match {
            case Some((chatId, text)) =>
              Commands.handleMessage(text).flatMap { responseMessage =>
                Telegram.sendMessage(chatId, responseMessage).map { response =>
                  response.discardEntityBytes()
                  StatusCodes.OK
                }
              }.recover {
                case e: Exception =>
                  println(s"Error during message handling: $e")
                  StatusCodes.OK
              }

            case None =>
              println(s"Telegram update is not a message, ignoring")
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
