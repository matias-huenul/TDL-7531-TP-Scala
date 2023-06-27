package etl.utils

import com.typesafe.scalalogging.Logger
import etl.model.Property

import java.sql.{Connection, DriverManager}

object DatabaseManager {
  private def getConnection: Connection = {
    val url = sys.env("JDBC_URL")
    val username = sys.env("DB_USER")
    val password = sys.env("DB_PASSWORD")

    Class.forName("org.postgresql.Driver")

    // Create the connection
    DriverManager.getConnection(url, username, password)
  }

  def deletePropertiesWithPage(operation: Operation.Value, page: Page.Value): Unit = {
    val connection = getConnection

    try {
      val query = "DELETE FROM properties_"+operation.toString.toLowerCase+" WHERE page = ?"
      val statement = connection.prepareStatement(query)

      statement.setInt(1, page.id)

      statement.executeUpdate()
      statement.close()
    } finally {
      connection.close()
    }
  }

  def insertProperties(properties: List[Property], operation: Operation.Value): Unit = {
    val connection = getConnection

    try {
      val query = "INSERT INTO properties_"+operation.toString.toLowerCase+" (url, property_type, price, currency, expenses, total_surf, covered_surf, rooms, bedrooms, bathrooms, address, neighborhood, garage, page)" +
        " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
      val statement = connection.prepareStatement(query)

      for (property <- properties) {
        try {
          statement.setString(1, property.url)
          statement.setString(2, property.propType.toString)
          statement.setInt(3, property.price)
          statement.setString(4, property.currency.toString)
          statement.setInt(5, property.expenses)
          statement.setInt(6, property.totalArea)
          statement.setInt(7, property.coveredArea)
          statement.setInt(8, property.rooms)
          statement.setInt(9, property.bedrooms)
          statement.setInt(10, property.bathrooms)
          statement.setString(11, property.address)
          statement.setString(12, property.neighborhood)
          statement.setInt(13, property.garage)
          statement.setInt(14, property.page.id)

          statement.executeUpdate()
        }catch {
          case e: Exception => println("Error inserting property: " + property.url + " " + e.getMessage)
        }
      }

      statement.close()
    } finally {
      connection.close()
    }
  }
}

