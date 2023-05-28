package example

import org.json4s._
import org.json4s.native.JsonMethods._
import org.jsoup._

import java.io.PrintWriter
object WebScrapper{
  def zonaprop(): Unit = {
    val operacion = "2" // Alquiler id = 2 Venta es 1

    val url = "https://www.zonaprop.com.ar"
    val doc1 = Jsoup.connect(url + "/casas-departamentos-ph-alquiler-capital-federal.html")
      .userAgent("Mozilla")
      .get()

    val listProperties = new scala.collection.mutable.ListBuffer[Propiedad]()
    val numberProperties = doc1.getElementsByTag("h1").first().text().replaceAll("[^0-9]", "")
    println(numberProperties)

    // TODO: iterate over all pages
    for(i <- 1 to 10){
      val doc = Jsoup.connect(url + "/casas-departamentos-ph-alquiler-capital-federal-pagina-" + i + ".html")
      .userAgent("Mozilla")
      .get()
      val s = doc.getElementById("preloadedData").data()


      val result = s.split("\\{\"listPostings\":")(1).split("\\,\"listCondominium\"")(0)

      implicit val formats = DefaultFormats

      val json = parse(result)

      val dataList = json.extract[JArray].arr


      for(data <- dataList){
        val property = new Propiedad()
        property.url = url + (data \ "url").extract[String]

        // 'priceOperationTypes': [
        // {'lowPricePercentage': None,
        //   'operationType': {'name': 'Venta', 'operationTypeId': '1'},
        //   'prices': [
        //      {'currencyId': '2',
        //     'amount': 3000000,
        //     'formattedAmount': '3.000.000',
        //     'currency': 'USD'}
        //     ]
        //   },
        //  {'lowPricePercentage': None,
        //   'operationType': {'name': 'Alquiler', 'operationTypeId': '2'},
        //   'prices': [{'currencyId': '2',
        //     'amount': 6900,
        //     'formattedAmount': '6.900',
        //     'currency': 'USD'}]}],
        val operationTypes = (data \ "priceOperationTypes").extract[JArray].arr
        for (operationType <- operationTypes) {
          if ((operationType \ "operationType" \ "operationTypeId").extract[String] == operacion) {
            property.operacion = (operationType \ "operationType" \ "name").extract[String]
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

    println(listProperties.length)
    println(listProperties)
  }
}
