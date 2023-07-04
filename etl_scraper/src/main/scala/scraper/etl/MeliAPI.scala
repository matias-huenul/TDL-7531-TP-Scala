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
  val MELI_URL = "https://api.mercadolibre.com"
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
        1000
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
   * @return List of JValue with the API response 200
   */
  def getByIds(ids: List[String]): List[JValue] = {
    val groupedIds: List[String] = ids.grouped(20).map(ids => ids.mkString(",")).toList
    val urls = groupedIds.map(ids => s"$MELI_URL/items?ids=$ids")

    urls.flatMap(url => {
      getApiResponse(url) match {
        case Success(apiResponse) =>
          val responseJson = parseJson(apiResponse)
          responseJson.children.filter(data => (data \ "code").extract[Int] == 200)
        case Failure(ex) =>
          println(s"Failed to retrieve API response: ${ex.getMessage}")
          List()
      }
    })
  }

  /**
   * Get properties for rent in CABA from MercadoLibre API (max 1000 results for neighborhood)
   * Category ID: MLA1459
   * @return List of properties
   */
  def getRentPropertiesCABA: Set[Property] = {
    // Get neighborhoods from wikipedia?
    val neighborhoods: List[String] = List("Agronomia", "Almagro", "Balvanera", "Barracas", "Belgrano", "Boedo", "Caballito",
      "Chacarita", "Coghlan", "Colegiales", "Constitucion", "Flores", "Floresta", "La%20Boca", "La%20Paternal", "Liniers",
      "Mataderos", "Monte%20Castro", "Monserrat", "Nueva%20Pompeya", "Nunez", "Palermo", "Parque%20Avellaneda", "Parque%20Chacabuco",
      "Parque%20Chas", "Parque%20Patricios", "Puerto%20Madero", "Recoleta", "Retiro", "Saavedra", "San%20Cristobal",
      "San%20Nicolas", "San%20Telmo", "Velez%20Sarsfield", "Versalles", "Villa%20Crespo", "Villa%20del%20Parque",
      "Villa%20Devoto", "Villa%20General%20Mitre", "Villa%20Lugano", "Villa%20Luro", "Villa%20Ortuzar", "Villa%20Pueyrredon",
      "Villa%20Real", "Villa%20Riachuelo", "Villa%20Santa%20Rita", "Villa%20Soldati", "Villa%20Urquiza")

    println("Generating links...")
    val baseUrl = s"$MELI_URL/sites/MLA/search?category=MLA1473"
    val urls = neighborhoods.flatMap(neighborhood => {
      val max = getMaxResults(s"$baseUrl&q=$neighborhood")
      (0 to max by 50).map(offset => s"$baseUrl&q=$neighborhood&offset=$offset")
    })

    val pb = new ProgressBarBuilder()
      .setInitialMax(urls.size)
      .setTaskName("Properties from MELI")
      .setUnit(" url", 1)
      .setStyle(ProgressBarStyle.ASCII)
      .build()

    val properties = urls.flatMap(url => {
      pb.step()
      getApiResponse(url) match {
        case Success(apiResponse) =>
          val data = parseJson(apiResponse)
          val ids = getIds(data \ "results")
          getByIds(ids).map(data => parseProperties(data \ "body"))
        case Failure(ex) =>
          println(s"Failed to retrieve API response: ${ex.getMessage}")
          List[Property]()
      }
    }).toSet


    pb.close()
    system.terminate()
    properties
  }
}
