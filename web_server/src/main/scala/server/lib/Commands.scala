package server.lib

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
        |  *Parámetros:*
        |    - tipo: Tipo de propiedad (casa, departamento)
        |    - operacion: Tipo de operación (venta, alquiler)
        |    - ubicacion: Ubicación de la propiedad
        |    - ambientes: Cantidad de ambientes
        |    - precio: Precio de la propiedad
        |  *Ejemplo de uso:*
        |    /buscar ubicacion=Palermo, operacion=venta
        |
        |/tasar - Obtener un valor estimado de tu propiedad
        |  *Parámetros:*
        |    - tipo: Tipo de propiedad (casa, departamento)
        |    - ubicacion: Ubicación de la propiedad
        |    - ambientes: Cantidad de ambientes
        |    - superficie: Superficie de la propiedad
        |  *Ejemplo de uso:*
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
    val operationType = args("operacion").toLowerCase

    Database.searchProperties(args).map { properties =>
      if (properties.isEmpty) {
        "No se encontraron propiedades con los parámetros especificados."
      } else {
        val resultString = properties.map { property =>
          val propertyType = property("property_type").toLowerCase.capitalize
          val l3 = property("neighborhood")
          val rooms = property("rooms")
          val price = NumberFormat.getNumberInstance.format(property("price").toInt)
          val currency = property("currency")
          val url = property("url")
          s"- [$propertyType en $operationType en $l3 de $rooms ambientes: $price $currency]($url)"
        }
        .mkString("\n")
        s"Resultados de la búsqueda:\n\n$resultString"
      }
    }
  }

  /** Estimate the value of a property.
    *
    * @param args The arguments of the command.
    * @return The response to the command.
    */
  def estimatePropertyValue(args: Map[String, String])(implicit ec: ExecutionContext): Future[String] = {
    Database.estimatePropertyValue(args).flatMap { estimatedValues =>
      Utils.getUsdToArsConversion().map { conversion =>
        if (estimatedValues.isEmpty) {
          "No se pudo estimar el valor de tu propiedad ya que no se encontraron propiedades similares."
        } else {
          val estimatedValuesInArs: List[Int] = estimatedValues.map { estimatedValue =>
            val value = estimatedValue("estimated_property_value").toInt
            val currency = estimatedValue("currency")
            if (currency == "ARS") {
              value
            } else {
              (value * conversion).toInt            
            }
          }

          val estimatedValue = (estimatedValuesInArs.sum / estimatedValuesInArs.length).toInt
          val estimatedValueString = NumberFormat.getNumberInstance.format(estimatedValue)
          val estimatedValueUsd = (estimatedValue / conversion).toInt
          val estimatedValueUsdString = NumberFormat.getNumberInstance.format(estimatedValueUsd)
          s"El valor estimado de tu propiedad es $estimatedValueString ARS ($estimatedValueUsdString USD)"
        }
      }
    }
  }
}
