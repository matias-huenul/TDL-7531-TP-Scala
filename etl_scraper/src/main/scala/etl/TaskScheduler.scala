package etl

import com.typesafe.scalalogging.Logger
import etl.utils.{Operation, Page, DatabaseManager => DB}
import java.util.{Timer, TimerTask}

import java.util.Calendar

object TaskScheduler{
  val logger: Logger =Logger("TaskScheduler")
  private def updateMeli(operation:Operation.Value=Operation.RENT): Unit = {
    val currentTime=Calendar.getInstance.getTime.getTime
    val rentProperties: List[Property] = MeliAPI.getRentPropertiesCABA

    try {
      DB.deletePropertiesWithPage(operation, Page.MELI)
      DB.insertProperties(rentProperties, operation)
    } catch {
      case e: Exception => println("Error updating db " + e.printStackTrace())
    }
    logger.info("Database properties_"+operation.toString.toLowerCase+" updated in "+(Calendar.getInstance.getTime.getTime-currentTime)/1000+" seconds for page "+ Page.MELI.toString + " with " + rentProperties.length + " properties")
  }
  private def updateDB(operation:Operation.Value=Operation.RENT, page:Page.Value)={

    val currentTime=Calendar.getInstance.getTime.getTime
    var prop=List[Property]()
    page match{
      case Page.ZONAPROP=>prop=WebScraper.zonaprop(operation)
      case Page.ARGENPROP=>prop=WebScraper.argenprop(operation)
      case Page.MELI=>prop=WebScraper.mercadolibre()
      case _=>println("Page not recognized")
    }

    try{
      DB.deletePropertiesWithPage(operation,page)
      DB.insertProperties(prop,operation)
    }catch{
      case e:Exception=>println("Error updating db "+e.printStackTrace())
    }

    logger.info("Database properties_"+operation.toString.toLowerCase+" updated in "+(Calendar.getInstance.getTime.getTime-currentTime)/1000+" seconds for page "+page.toString)
  }

  def scheduler()={
    // Schedule to update the db every 7 days for Alquiler
    val schedulerAlquiler=new Timer()
    val taskAlquiler = new TimerTask {
      def run(): Unit = {
        updateDB(Operation.RENT, Page.ARGENPROP)
        updateDB(Operation.RENT, Page.ZONAPROP)
        updateMeli()
      }
    }

    schedulerAlquiler.schedule(taskAlquiler,0L,1000L*60L*60L*24L*7L)

    // Schedule to update the db every 30 days for Ventas
    val schedulerVenta=new Timer()
    val taskVenta=new TimerTask{
      def run()={
        updateDB(Operation.SALE,Page.ARGENPROP)
        updateDB(Operation.SALE,Page.ZONAPROP)
      }
    }
    schedulerVenta.schedule(taskVenta,1000L*60L*20L,1000L*60L*60L*24L*30L)
  }

}
