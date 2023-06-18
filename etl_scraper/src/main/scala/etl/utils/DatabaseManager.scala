package etl.utils

import com.typesafe.scalalogging.Logger
import etl.Property

import java.sql.{Connection, DriverManager}

object DatabaseManager {
  val logger = Logger("DatabaseManager")
  def getConnection(): Connection = {
    val url = "jdbc:postgresql://db.igdnlrrqfnwivrfldsyy.supabase.co:5432/postgres"
    val username = "postgres"
    val password = "+?gZMK.KFtxC@3x"

    // Register the PostgreSQL driver
    Class.forName("org.postgresql.Driver")

    // Create the connection
    DriverManager.getConnection(url, username, password)
  }

  def deletePropertiesWithPage(operation: Operation.Value, page: Page.Value): Unit = {
    val connection = getConnection()

    try {
      val query = "DELETE FROM properties_scraped WHERE page = ? AND operation = ?"
      val statement = connection.prepareStatement(query)

      statement.setInt(1, page.id)
      statement.setString(2, operation.toString)

      statement.executeUpdate()
      statement.close()
    } finally {
      connection.close()
    }
  }

  def insertProperties(properties: List[Property], operation: Operation.Value): Unit = {
    val connection = getConnection()

    try {
      val query = "INSERT INTO properties_scraped (url, property_type, price, currency, expenses, total_surf, covered_surf, rooms, bedrooms, bathrooms, address, neighborhood, garage, page, operation_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
      val statement = connection.prepareStatement(query)

      for (property <- properties) {
        try {
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
          statement.setString(15, operation.toString)

          statement.executeUpdate()
        }catch {
          case e: Exception => logger.error("Error inserting property: " + property.url + " " + e.getMessage)
        }
      }

      statement.close()
    } finally {
      connection.close()
    }
  }

}
