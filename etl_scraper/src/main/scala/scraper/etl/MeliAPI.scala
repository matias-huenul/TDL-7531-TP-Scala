package scraper.etl


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import scraper.etl.model.Property
import scraper.etl.utils.{Currency, Page}
import me.tongfei.progressbar.{ProgressBarBuilder, ProgressBarStyle}
import org.json4s.native.parseJson
import org.json4s.{DefaultFormats, JValue}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
object MeliAPI {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val formats: DefaultFormats.type = DefaultFormats

  /**
   * Get API response from url
   * @param url: String with the url to get the API response
   * @return API response in String format
   */
  private def getApiResponse(url: String): Try[String] = {
    val request = HttpRequest(uri = url)

    val response: Try[HttpResponse] = Try(Await.result(Http().singleRequest(request), 5.seconds))

    response.flatMap { httpResponse =>
      Try(Await.result(Unmarshal(httpResponse.entity).to[String], 5.seconds))
    }
  }

  /**
   * Returns the ids from the API response
   * @param data API response
   * @return List of ids in String format
   */
  private def getIds(data: JValue): List[String] = {
    val ids = new ListBuffer[String]()

    for (result <- data.children) {
      ids += (result \ "id").extract[String]
    }

    ids.toList
  }

  private def getMaxResults(url: String): Int = {
    getApiResponse(url) match {
      case Success(apiResponse) =>
        val data = parseJson(apiResponse)
        val paging = data \ "paging"
        val total = (paging \ "total").extract[Int]
        total.min(1000)
      case Failure(ex) =>
        println(s"Failed to retrieve API response: ${ex.getMessage}")
        0
    }
  }

  private def getValueFromAttribute(attribute: JValue): Int = {
    val attributeId = (attribute \ "id").extract[String]
    val value = if (List("TOTAL_AREA", "COVERED_AREA", "MAINTENANCE_FEE").contains(attributeId)) {
      (attribute \ "value_struct" \ "number").extractOpt[String].getOrElse("0")
    } else {
      (attribute \ "value_name").extractOpt[String].getOrElse("0")
    }

    if(value.contains(".")) value.split("\\.")(0).toInt else value.toInt
  }

  /**
   * Parse the API response and return a Property object
   * @param data: JValue API response
   * @return Property
   */
  def parseProperties(data:JValue): Property ={
    val property = new Property()
    property.url = (data \ "permalink").extract[String]
    property.price = (data \ "price").extract[Int]
    property.currency = Currency.fromString((data \ "currency_id").extract[String])
    property.neighborhood = (data \ "location" \ "neighborhood" \ "name").extractOpt[String].getOrElse("")
    property.address = (data \ "location" \ "address_line").extractOpt[String].getOrElse("")
    property.page = Page.MELI

    val attributes = data \ "attributes"

    for(attribute <- attributes.children){


      (attribute \ "id").extract[String] match {
        case "ROOMS" => property.rooms = getValueFromAttribute(attribute)
        case "BEDROOMS" => property.bedrooms = getValueFromAttribute(attribute)
        case "FULL_BATHROOMS" => property.bathrooms = getValueFromAttribute(attribute)
        case "TOTAL_AREA" => property.totalArea = getValueFromAttribute(attribute)
        case "COVERED_AREA" => property.coveredArea = getValueFromAttribute(attribute)
        case "MAINTENANCE_FEE" => property.expenses = getValueFromAttribute(attribute)
        case "PARKING_LOTS" => property.garage = getValueFromAttribute(attribute)
        case _ => ()
      }
    }
    property
  }

  /**
   * Get properties publivation by ID from MercadoLibre API
   * @param ids List of IDs to retrieve(have to be ids of properties)
   * @return List of Properties
   */
  def getByIds(ids: List[String]): List[Property] = {
    val properties = new ListBuffer[Property]()
    val groupedIds: List[String] = ids.grouped(20).map(ids => ids.mkString(",")).toList

    for(groupedId <- groupedIds) {
      val url = s"https://api.mercadolibre.com/items?ids=$groupedId"
      getApiResponse(url) match {
        case Success(apiResponse) =>
          val responseJson = parseJson(apiResponse)
          for (data <- responseJson.children) {
            if((data \ "code").extract[Int] == 200) properties += parseProperties(data \ "body")
          }
        case Failure(ex) =>
          println(s"Failed to retrieve API response: ${ex.getMessage}")
      }
    }
    properties.toList
  }

  /**
   * Get properties for rent in CABA from MercadoLibre API (max 1000 results for neighborhood)
   * Category ID: MLA1459
   * @return List of properties
   */
  def getRentPropertiesCABA: List[Property] = {
    // Get neighborhoods from wikipedia?
    val neighborhoods: List[String] = List("Agronomia", "Almagro", "Balvanera", "Barracas", "Belgrano", "Boedo", "Caballito",
      "Chacarita", "Coghlan", "Colegiales", "Constitucion", "Flores", "Floresta", "La%20Boca", "La%20Paternal", "Liniers",
      "Mataderos", "Monte%20Castro", "Monserrat", "Nueva%20Pompeya", "Nunez", "Palermo", "Parque%20Avellaneda", "Parque%20Chacabuco",
      "Parque%20Chas", "Parque%20Patricios", "Puerto%20Madero", "Recoleta", "Retiro", "Saavedra", "San%20Cristobal",
      "San%20Nicolas", "San%20Telmo", "Velez%20Sarsfield", "Versalles", "Villa%20Crespo", "Villa%20del%20Parque",
      "Villa%20Devoto", "Villa%20General%20Mitre", "Villa%20Lugano", "Villa%20Luro", "Villa%20Ortuzar", "Villa%20Pueyrredon",
      "Villa%20Real", "Villa%20Riachuelo", "Villa%20Santa%20Rita", "Villa%20Soldati", "Villa%20Urquiza")


    val rentProperties = new ListBuffer[Property]()
    val pb = new ProgressBarBuilder()
      .setInitialMax(neighborhoods.size)
      .setTaskName("Properties from MELI")
      .setUnit(" neighborhood", 1)
      .setStyle(ProgressBarStyle.ASCII)
      .build()

    for (neighborhood <- neighborhoods) {
      var max = 0
      var offset = 0
      do {
        val url = s"https://api.mercadolibre.com/sites/MLA/search?category=MLA1473&q=$neighborhood&offset=$offset"
        if (max == 0) max = getMaxResults(url)
        getApiResponse(url) match {
          case Success(apiResponse) =>
            val data = parseJson(apiResponse)
            val ids = getIds(data \ "results")
            val props = getByIds(ids)

            // Add properties to list that arent already in it by url
            props.foreach(prop => if(!rentProperties.exists(_.url == prop.url)) rentProperties += prop)
          case Failure(ex) =>
            println(s"Failed to retrieve API response: ${ex.getMessage}")
        }
        offset += 50
      }while (offset <= max)
      pb.step()
    }
    pb.close()
    system.terminate()
    rentProperties.toList
  }
}
