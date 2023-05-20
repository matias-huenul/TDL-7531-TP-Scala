package example.utils

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

  case class TelegramMessage(chat_id: String, text: String)

  def parseMessage(text: String): (String, Map[String, String]) = {
    val commandPattern = """^/(\w+)(\s+\w+=\w+[,\s*\w+=\w+]*)*""".r

    text match {
      case commandPattern(command, args) =>
        val argMap = Option(args) match {
          case Some(argsString) =>
            val argList = argsString.split(",").map(_.trim)
            argList.map(_.split("=")).collect { case Array(key, value) => key -> value }.toMap
          case None => Map.empty[String, String]
        }
        (command, argMap)

      case _ => ("default", Map.empty[String, String])
    }
  }

  def greet(): String = {
    """¡Hola!
      |
      |Soy un bot diseñado para ayudarte a encontrar propiedades en venta o alquiler, así también como para estimar el valor de tus propiedades.
      |
      |Escribí /ayuda para obtener información sobre los comandos disponibles""".stripMargin
  }

  def help(): String = {
    """Los comandos disponibles son:
      |
      |/buscar - Buscar propiedades en venta o alquiler
      |  Parámetros:
      |    - ubicacion: Ubicación de la propiedad
      |    - tipo: Tipo de propiedad (casa, departamento, local, terreno)
      |    - operacion: Tipo de operación (venta, alquiler)
      |  Ejemplo de uso:
      |    /buscar ubicacion=Palermo, tipo=departamento, operacion=venta
      |
      |/tasar - Obtener un valor estimado de tu propiedad
      |  Parámetros:
      |    - ubicacion: Ubicación de la propiedad
      |    - tipo: Tipo de propiedad (casa, departamento, local, terreno)
      |    - superficie: Superficie de la propiedad
      |  Ejemplo de uso:
      |    /tasar ubicacion=Palermo, tipo=departamento, superficie=50""".stripMargin
  }

  def searchProperties(args: Map[String, String]): String = {
    val location = args.getOrElse("ubicacion", "")
    val propertyType = args.getOrElse("tipo", "")
    val operationType = args.getOrElse("operacion", "")

    val message =
      s"""Buscando propiedades en $location
        |Tipo: $propertyType
        |Operación: $operationType""".stripMargin

    message
  }

  def estimatePropertyValue(args: Map[String, String]): String = {
    val propertyType = args.getOrElse("tipo", "")
    val location = args.getOrElse("ubicacion", "")
    val propertySize = args.getOrElse("superficie", "")

    val message =
      s"""Estimando el valor de tu propiedad en $location
        |Tipo: $propertyType
        |Tamaño: $propertySize""".stripMargin

    message
  }

  def handleCommand(command: String, args: Map[String, String]): String = {
    command match {
      case "buscar" => searchProperties(args)
      case "tasar" => estimatePropertyValue(args)
      case "ayuda" => help()
      case _ => greet()
    }
  }

  def sendMessage(chatId: String, text: String)(implicit mat: Materializer): Future[HttpResponse] = {
    val telegramApiKey = sys.env("TELEGRAM_API_TOKEN")
    val message = TelegramMessage(chatId, text)
    val telegramRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = s"https://api.telegram.org/bot$telegramApiKey/sendMessage",
      entity = HttpEntity(ContentTypes.`application/json`, write(message))
    )
    Http().singleRequest(telegramRequest)
  }
}
