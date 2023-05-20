package example.lib

import akka.actor.ActorSystem
import org.json4s._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.JsonMethods._
import scala.concurrent.Future
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.stream.Materializer

object Commands {
  implicit val system = ActorSystem("commands")
  implicit val formats: Formats = DefaultFormats

  /** Handle a message and return a response.
    *
    * @param text The text of the message to handle.
    * @return The response to the message.
    */
  def handleMessage(text: String): String = {
    val (command, args) = parseMessage(text)
    handleCommand(command, args)
  }

  /** Parse a message content.
    *
    * @param text The text of the message to parse.
    * @return A tuple containing the command and the arguments of the message.
    */
  def parseMessage(text: String): (String, Map[String, String]) = {
    val commandPattern = """^/(\w+)(\s+\w+=\w+[,\s*\w+=\w+]*)*""".r

    text match {
      case commandPattern(command, args) =>
        val argMap = Option(args) match {
          case Some(argsString) =>
            val argList = argsString.split(",").map(_.trim)
            argList
              .map(_.split("="))
              .collect { case Array(key, value) => key -> value }
              .toMap
          case None => Map.empty[String, String]
        }
        (command, argMap)

      case _ => ("default", Map.empty[String, String])
    }
  }

  /** Handle a command and return a response.
    *
    * @param command The command to handle.
    * @param args The arguments of the command.
    * @return The response to the command.
    */
  def handleCommand(command: String, args: Map[String, String]): String = {
    command match {
      case "buscar" => searchProperties(args)
      case "tasar" => estimatePropertyValue(args)
      case "ayuda" => help()
      case _ => greet()
    }
  }

  /** Return a greeting message.
    *
    * @return The greeting message.
    */
  def greet(): String = {
    """¡Hola!
      |
      |Soy un bot diseñado para ayudarte a encontrar propiedades en venta o alquiler, así también como para estimar el valor de tus propiedades.
      |
      |Escribí /ayuda para obtener información sobre los comandos disponibles""".stripMargin
  }

  /** Return a help message.
    *
    * @return The help message.
    */
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

  /** Search properties.
    *
    * @param args The arguments of the command.
    * @return The response to the command.
    */
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

  /** Estimate the value of a property.
    *
    * @param args The arguments of the command.
    * @return The response to the command.
    */
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
}
