import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

object WebServer {
  def main(args: Array[String]): Unit = {
    // Initialize the actor system and materializer
    implicit val system = ActorSystem("web-server")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

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
              StatusCodes.OK
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
