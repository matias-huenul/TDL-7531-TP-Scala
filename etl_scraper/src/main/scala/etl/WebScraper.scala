package etl

import etl.model.Property
import etl.utils.{Operation, Page}
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
object WebScraper{
  private val URL_ARGENPROP="https://www.argenprop.com"
  private val URL_MELI="https://inmuebles.mercadolibre.com.ar/departamentos/alquiler/capital-federal/"
  val URL_ZONAPROP="https://www.zonaprop.com.ar"
  val USER_AGENT="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3\""

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
  def zonaprop(operation:Operation.Value=Operation.RENT):List[Property]={
    val currentTime=Calendar.getInstance.getTime.getTime
    val url=URL_ZONAPROP+"/casas-departamentos-ph-"+Operation.toSpanishString(operation)+"-capital-federal"
    val session=Jsoup.newSession().referrer("https://www.google.com").userAgent(USER_AGENT)


    val listProperties=new ListBuffer[Property]()
    var maxPages=1
    var pb:ProgressBar=null

    var i=1
    do{
      try{
        val doc=session.newRequest().url(url+"-pagina-"+i+".html").get()
        val dataList=parseJson(doc.getElementById("preloadedData").data())

        if(pb==null){
          maxPages=getNumberPagesZonaprop(doc)
          pb=progressBar(maxPages,"Scraping ZonaProp "+operation.toString)
        }

        for(data<-dataList){
          val property=readPropertyZonaprop(data,operation)
          if(!listProperties.exists(_.url == property.url)) listProperties+=property
        }
      }catch{
        case  e:Exception=>println("Error scraping ZonaProp page "+i+": "+e.getMessage)
      }
      if(pb!=null) pb.step()
      if(i%50==0)Thread.sleep(5000)
      i+=1
    }while(i<=maxPages)

    pb.close()
    println("Scraping ZonaProp "+operation+" finished in "+(Calendar.getInstance.getTime.getTime-currentTime)/1000+" seconds reading a total of "+listProperties.size+" properties")
    listProperties.toList
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
  def argenprop(operation:Operation.Value=Operation.RENT):List[Property]={
    val currentTime=Calendar.getInstance.getTime.getTime
    val listProperties=new ListBuffer[Property]()
    val browser=JsoupBrowser()
    var url=URL_ARGENPROP+"/departamento-y-casa-"+Operation.toSpanishString(operation)+"-localidad-capital-federal"
    var pb:ProgressBar=null

    try{
      do{
        val doc=browser.get(url)
        if(pb==null){
          val href=doc>>elementList(".pagination__page>a")
          val lastPage=toNumber(href(href.length-2).text)

          pb=progressBar(lastPage.min(99),"Scraping Argenprop "+operation.toString)
        }
        val elements=doc>>"div.listing__item"

        for(element<-elements){
          val prop = scrapePropertyArgenprop(element)
          if(!listProperties.exists(_.url==prop.url)) listProperties += prop
        }

        url=URL_ARGENPROP+getNextPageArgenprop(doc)
        pb.step()
        if(toNumber(url)%25==0)Thread.sleep(5000) //Sleep 10 seconds every 25 pages
      }while(url!=URL_ARGENPROP&&toNumber(url)< 100)
    }catch{
      case  e:Exception=>println("Error scraping Argenprop: "+e.getMessage)
    }
    pb.close()
    println("Scraping Argenprop finished in "+(Calendar.getInstance.getTime.getTime-currentTime)/1000+" seconds reading a total of "+listProperties.size+" properties")
    listProperties.toList
  }

  /**
   * Scrapes mercadolibre.com.ar and returns of links of publications
   * @return Elements: List of links of publications
   */
  private def getPublicationLinksMELI:List[String]={
    val elements:Elements=new Elements()

    var maxPages=0
    var pageNumber=0
    var pb:ProgressBar=null

    do{
      val indexNumber=pageNumber*48+1
      val indexString=if(pageNumber!=0)"_Desde_"+indexNumber+"_NoIndex_True" else "_NoIndex_True"
      val doc=Jsoup.connect(URL_MELI+indexString)
        .userAgent(USER_AGENT)
        .get()

      elements.addAll(doc.select("div.ui-search-item__group--title>a.ui-search-link"))

      if(maxPages==0){
        maxPages=toNumber(doc.select("li.andes-pagination__page-count").last().text())
        pb=new ProgressBarBuilder()
          .setInitialMax(maxPages)
          .setTaskName("Getting properties links MELI")
          .setUnit(" pages",1)
          .setStyle(ProgressBarStyle.ASCII)
          .build()
      }
      pageNumber+=1
      pb.step()
    }while(pageNumber<maxPages)

    val links=new ListBuffer[String]()
    for(i <- 0 until elements.size()){
      links.append(elements.get(i).attr("href").split("#")(0))
    }
    links.toList
  }

  private def scrapePropertyMELI(url:String,session:Connection):Property={
    val doc=session.newRequest().url(url).get()
    val property=new Property(page=Page.MELI)

    property.url=url
    val priceTag=doc.select("span.andes-money-amount>.andes-visually-hidden").first.text
    property.price=toNumber(priceTag)
    property.setCurrency(priceTag.replaceAll("[1-9]","").strip)

    try{
      val locationString=doc.select(".ui-vip-location").first.text
      val split=locationString.split(",")
      property.address=split(0).strip.replace("Ubicación","")
      property.neighborhood=split(1).strip
    }catch{
      case  _:Exception=>property.neighborhood=doc.select(".andes-breadcrumb__item").last.text
    }

    val tableRows=doc.select("tr")

    for(i <- 0 until tableRows.size()){
      val row=tableRows.get(i)
      val value=toNumber(row.getElementsByTag("td").first().text())

      row.getElementsByTag("th").first().text().toUpperCase match{
        case "SUPERFICIE TOTAL"=>property.totalArea=value
        case "SUPERFICIE CUBIERTA"=>property.coveredArea=value
        case "AMBIENTES"=>property.rooms=value
        case "DORMITORIOS"=>property.bedrooms=value
        case "BAÑOS"=>property.bathrooms=value
        case "COCHERAS"=>property.garage=value
        case "EXPENSAS"=>property.expenses=value
        case  _=> //Rest of the keys
      }
    }

    property
  }

  /**
   * Scrapes mercadolibre.com.ar for properties in rent and returns a list of properties
   * It is slow but gets all rent properties
   * @return List[Propiedad]: List of properties
   */
  def mercadolibre():List[Property]={
    val currentTime=Calendar.getInstance.getTime.getTime
    val listProperties=new ListBuffer[Property]()
    val links=getPublicationLinksMELI
    val session=Jsoup.newSession().userAgent("Mozilla")
    val pb=new ProgressBarBuilder()
      .setInitialMax(links.size)
      .setTaskName("Reading properties from MELI")
      .setUnit(" properties",1)
      .setStyle(ProgressBarStyle.ASCII)
      .build()
    for(link<-links){
      try{
        val property=scrapePropertyMELI(link,session)
        listProperties.append(property)
      }catch{
        case  e:Exception=>println("Error scraping MELI: "+e.getMessage)
      }
      if(links.indexOf(link)%50==0)Thread.sleep(5000) //Sleep 5 seconds every 50 pages
      pb.step()
    }
    pb.close()
    println("Scraping MELI finished in "+(Calendar.getInstance.getTime.getTime-currentTime)/1000+" seconds reading a total of "+listProperties.size+" properties")
    listProperties.toList
  }
}
