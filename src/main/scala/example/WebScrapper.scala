package example

import org.json4s._
import org.json4s.native.JsonMethods._
import org.jsoup._

import java.io.PrintWriter
object WebScrapper{
  def zonaprop(): Unit = {
    val url = "https://www.zonaprop.com.ar"

    val doc = Jsoup.connect(url + "/casas-departamentos-ph-alquiler-capital-federal.html")
      .userAgent("Mozilla")
      .get()

    val s = doc.getElementById("preloadedData").data()

    val result = s.split("\\{\"listPostings\":")(1)
      .split("\\,\"listCondominium\"")(0)

    implicit val formats = DefaultFormats

    val json = parse(result)

    val dataList = json.extract[JArray].arr
    val examples = dataList(0)


    val property = new Propiedad()
    println("Id " + (examples \ "url").extract[String])
    property.url = (examples \ "url").extract[String]


    println("Price " + (examples \ "priceOperationTypes")(0) \ "operationType")
    // 'priceOperationTypes': [
    //  {'lowPricePercentage': None,
    //   'operationType': {'name': 'Alquiler', 'operationTypeId': '2'},
    //   'prices': [
    //     {'currencyId': '2',
    //     'amount': 4200,
    //     'formattedAmount': '4.200',
    //     'currency': 'USD'}
    //     ]
    //   }
    // ],

    val features = (examples \ "mainFeatures")
    val keys = features match {
      case JObject(fields) => fields.map { case (key, _) => key }
      case _ => List.empty[String]
    }

    for(key <- keys){
      println("key " + key)
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

    println("tipo " + (examples \ "realEstateType" \ "name"))
    //'realEstateType': {'name': 'Casa', 'realEstateTypeId': '1'},

    //println("location " + (examples \ "postingLocation"))
    //'postingLocation': {'address': {'name': 'Juez Estrada al 2700',
    //   'visibility': 'EXACT'},
    //  'location': {'locationId': 'V1-D-1003684',
    //   'name': 'Palermo Chico',
    //   'label': 'ZONA',
    //   'depth': 3,
    //   'parent': {'locationId': 'V1-C-1003694',
    //    'name': 'Palermo',
    //    'label': 'BARRIO',
    //    'depth': 2,
    //    'parent': {'locationId': 'V1-B-6',
    //     'name': 'Capital Federal',
    //     'label': 'CIUDAD',
    //     'depth': 1,
    //     'parent': {'locationId': 'V1-A-1',
    //      'name': 'Argentina',
    //      'label': 'PAIS',
    //      'depth': 0,
    //      'parent': None,
    //      'acronym': None},
    //     'acronym': 'CABA'},
    //    'acronym': None},
    //   'acronym': None},

    //println("geolocation " + (examples \ "postingGeolocation"))
    // 'postingGeolocation': {'geolocation': {'latitude': -34.58013254297343, 'longitude': -58.39818611513063},


    /*val title = doc >> element("h1")
    val listings = doc >> elementList(".sc-1tt2vbg-3")

    for(listing <- listings) {
      val property = new Propiedad()

      // Set price and currency
      val price = listing >> text("[data-qa=POSTING_CARD_PRICE]")
      if (price.contains("USD")) {
        property.moneda = 2
      }

      try{
        property.precio = (price.replaceAll("[^0-9]","")).toInt
      }catch {
        case e: Exception => property.precio = -1
      }

      // Get url and set it
      val urlListing = url + (listing >> element(".sc-i1odl-0")).attr("data-to-posting") // Se puede hacer con >> attr("data-to-posting")
      property.url = urlListing

      val expensas = (listing >?> text("[data-qa=expensas]"))
      if(expensas.isDefined) {
        property.expensas = (expensas.get.replaceAll("[^0-9]","")).toInt
      }else {
        property.expensas = 0
      }


      val docListing = browser.get(urlListing)
      val iconFeatures = docListing >> elementList(".icon-feature")

      for(iconFeature <- iconFeatures) {
        val featureType = iconFeature >> element("i") >> attr("class")

        featureType match {
          case "icon-stotal" => property.superficeTotal = (iconFeature.text).split(" ")(0).toInt
          case "icon-scubierta" => property.superficieCubierta = (iconFeature.text).split(" ")(0).toInt
          case "icon-ambiente" => property.ambientes = (iconFeature.text).split(" ")(0).toInt
          case "icon-dormitorio" => property.dormitorios = (iconFeature.text).split(" ")(0).toInt
          case "icon-bano" => property.banios = (iconFeature.text).split(" ")(0).toInt
          case "icon-cochera" => property.cochera = (iconFeature.text).split(" ")(0).toInt
          case _ => // Do nothing when no match is found
        }
      }

      val address = docListing >> element(".title-location")
      property.direccion = address.text
      property.barrio = ((address >> element("span")).text).split(",")(0)
      println(property)


    }
    */

  }
}
