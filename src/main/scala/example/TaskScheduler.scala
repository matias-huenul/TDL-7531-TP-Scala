package example

import com.typesafe.scalalogging.Logger
import example.utils.{Operation, Page, DatabaseManager => DB}

import java.util.Calendar

object TaskScheduler{
  def updateDB(operation: Operation.Value = Operation.ALQUILER, page:Page.Value)={
    val logger = Logger("TaskScheduler")
    val currentTime =  Calendar.getInstance.getTime.getTime
    var prop = List[Property]()
    page match {
      case Page.ZONAPROP => prop = WebScraper.zonaprop(operation)
      case Page.ARGENPROP => prop = WebScraper.argenprop(operation)
      case Page.MELI => prop = WebScraper.mercadolibre ()
      case _ => println("No se reconoce la pagina")
    }

    try {
      DB.deletePropertiesWithPage(operation, page)
      DB.insertProperties(prop, operation)
    } catch {
      case e: Exception => println("Error al actualizar la base de datos" + e.printStackTrace())
    }

    logger.info("Database properties_" + operation.toString.toLowerCase +" updated in " + (Calendar.getInstance.getTime.getTime - currentTime)/1000 + " seconds for page " + page.toString)
  }

  def scheduler() ={
    // Schedule to update the db every 7 days for Alquiler
    val schedulerAlquiler = new java.util.Timer()
    val taskAlquiler = new java.util.TimerTask {
      def run() = {
        updateDB(Operation.ALQUILER, Page.ARGENPROP)
        updateDB(Operation.ALQUILER, Page.ZONAPROP)
        updateDB(Operation.ALQUILER, Page.MELI)
      }
    }
    schedulerAlquiler.schedule(taskAlquiler, 0L, 1000L * 60L * 60L * 24L * 7L)

    // Schedule to update the db every 30 days for Ventas
    val schedulerVenta = new java.util.Timer()
    val taskVenta = new java.util.TimerTask {
      def run() = {
        updateDB(Operation.VENTA, Page.ARGENPROP)
        updateDB(Operation.VENTA, Page.ZONAPROP)
      }
    }
    schedulerVenta.schedule(taskVenta, 0L, 1000L * 60L * 60L * 24L * 30L)
  }

  def main(args: Array[String]): Unit = {
    scheduler()
    while (true) {
      Thread.sleep(1000)
    }
  }
}
