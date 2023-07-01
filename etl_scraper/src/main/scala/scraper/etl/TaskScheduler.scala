package scraper.etl

import scraper.etl.model.Property
import scraper.etl.utils.{Operation, Page, DatabaseManager => DB}

import java.util.{Timer, TimerTask}
import java.util.Calendar

object TaskScheduler{
  private def updateMeli(operation:Operation.Value=Operation.RENT): Unit = {
    val rentProperties: List[Property] = MeliAPI.getRentPropertiesCABA
    val currentTime=Calendar.getInstance.getTime.getTime

    try {
      DB.deletePropertiesWithPage(operation, Page.MELI)
      DB.insertProperties(rentProperties.toSet, operation)
    } catch {
      case e: Exception => println("Error updating db " + e.printStackTrace())
    }
    println("Database properties_"+operation.toString.toLowerCase+" updated in "+(Calendar.getInstance.getTime.getTime-currentTime)/1000+" seconds for page "+ Page.MELI.toString + " with " + rentProperties.length + " properties")
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
      case Page.ARGENPROP=>prop=WebScraper.argenprop(operation)
      case Page.MELI=>prop=WebScraper.mercadolibre().toSet
      case _=>println("Page not recognized")
    }
    val currentTime=Calendar.getInstance.getTime.getTime

    try{
      DB.deletePropertiesWithPage(operation,page)
      DB.insertProperties(prop,operation)
    }catch{
      case e:Exception=>println("Error updating db "+e.printStackTrace())
    }

    println("Database properties_"+operation.toString.toLowerCase+" updated in "+(Calendar.getInstance.getTime.getTime-currentTime)/1000+" seconds for page "+page.toString)
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
        updateMeli()
        updateDB(Operation.RENT, Page.ARGENPROP)
        updateDB(Operation.RENT, Page.ZONAPROP)
      }
    }

    schedulerRent.schedule(taskRent,0L,1000L*60L*60L*24L*7L)

    // Schedule to update the db every 30 days for Ventas
    val schedulerSale=new Timer()
    val taskSale=new TimerTask{
      def run()={
        updateDB(Operation.SALE,Page.ARGENPROP)
        //updateDB(Operation.SALE,Page.ZONAPROP)
      }
    }
    schedulerSale.schedule(taskSale,1000L*60L*20L,1000L*60L*60L*24L*30L)
  }

}
