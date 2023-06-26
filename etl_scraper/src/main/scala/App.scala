import etl.TaskScheduler

object App extends App{
  TaskScheduler.scheduler()

  while (true) {
    Thread.sleep(1000)
  }
}
