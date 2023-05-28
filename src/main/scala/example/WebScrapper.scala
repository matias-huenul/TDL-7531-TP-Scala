package example

import net.ruippeixotog.scalascraper.browser._

import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

import org.json4s._
import org.json4s.native.JsonMethods._


import org.jsoup._
object WebScrapper{
  def zonaprop(): Unit = {
    val url = "https://www.zonaprop.com.ar"
    // `extract` is the same as `>>`
    //doc.extract("title")

    // `tryExtract` is the same as `>?>`
    //doc.tryExtract.element("#optional")

    val browser = new JsoupBrowser(userAgent = "Mozilla/5.0")
    //browser.get(url + "/casas-departamentos-ph-alquiler-capital-federal.html")


    val doc = Jsoup.connect(url + "/casas-departamentos-ph-alquiler-capital-federal.html")
      .userAgent("Mozilla")
      .get()

    val s = doc.getElementById("preloadedData").data()

    val result = s.split("\\{\"listPostings\":")(1)
      .split("\\,\"listCondominium\"")(0)

    //Put result into txt file
    //val pw = new java.io.PrintWriter(new java.io.File("result.txt" ))
    //pw.write(result)
    //pw.close
    implicit val formats = DefaultFormats

    val json = parse(result)

    val dataList = json.extract[JArray]
    val examples = dataList(0)


    println("Id " + (examples \ "url").extract[String])


    println("Price " + (examples \ "priceOperationTypes"))
    // 'priceOperationTypes': [{'lowPricePercentage': None,
    //   'operationType': {'name': 'Alquiler', 'operationTypeId': '2'},
    //   'prices': [{'currencyId': '2',
    //     'amount': 4200,
    //     'formattedAmount': '4.200',
    //     'currency': 'USD'}]}],

    //println("exp " + (examples \ "expenses"))

    //println("Main features " + (examples \ "mainFeatures"))
    //'mainFeatures': {'1000027': {'featureId': '1000027',
    //   'label': 'Luminoso',
    //   'measure': None,
    //   'value': 'Muy luminoso',
    //   'icon': None,
    //   'featureCategoryId': '4'},
    //  'CFT100': {'featureId': 'CFT100',
    //   'label': 'Superficie total',
    //   'measure': 'm²',
    //   'value': '418',
    //   'icon': None,
    //   'featureCategoryId': 'CFC2'},
    //  'CFT101': {'featureId': 'CFT101',
    //   'label': 'Superficie cubierta',
    //   'measure': 'm²',
    //   'value': '378',
    //   'icon': None,
    //   'featureCategoryId': 'CFC2'},
    //  'CFT2': {'featureId': 'CFT2',
    //   'label': 'Dormitorios',
    //   'measure': None,
    //   'value': '4',
    //   'icon': None,
    //   'featureCategoryId': 'CFC1'},
    //  'CFT3': {'featureId': 'CFT3',
    //   'label': 'Baños',
    //   'measure': None,
    //   'value': '3',
    //   'icon': None,
    //   'featureCategoryId': 'CFC1'},
    //  'CFT7': {'featureId': 'CFT7',
    //   'label': 'Cocheras',
    //   'measure': None,
    //   'value': '2',
    //   'icon': None,
    //   'featureCategoryId': 'CFC1'}},

    //println("general fatures " + (examples \ "generalFeatures"))
    //'generalFeatures': {'Características generales': {'1000018': {'featureId': '1000018',
    //    'label': 'Cobertura cochera',
    //    'measure': None,
    //    'value': 'Cubierta',
    //    'icon': None,
    //    'featureCategoryId': '4'},
    //   '1000025': {'featureId': '1000025',
    //    'label': 'Frente del terreno (mts)',
    //    'measure': None,
    //    'value': '8',
    //    'icon': None,
    //    'featureCategoryId': '4'},
    //   '1000078': {'featureId': '1000078',
    //    'label': 'Parrilla',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '4'}},
    //  'Servicios': {'2000148': {'featureId': '2000148',
    //    'label': 'Ascensor',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '1'}},
    //  'Ambientes': {'1000100': {'featureId': '1000100',
    //    'label': 'Balcón',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000102': {'featureId': '1000102',
    //    'label': 'Cocina',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000103': {'featureId': '1000103',
    //    'label': 'Comedor',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000105': {'featureId': '1000105',
    //    'label': 'Dependencia servicio',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000106': {'featureId': '1000106',
    //    'label': 'Dormitorio en suite',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000110': {'featureId': '1000110',
    //    'label': 'Lavadero',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000111': {'featureId': '1000111',
    //    'label': 'Living',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000112': {'featureId': '1000112',
    //    'label': 'Living comedor',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000114': {'featureId': '1000114',
    //    'label': 'Patio',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000116': {'featureId': '1000116',
    //    'label': 'Terraza',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000117': {'featureId': '1000117',
    //    'label': 'Toilette',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'},
    //   '1000118': {'featureId': '1000118',
    //    'label': 'Vestidor',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '2'}},
    //  'Características': {'1000085': {'featureId': '1000085',
    //    'label': 'Quincho',
    //    'measure': None,
    //    'value': None,
    //    'icon': None,
    //    'featureCategoryId': '5'}}},

    println("tipo " + (examples \ "realEstateType"))
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


    val title = doc >> element("h1")
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


  }
}
