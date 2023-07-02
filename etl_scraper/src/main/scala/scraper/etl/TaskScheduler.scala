package scraper.etl

import scraper.etl.model.Property
import scraper.etl.utils.{Operation, Page, DatabaseManager => DB}

import java.util.{Timer, TimerTask}

object TaskScheduler{
  private def updateMeli(operation:Operation.Value=Operation.RENT): Unit = {
    val rentProperties: List[Property] = MeliAPI.getRentPropertiesCABA
    val startTime=System.currentTimeMillis()

    try {
      DB.deletePropertiesWithPage(operation, Page.MELI)
      DB.insertProperties(rentProperties.toSet, operation)
    } catch {
      case e: Exception => println("Error updating db " + e.printStackTrace())
    }
    val timestamp = (System.currentTimeMillis() - startTime) / 1000
    println(s"Database properties_${operation.toString.toLowerCase} updated in $timestamp seconds for Mercadolibre API")
  }

  /**
   * Update database with new properties
   * @param operation: Oparation to update
   * @param page: Page being scraped
   */
  private def updateDB(operation:Operation.Value=Operation.RENT, page:Page.Value)={
    var prop=Set[Property]()
    page match{
      case Page.ZONAPROP=>prop=WebScraper.zonaprop(operation)
      case Page.ARGENPROP=>prop=WebScraper.scrapeArgenprop(operation)
      case _=>println("Page not recognized")
    }
    val startTime=System.currentTimeMillis()

    try{
      DB.deletePropertiesWithPage(operation,page)
      DB.insertProperties(prop,operation)
    }catch{
      case e:Exception=>println("Error updating db "+e.printStackTrace())
    }
    val timestamp = (System.currentTimeMillis() - startTime)/1000
    println(s"Database properties_${operation.toString.toLowerCase} updated in $timestamp seconds for page ${page.toString}")
  }

  /**
   * Leave running schedule tasks to update DB
   * Every 7 days for Rent
   * Every 30 days for Sale
   */
  def scheduler()={
    // Schedule to update the db every 7 days for Alquiler
    val schedulerRent=new Timer()
    val taskRent = new TimerTask {
      def run(): Unit = {
        updateDB(Operation.RENT, Page.ZONAPROP)
        updateMeli()
        updateDB(Operation.RENT, Page.ARGENPROP)
      }
    }

    schedulerRent.schedule(taskRent,0L,1000L*60L*60L*24L*7L)

    // Schedule to update the db every 30 days for Ventas
    val schedulerSale=new Timer()
    val taskSale=new TimerTask{
      def run(): Unit ={
        updateDB(Operation.SALE,Page.ARGENPROP)
        //updateDB(Operation.SALE,Page.ZONAPROP)
      }
    }
    //schedulerSale.schedule(taskSale,1000L*60L*20L,1000L*60L*60L*24L*30L)
  }

}
