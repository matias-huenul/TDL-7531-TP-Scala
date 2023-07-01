package scraper.etl

import scraper.etl.model.Property
import scraper.etl.utils.{Operation, Page}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.{Element, Document => ScalaDocument}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.jsoup._
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import me.tongfei.progressbar._

import java.util.Calendar
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
object WebScraper{
  private val URL_ARGENPROP="https://www.argenprop.com"
  private val URL_MELI="https://inmuebles.mercadolibre.com.ar/departamentos/alquiler/capital-federal/"
  val URL_ZONAPROP="https://www.zonaprop.com.ar"
  val USER_AGENT="Mozilla/5.0 (X11; Linux x86_64; rv:107.0) Gecko/20100101 Firefox/107.0"

  /**
   * Builds a progress bar
   * @param total Total number of pages
   * @param message Message to show in the progress bar
   * @return ProgressBar object for usage
   */
  private def progressBar(total:Int,message:String):ProgressBar={
    new ProgressBarBuilder()
      .setInitialMax(total)
      .setTaskName(message)
      .setUnit(" pages",1)
      .setStyle(ProgressBarStyle.ASCII)
      .build()
  }

  /**
   * Get all the numbers from a string
   *
   * @param s String: A string with the numbers asda sdwda
   * @return Int: The numbers of the string in a integer
   */
  private def toNumber(s:String):Int={
    val number=s.replaceAll("[^0-9]","")
    if(number.isEmpty)0 else number.toInt
  }

  /**
   * Get the number of pages of zonaprop.com.ar
   * @param doc: Document of the first page of zonaprop.com.ar/casas-departamentos-ph-_-capital-federal
   * @return Int: Number of pages
   */
  def getNumberPagesZonaprop(doc:Document):Int={
    val numberProperties=toNumber(doc.getElementsByTag("h1").first().text())
    (numberProperties/20)+1
  }

  private def getUrlsZonaprop(operation:Operation.Value):List[String]={
    val baseUrl=URL_ZONAPROP+"/casas-departamentos-ph-"+ Operation.toSpanishString(operation) + "-capital-federal"

    val doc = JsoupBrowser().get(baseUrl+".html")
    val  maxPages=(toNumber((doc>>texts("h1")).head)/20)+1
    val URLs = (1 to 5).map(i=> baseUrl + (if(i==1) "" else "-pagina-"+ i) +".html")
    URLs.toList
  }

  /**
   * Transform a string with json data to a list of json objects
   * @param stringData: String with json data
   * @return List[JValue]: List of json objects
   */
  private def parseJson(stringData:String):List[JValue]={
    implicit val formats:DefaultFormats.type=DefaultFormats
    val result=stringData.split("\\{\"listPostings\":")(1).split("\\,\"listCondominium\"")(0)
    val json=parse(result)
    json.extract[JArray].arr
  }

  /**
   * Extract the data of the properties from the json
   * @param data: Json with the data of the properties
   * @param operation: Operation of the properties
   * @return Property: Property with the data of the json
   */
  private def readPropertyZonaprop(data:JValue,operation:Operation.Value):Property={
    implicit val formats:DefaultFormats.type=DefaultFormats
    val property=new Property(page=Page.ZONAPROP)

    property.url=URL_ZONAPROP+(data \ "url").extract[String]

    //Set data of price (precio, moneda, expensas)
    val operationTypes=(data \ "priceOperationTypes").extract[JArray].arr
    for(operationType<-operationTypes){
      if(Operation.fromString((operationType \ "operationType" \ "name").extract[String])==operation){
        val price=(operationType \ "prices")(0)
        property.price=(price \ "amount").extractOpt[Int].getOrElse(0)
        property.setCurrency((price \ "currency").extractOpt[String].getOrElse("ARS"))
      }
    }
    property.expenses=(data \ "expenses" \ "amount").extractOpt[Int].getOrElse(0)

    // Set data of features (superficie, ambientes, dormitorios, banios, cochera)
    val features=data \ "mainFeatures"
    val keys=features match{
      case  JObject(fields)=>fields.map{case (key,_)=>key}
      case  _=>List.empty[String]
    }

    for(key<-keys){
      val value=toNumber(((features \ key) \ "value").extractOpt[String].getOrElse("0"))

      key match{
        case "CFT100"=>property.totalArea=value
        case "CFT101"=>property.coveredArea=value
        case "CFT1"=>property.rooms=value
        case "CFT2"=>property.bedrooms=value
        case "CFT3"=>property.bathrooms=value
        case "CFT7"=>property.garage=value
        case  _=> //Rest of the keys
      }
    }

    // Set data of property type
    property.setPropertyType((data \ "realEstateType" \ "name").extract[String])

    // Set data of location (barrio, direccion, coordenadas)
    val postLocation=data \ "postingLocation"
    property.address=(postLocation \ "address" \ "name").extractOpt[String].getOrElse("")

    if((postLocation \ "location" \ "label").extract[String]=="BARRIO"){
      property.neighborhood=(postLocation \ "location" \ "name").extract[String]
    }else{
      property.neighborhood=(postLocation \ "location" \ "parent" \ "name").extractOpt[String].getOrElse("")
    }

    property
  }

  /**
   * Scrapes zonaprop.com.ar and returns a list of properties
   *
   * @param operation: Operation to scrape (Alquiler, Venta)
   * @return List[Propiedad]: List of properties
   */
  def zonaprop(operation:Operation.Value=Operation.RENT):Set[Property]={
    val currentTime=Calendar.getInstance.getTime.getTime

    val urls = getUrlsZonaprop(operation)
    val browser=JsoupBrowser()

    val pb:ProgressBar=progressBar(urls.size,"Scraping ZonaProp "+operation.toString)

    val listProperties = urls.flatMap(url => {
      try {
        pb.step()
        val doc = browser.get(url)
        val dataList = parseJson((doc >> element("#preloadedData")).innerHtml)
        dataList.map(data => readPropertyZonaprop(data, operation)).toList
      } catch {
        case e: Exception =>
          println(s"Error reading url. Error message: ${e.getMessage}")
          None
      }
    }).toSet
    pb.close()

    println("Scraping ZonaProp "+operation+" finished in "+(Calendar.getInstance.getTime.getTime-currentTime)/1000+" seconds reading a total of "+listProperties.size+" properties")
    listProperties
  }

  /**
   * Get the url of the next page of argenprop.com
   * @param doc: Document to scrape
   * @return String: Url of the next page
   */
  private def getNextPageArgenprop(doc:ScalaDocument):String={
    val nextPage=doc>>elementList(".pagination__page-next>a")
    if(nextPage.isEmpty)""else(nextPage.head>?>attr("href")).getOrElse("")
  }

  private def getUrlsArgenprop(operation: Operation.Value): IndexedSeq[String] = {
    val url = URL_ARGENPROP + "/departamento-y-casa-" + Operation.toSpanishString(operation) + "-localidad-capital-federal"

    val doc = JsoupBrowser().get(url)
    val href = doc >> elementList(".pagination__page>a")
    val maxPages = toNumber(href(href.length - 2).text).min(99)
    val URLs = (1 to maxPages).map(i => url + (if (i != 1) s"-pagina-$i"))
    URLs
  }

  /**
   * Scrapes argenprop.com and returns a list of properties
   * @param element: Element to scrape
   * @return List[Propiedad]: List of properties
   */
  private def scrapePropertyArgenprop(element:Element):Property={
    val property=new Property(page=Page.ARGENPROP)

    property.url=URL_ARGENPROP+(element>>"a").head.attr("href")
    property.setPropertyType(property.url.split("/")(1).split("-")(0))

    val cardPrice=(element>>"p.card__price").head
    if((cardPrice>?>".card__noprice").head.isEmpty){
      property.setCurrency((cardPrice>>elementList(".card__currency")>?>text).head.getOrElse("ARS"))
      property.price=toNumber(cardPrice.ownText)
    }

    property.address=(element>>".card__address").head.text
    property.neighborhood=(element>>".card__title--primary").last.text.split(",")(0)
    property.expenses=toNumber((element>?>text(".card__expenses")).getOrElse("0"))

    val features=element>>".card__main-features>li"

    for(feature<-features){
      val value=toNumber(feature.text)

      (feature>>"i").head.attr("class")match{
        case "icono-superficie_total"=>property.totalArea=value
        case "icono-superficie_cubierta"=>property.coveredArea=value
        case "icono-cantidad_ambientes"=>property.rooms=value
        case "icono-cantidad_dormitorios"=>property.bedrooms=value
        case "icono-cantidad_banos"=>property.bathrooms=value
        case "icono-ambiente_cochera"=>property.garage=value
        case  _=> //Rest of the features
      }
    }
    property
  }

  /**
   * Scrapes argenprop.com and returns a list of properties
   * argenprop has a limit of 99 pages before it returns an error
   * @param operation: Operation to scrape (Alquiler, Venta)
   * @return List[Propiedad]: List of properties
   */
  def argenprop(operation:Operation.Value=Operation.RENT):Set[Property]={
    val currentTime=Calendar.getInstance.getTime.getTime
    val browser=JsoupBrowser()

    val urls = getUrlsArgenprop(operation)
    val pb:ProgressBar=progressBar(urls.size,"Scraping Argenprop "+operation.toString)

    val props = urls.flatMap(url =>{
      try{
        pb.step()
        val doc=browser.get(url)
        val elements = doc>>"div.listing__item"
        elements.map( e => scrapePropertyArgenprop(e)).toList
      }catch {
        case e: Exception =>
          println(s"Error reading url. Error message: ${e.getMessage}")
          None
      }
    }).toSet

    pb.close()
    println("Scraping Argenprop finished in "+(Calendar.getInstance.getTime.getTime-currentTime)/1000+" seconds reading a total of "+props.size+" properties")
    props
  }
}
