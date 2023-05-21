package example


import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.functions.{lower, regexp_extract, regexp_replace, substring_index, when, concat, lit}
import org.apache.spark.sql.types.{FloatType, IntegerType, StringType}
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.{Paths, Files}

object PropertiesReader {

  def read_csv(path: String)= {
    val spark: SparkSession = SparkSession.builder()
      .appName("Example")
      .master("local[*]")
      .getOrCreate()
    import spark.implicits._

    if (!Files.exists(Paths.get("./ar_properties.csv"))){
      println("Downloading file...")
      val url = "https://gitlab.com/mpata2000/datos_tdl/-/raw/main/ar_properties.csv"
      val outputPath = "ar_properties.csv" // Replace with the desired output file path

      try {
        val inputStream = new URL(url).openStream()
        val outputStream = new FileOutputStream(outputPath)
        val buffer = new Array[Byte](1024)
        var bytesRead = inputStream.read(buffer)

        while (bytesRead != -1) {
          outputStream.write(buffer, 0, bytesRead)
          bytesRead = inputStream.read(buffer)
        }

        outputStream.close()
        inputStream.close()
        println("File downloaded successfully.")
      } catch {
        case ex: Exception => println("Error downloading file from Google Drive.")
      }
    }

    val df: DataFrame = spark.read
      .option("header", "true")
      .csv("./ar_properties.csv")


    // Filter by l2 = Capital Federal, currency = USD or ARS, price is not null, property_type = Departamento, PH, Casa
    val filteredDF = df.filter($"l2" === "Capital Federal" && $"currency".isin("USD", "ARS") && $"price".isNotNull && $"property_type".isin("Departamento", "PH", "Casa"))

    // Create a new column with the concatenation of lat and lon
    val dfWithDir = filteredDF.withColumn("direccion", concat($"lat".cast(StringType), lit(","), $"lon".cast(StringType)))
    //Clean description column
    val dfCleanDesc = dfWithDir.withColumn("description", regexp_replace($"description", "\n", " "))

    val pattern = "(\\d+)\\s*m\\s*2?" // Should a number followed by m or m2 with or without spaces in between
    val columns = List("title", "description")

    val dfSurface = columns.foldLeft(dfCleanDesc) { (accDF, column) =>
      accDF.withColumn("surface_total",
        when($"surface_total".isNull && col(column).isNotNull, regexp_extract(lower(col(column)), pattern, 1).cast(FloatType))
          .otherwise($"surface_total"))
    }

    // Search for rooms in title and description
    val updatedDF = columns.foldLeft(dfSurface) { (accDF, column) =>
      accDF.withColumn("rooms",
        when($"rooms".isNull && col(column).contains("mono"), 1)
          .otherwise(when($"rooms".isNull && lower(col(column)).contains("amb"),
            substring_index(regexp_extract(lower(col(column)), "\\d+ amb", 0), " ", 1).cast(IntegerType))
          .otherwise($"rooms"))
      )
    }

    // TODO: Search for bathrooms and rooms?

    //Drop unnecessary columns
    val finalDF = updatedDF.drop("id", "ad_type", "start_date", "end_date", "lat", "lon", "l1", "l2", "l4", "l5", "l6", "price_period", "title", "description")

    // Write to postgres
    finalDF.write
      .format("jdbc")
      .mode("overwrite")
      .option("driver", "org.postgresql.Driver")
      .option("url", "jdbc:postgresql://db.igdnlrrqfnwivrfldsyy.supabase.co:5432/postgres")
      .option("dbtable", "properties")
      .option("user", "postgres")
      .option("password", "+?gZMK.KFtxC@3x")
      .save()
  }
}
