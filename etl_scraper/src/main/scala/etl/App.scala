package etl

object App extends App{
  TaskScheduler.scheduler()

  while (true) {
    Thread.sleep(1000)
  }
}
