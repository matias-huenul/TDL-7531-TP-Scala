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
import java.text.NumberFormat

import example.lib.Database
import example.lib.Utils

object Commands {
  implicit val system = ActorSystem("commands")
  implicit val formats: Formats = DefaultFormats

  /** Handle a message and return a response.
    *
    * @param text The text of the message to handle.
    * @return The response to the message.
    */
  def handleMessage(text: String)(implicit ec: ExecutionContext): Future[String] = {
    val (command, args) = parseMessage(text)
    println(s"Command: $command, args: $args")
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
  def handleCommand(command: String, args: Map[String, String])(implicit ec: ExecutionContext): Future[String] = {
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
  def greet()(implicit ec: ExecutionContext): Future[String] = {
    Future {
      """¡Hola!
        |
        |Soy un bot diseñado para ayudarte a encontrar propiedades en venta o alquiler, así también como para estimar el valor de tus propiedades.
        |
        |Escribí /ayuda para obtener información sobre los comandos disponibles""".stripMargin
    }
  }

  /** Return a help message.
    *
    * @return The help message.
    */
  def help()(implicit ec: ExecutionContext): Future[String] = {
    Future {
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
  }

  /** Search properties.
    *
    * @param args The arguments of the command.
    * @return The response to the command.
    */
  def searchProperties(
    args: Map[String, String]
  )(implicit ec: ExecutionContext): Future[String] = {
    println(s"Searching properties with args: $args")
    Database.searchProperties(args).map { properties =>
      properties.map { property =>
        val operationType = property("operation_type")
        val propertyType = property("property_type")
        val l3 = property("l3")
        val rooms = property("rooms")
        val price = property("price")
        val currency = property("currency")
        s"$operationType $propertyType en $l3, $rooms ambientes, $price $currency"
      }
      .mkString("\n")
    }
  }

  /** Estimate the value of a property.
    *
    * @param args The arguments of the command.
    * @return The response to the command.
    */
  def estimatePropertyValue(args: Map[String, String])(implicit ec: ExecutionContext): Future[String] = {
    println(s"Searching properties with args: $args")
    Database.estimatePropertyValue(args).flatMap { estimatedValues =>
      Utils.getUsdToArsConversion().map { conversion =>
        println(s"Property values: $estimatedValues")
        println(s"USD to ARS conversion: $conversion")

        val estimatedValuesInArs: List[Int] = estimatedValues.map { estimatedValue =>
          val value = estimatedValue("estimated_property_value").toInt
          val currency = estimatedValue("currency")
          if (currency == "ARS") {
            value
          } else {
            (value * conversion).toInt            
          }
        }

        println(s"Estimated values in ARS: $estimatedValuesInArs")
        val estimatedValue = (estimatedValuesInArs.sum / estimatedValuesInArs.length).toInt
        val estimatedValueString = NumberFormat.getNumberInstance.format(estimatedValue)
        s"El valor estimado de tu propiedad es $estimatedValueString ARS"
      }
    }
  }
}
