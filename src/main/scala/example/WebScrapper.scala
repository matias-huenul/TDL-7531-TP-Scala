package example

import org.json4s._
import org.json4s.native.JsonMethods._
import org.jsoup._

object WebScrapper{

  /*
   * Operacion:
   *    1 -> Venta
   *    2 -> Alquiler
  */
  def zonaprop(operacion:String = "2"): List[Propiedad] = {
    //Todo: add loggin
    val operationStr = if (operacion == "1") "venta" else "alquiler"
    val urlZonaProp = "https://www.zonaprop.com.ar"
    val url = urlZonaProp + "/casas-departamentos-ph-" + operationStr + "-capital-federal"

    var doc = Jsoup.connect(url + ".html")
      .userAgent("Mozilla")
      .get()

    val listProperties = new scala.collection.mutable.ListBuffer[Propiedad]()
    val numberProperties = doc.getElementsByTag("h1").first().text().replaceAll("[^0-9]", "").toInt
    val numberOfPages = (numberProperties/20)+1 // 20 propiedades por pagina

    // 461 seconds for 277 page y 5537 properties, 1.66 sec per page
    for(i <- 1 to numberOfPages){
      doc = Jsoup.connect(url + "-pagina-" + i + ".html")
        .userAgent("Mozilla")
        .get()

      // Get json data from script
      val s = doc.getElementById("preloadedData").data()
      val result = s.split("\\{\"listPostings\":")(1).split("\\,\"listCondominium\"")(0)

      implicit val formats = DefaultFormats

      val json = parse(result)
      val dataList = json.extract[JArray].arr

      for(data <- dataList){
        val property = new Propiedad()
        property.id = (data \ "postingId").extract[String]
        property.url = urlZonaProp + (data \ "url").extract[String]

        //
        val operationTypes = (data \ "priceOperationTypes").extract[JArray].arr
        for (operationType <- operationTypes) {
          if ((operationType \ "operationType" \ "operationTypeId").extract[String] == operacion) {
            property.operacion = operationStr.capitalize
            val prices = (operationType \ "prices")(0)
            property.precio = (prices \ "amount").extractOpt[Int].getOrElse(0)
            property.moneda = (prices \ "currency").extractOpt[String].getOrElse("ARS")
          }
        }

        property.expensas = (data \ "expenses" \ "amount").extractOpt[Int].getOrElse(0)

        val features = (data \ "mainFeatures")
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
            case _ => //Resto de keys
          }
        }

        property.tipo = (data \ "realEstateType" \ "name").extract[String]

        val postLocation = (data \ "postingLocation")
        property.direccion = (postLocation \ "address" \ "name").extractOpt[String].getOrElse("")

        if ((postLocation \ "location" \ "label").extract[String] == "BARRIO") {
          property.barrio = (postLocation \ "location" \ "name").extract[String]
        } else {
          property.barrio = (postLocation \ "location" \ "parent" \ "name").extractOpt[String].getOrElse("")
        }


        // 'postingGeolocation': {'geolocation': {'latitude': -34.58013254297343, 'longitude': -58.39818611513063},
        val geolocation = (postLocation \ "postingGeolocation" \ "geolocation")
        property.coordenadas = (geolocation \ "latitude").extractOpt[String].getOrElse("0") + "," +
          (geolocation \ "longitude").extractOpt[String].getOrElse("0")

        listProperties.append(property)
      }
    }

    listProperties.toList
  }
}
