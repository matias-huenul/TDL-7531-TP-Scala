package example

import example.utils.{Operation, Page}

import java.sql.{Connection, DriverManager, PreparedStatement}

object TaskScheduler{
  def getConnection(): Connection = {
    val url = "jdbc:postgresql://db.igdnlrrqfnwivrfldsyy.supabase.co:5432/postgres"
    val username = "postgres"
    val password = "+?gZMK.KFtxC@3x"

    // Register the PostgreSQL driver
    Class.forName("org.postgresql.Driver")

    // Create the connection
    DriverManager.getConnection(url, username, password)
  }

  def deletePropertiesWithPage(operation:Operation.Value,page: Page.Value): Unit = {
    val connection = getConnection()

    try {
      val query = "DELETE FROM properties_"+ operation.toString.toLowerCase +" WHERE page = ?"
      val statement = connection.prepareStatement(query)

      statement.setInt(1, page.id)

      statement.executeUpdate()
      statement.close()
    } finally {
      connection.close()
    }
  }

  def insertProperties(properties: List[Property], operation: Operation.Value): Unit = {
    val connection = getConnection()

    try {
      val query = "INSERT INTO properties_"+ operation.toString.toLowerCase +" (url, type, price, currency, expenses, total_surf, covered_surf, rooms, bedrooms, bathrooms, address, barrio, garage, page) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
      val statement = connection.prepareStatement(query)

      for (property <- properties) {
        statement.setString(1, property.url)
        statement.setString(2, property.propType.toString)
        statement.setInt(3, property.price)
        statement.setString(4, property.currency.toString)
        statement.setInt(5, property.expenses)
        statement.setInt(6, property.totalSurf)
        statement.setInt(7, property.coveredSurf)
        statement.setInt(8, property.rooms)
        statement.setInt(9, property.bedrooms)
        statement.setInt(10, property.bathrooms)
        statement.setString(11, property.address)
        statement.setString(12, property.barrio)
        statement.setInt(13, property.garage)
        statement.setInt(14, property.page.id)

        statement.executeUpdate()
      }

      statement.close()
    } finally {
      connection.close()
    }
  }

  def updateZonaprop(operation: Operation.Value)={
    val prop = WebScraper.zonaprop(operation)
    deletePropertiesWithPage(operation, Page.ZONAPROP)
    insertProperties(prop, operation)
  }

  def main(args: Array[String]): Unit = {
    updateZonaprop(Operation.ALQUILER)
    updateZonaprop(Operation.VENTA)
  }
}
