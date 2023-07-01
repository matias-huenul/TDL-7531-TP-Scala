package scraper

import scraper.etl.WebScraper
import scraper.etl.utils.Operation

object App extends App{
  //TaskScheduler.scheduler()
  WebScraper.argenprop(Operation.RENT)
  //while (true) {
    //Thread.sleep(1000)
  //}
}
