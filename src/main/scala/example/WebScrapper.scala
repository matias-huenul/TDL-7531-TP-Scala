package example

import example.utils.{Currency, Operation}

import org.json4s._
import org.json4s.native.JsonMethods._

import org.jsoup._
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.{Document => ScalaDocument}
import net.ruippeixotog.scalascraper.model.Element

import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupElement
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._

import scala.collection.mutable.ListBuffer
object WebScrapper{
  val URL_ARGENPROP = "https://www.argenprop.com"

  /**
   * Get all the numbers from a string
   * @param s String: A string with the numbers
   * @return Int: The numbers of the string in a integer
   */
  private def toNumber(s: String): Int = {
    val number = s.replaceAll("[^0-9]", "")
    if (number.isEmpty) 0 else number.toInt
  }

  /**
   * Get the number of pages of zonaprop.com.ar
   * @param doc: Document of the first page of zonaprop.com.ar/casas-departamentos-ph-_-capital-federal
   * @return Int: Number of pages
   */
  def getNumberPagesZonaprop(doc: Document): Int = {
    val numberProperties = toNumber(doc.getElementsByTag("h1").first().text())
    (numberProperties/20)+1 // 20 propiedades por pagina
  }

  /**
   * Transform a string with json data to a list of json objects
   * @param stringData: String with json data
   * @return List[JValue]: List of json objects
  */
  def parseJson(stringData: String): List[JValue] ={
    implicit val formats: DefaultFormats.type = DefaultFormats
    val result = stringData.split("\\{\"listPostings\":")(1).split("\\,\"listCondominium\"")(0)
    val json = parse(result)
    json.extract[JArray].arr
  }

  /**
   * Scrapes zonaprop.com.ar and returns a list of properties
   * @param operation: Operation to scrape (Alquiler, Venta)
   * @return List[Propiedad]: List of properties
  */
  def zonaprop(operation:Operation.Value = Operation.ALQUILER): List[Propiedad] = {
    //Todo: add log
    implicit val formats: DefaultFormats.type = DefaultFormats

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
            property.operacion = operation
            val price = (operationType \ "prices")(0)
            property.precio = (price \ "amount").extractOpt[Int].getOrElse(0)
            property.moneda = Currency.fromString((price \ "currency").extractOpt[String].getOrElse("ARS"))
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

  private def getNextPageArgenprop(doc: ScalaDocument): String = {
    val nextPage = doc >> elementList(".pagination__page-next>a")
    if(nextPage.isEmpty) "" else (nextPage.head >?> attr("href")).getOrElse("")
  }

  /**
   * Scrapes argenprop.com and returns a list of properties
   * @param element: Element to scrape
   * @param operation: Operation to scrape (Alquiler, Venta)
   * @return List[Propiedad]: List of properties
  */
  private def scrapePropertyArgenprop(element: Element, operation: Operation.Value): Propiedad ={
    val property = new Propiedad()

    property.url = URL_ARGENPROP + (element >> "a").head.attr("href")
    property.tipo = property.url.split("/")(1).split("-")(0)

    val cardPrice = (element >> "p.card__price").head

    if ((cardPrice >?> ".card__noprice").head.isEmpty) {
      property.operacion = operation
      property.moneda = Currency.fromString((cardPrice >> elementList(".card__currency") >?> text).head.getOrElse("ARS"))
      property.precio = toNumber(cardPrice.ownText)
    }

    property.direccion = (element >> ".card__address").head.text
    property.barrio = (element >> ".card__title--primary").last.text.split(",")(0)
    property.expensas = toNumber((element >?> text(".card__expenses")).getOrElse("0"))

    val features = element >> ".card__main-features>li"

    for (feature <- features) {
      val value = toNumber(feature.text)

      (feature >> "i").head.attr("class") match {
        case "icono-superficie_total" => property.superficeTotal = value
        case "icono-superficie_cubierta" => property.superficieCubierta = value
        case "icono-cantidad_ambientes" => property.ambientes = value
        case "icono-cantidad_dormitorios" => property.dormitorios = value
        case "icono-cantidad_banos" => property.banios = value
        case "icono-ambiente_cochera" => property.cochera = value
        case _ => //Rest of the features
      }
    }
    property
  }

  /**
   * Scrapes argenprop.com and returns a list of properties
   * argenprop has a limit of 100 pages before it returns an error
   * @param operation: Operation to scrape (Alquiler, Venta)
   * @return List[Propiedad]: List of properties
  */
  def argenprop(operation:Operation.Value = Operation.ALQUILER): List[Propiedad] = {
    val listProperties = new ListBuffer[Propiedad]()
    val browser = JsoupBrowser()
    var url = URL_ARGENPROP + "/departamento-y-casa-y-ph-" + operation + "-localidad-capital-federal"

    try{
      do {
        val doc = browser.get(url)
        val elements = doc >> "div.listing__item"

        for (element <- elements) {
          listProperties.append(scrapePropertyArgenprop(element, operation))
        }

        url = URL_ARGENPROP + getNextPageArgenprop(doc)
        if(toNumber(url)%50 == 0) Thread.sleep(10000) //Sleep 10 seconds every 50 pages
      } while (url != URL_ARGENPROP)
    }catch {
      case e: Exception => println("Error scraping argenprop: " + e.getMessage)
    }
    listProperties.toList
  }

  /**
   * Scrapes mercadolibre.com.ar and returns of links of publications
   * @return Elements: List of <a> with links of publications
  */
  def getPublicationLinksMELI(): Elements = {
    val links: Elements = new Elements()

    var maxPages = 0
    var pageNumber = 0

    do {
      val indexNumber = pageNumber * 48 + 1
      val indexString = if (pageNumber != 0) "_Desde_" + indexNumber + "_NoIndex_True" else "_NoIndex_True"
      val doc = Jsoup.connect("https://inmuebles.mercadolibre.com.ar/departamentos/alquiler/capital-federal/" + indexString)
        .userAgent("Mozilla")
        .get()

      links.addAll(doc.select("div.ui-search-item__group--title>a.ui-search-link"))

      maxPages = if (maxPages == 0) toNumber(doc.select("li.andes-pagination__page-count").last().text()) else maxPages
      pageNumber += 1
    } while (pageNumber < maxPages)

    links
  }

  def scrapePropertyMELI(url: String, session:Connection): Propiedad ={
    val doc = session.newRequest().url(url).get()

    val property = new Propiedad()
    property.url = url
    property.precio = doc.select("span.andes-money-amount>.andes-visually-hidden").first().text().replaceAll("[^0-9]", "").toInt
    property.moneda = Currency.fromString(doc.getElementsByClass("andes-money-amount__currency-symbol").first().text.replaceAll("[1-9]", "").strip())

    val locationData = doc.select(".ui-vip-location").first()

    try {
      val locationString = locationData.text()
      val split = locationString.split(",")
      property.direccion = split(0).strip()
      property.barrio = split(1).strip()
    } catch {
      case e: Exception => property.barrio = doc.select(".andes-breadcrumb__item").last().text()
    }

    val tableRows = doc.select("tbody.andes-table__body").first().select("tr")

    for(i <- 1 until tableRows.size()){
      val row = tableRows.get(i)

      row.getElementsByTag("th").first().text().toUpperCase match {
        case "SUPERFICIE TOTAL" => property.superficeTotal = row.getElementsByTag("td").first().text().replaceAll("[^0-9]", "").toInt
        case "SUPERFICIE CUBIERTA" => property.superficieCubierta = row.getElementsByTag("td").first().text().replaceAll("[^0-9]", "").toInt
        case "AMBIENTES" => property.ambientes = row.getElementsByTag("td").first().text().toInt
        case "DORMITORIOS" => property.dormitorios = row.getElementsByTag("td").first().text().toInt
        case "BAÑOS" => property.banios = row.getElementsByTag("td").first().text().toInt
        case "COCHERAS" => property.cochera = row.getElementsByTag("td").first().text().toInt
        case "EXPENSAS" => property.expensas = row.getElementsByTag("td").first().text().replaceAll("[^0-9]", "").toInt
        case _ => //Rest of the keys
      }
    }

    property
  }

  /**
   * Scrapes mercadolibre.com.ar for properties in rent and returns a list of properties
   * It is slow and prone to errors
   * @return List[Propiedad]: List of properties
  */
  def mercadolibre(): List[Propiedad] = {
    val listProperties = new ListBuffer[Propiedad]()
    val links = getPublicationLinksMELI()
    val session = Jsoup.newSession().userAgent("Mozilla").timeout(10000)
    for(i <- 0 until links.size()){
      try {
        val property = scrapePropertyMELI(links.get(i).attr("href").split("#")(0),session)
        listProperties.append(property)
      } catch {
        case e: Exception => println("Error scraping property: " + links.get(i).attr("href").split("#")(0))
      }
    }

    listProperties.toList
  }
}
