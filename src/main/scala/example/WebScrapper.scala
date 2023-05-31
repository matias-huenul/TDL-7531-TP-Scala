package example

import org.json4s._
import org.json4s.native.JsonMethods._
import org.jsoup._
import example.utils.Operation
import org.jsoup.nodes.Document

import scala.collection.mutable.ListBuffer
object WebScrapper{

  /**
   * Get the number of pages of zonaprop.com.ar
   * @param doc: Document of the first page of zonaprop.com.ar/casas-departamentos-ph-_-capital-federal
   * @return Int: Number of pages
   */
  def getNumberPagesZonaprop(doc: Document): Int = {
    val numberProperties = doc.getElementsByTag("h1").first().text().replaceAll("[^0-9]", "").toInt
    (numberProperties/20)+1 // 20 propiedades por pagina
  }

  /** Transform a string with json data to a list of json objects
   *
   * @param stringData: String with json data
   * @return List[JValue]: List of json objects
  */
  def parseJson(stringData: String): List[JValue] ={
    implicit val formats = DefaultFormats
    val result = stringData.split("\\{\"listPostings\":")(1).split("\\,\"listCondominium\"")(0)
    val json = parse(result)
    json.extract[JArray].arr
  }

  /**
   * Scrapes zonaprop.com.ar and returns a list of properties
   * @param operation: Operation to scrape (Alquiler, Venta)
   * @return List[Propiedad]: List of properties
  */
  def zonaprop(operation:Operation.Value = Operation.Alquiler): List[Propiedad] = {
    //Todo: add log
    implicit val formats = DefaultFormats

    val urlZonaProp = "https://www.zonaprop.com.ar"
    val url = urlZonaProp + "/casas-departamentos-ph-" + operation + "-capital-federal"

    var doc = Jsoup.connect(url + ".html")
      .userAgent("Mozilla")
      .get()

    val listProperties = new ListBuffer[Propiedad]()
    val numberOfPages = getNumberPagesZonaprop(doc)

    // 461 seconds for 277 page y 5537 properties, 1.66 sec per page
    for(i <- 1 to numberOfPages){
      doc = Jsoup.connect(url + "-pagina-" + i + ".html")
        .userAgent("Mozilla")
        .get()

      val dataList = parseJson(doc.getElementById("preloadedData").data())

      for(data <- dataList){
        val property = new Propiedad()

        property.id = (data \ "postingId").extract[String]
        property.url = urlZonaProp + (data \ "url").extract[String]

        //Set data of price (precio, moneda, expensas)
        val operationTypes = (data \ "priceOperationTypes").extract[JArray].arr
        for (operationType <- operationTypes) {
          if ((operationType \ "operationType" \ "name").extract[String].compareToIgnoreCase(operation.toString) == 0) {
            property.operacion = operation.toString.capitalize
            val price = (operationType \ "prices")(0)
            property.precio = (price \ "amount").extractOpt[Int].getOrElse(0)
            property.moneda = (price \ "currency").extractOpt[String].getOrElse("ARS")
          }
        }
        property.expensas = (data \ "expenses" \ "amount").extractOpt[Int].getOrElse(0)

        // Set data of features (superficie, ambientes, dormitorios, banios, cochera)
        val features = data \ "mainFeatures"
        val keys = features match {
          case JObject(fields) => fields.map { case (key, _) => key }
          case _ => List.empty[String]
        }

        for (key <- keys) {
          val value = ((features \ key) \ "value").extractOpt[String]

          key match {
            case "CFT100" => property.superficeTotal = value.getOrElse("0").toInt
            case "CFT101" => property.superficieCubierta = value.getOrElse("0").toInt
            case "CFT1" => property.ambientes = value.getOrElse("0").toInt
            case "CFT2" => property.dormitorios = value.getOrElse("0").toInt
            case "CFT3" => property.banios = value.getOrElse("0").toInt
            case "CFT7" => property.cochera = value.getOrElse("0").toInt
            case _ => //Rest of the keys
          }
        }

        // Set data of property type
        property.tipo = (data \ "realEstateType" \ "name").extract[String]

        // Set data of location (barrio, direccion, coordenadas)
        val postLocation = data \ "postingLocation"
        property.direccion = (postLocation \ "address" \ "name").extractOpt[String].getOrElse("")

        if ((postLocation \ "location" \ "label").extract[String] == "BARRIO") {
          property.barrio = (postLocation \ "location" \ "name").extract[String]
        } else {
          property.barrio = (postLocation \ "location" \ "parent" \ "name").extractOpt[String].getOrElse("")
        }

        // 'postingGeolocation': {'geolocation': {'latitude': -34.58013254297343, 'longitude': -58.39818611513063},
        val geolocation = postLocation \ "postingGeolocation" \ "geolocation"
        property.coordenadas = (geolocation \ "latitude").extractOpt[String].getOrElse("0") + "," +
          (geolocation \ "longitude").extractOpt[String].getOrElse("0")

        listProperties.append(property)
      }
    }
    println(listProperties.head)
    listProperties.toList
  }
}
