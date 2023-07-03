package scraper

import scraper.etl.{MeliAPI, TaskScheduler}

object App extends App{
  MeliAPI.getRentPropertiesCABA
  /*TaskScheduler.scheduler()

  while (true) {
    Thread.sleep(1000)
  }*/
}
