package example


import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, concat, lit, lower, regexp_extract, regexp_replace, substring_index, when, max}
import org.apache.spark.sql.types.{FloatType, IntegerType, StringType}

import java.io.FileOutputStream
import java.net.URL
import java.nio.file.{Files, Paths}

object PropertiesReader {
  def propertiesDataUpdate(overwrite: Boolean = false): Unit = {
    val spark: SparkSession = SparkSession.builder()
      .appName("Example")
      .master("local[*]")
      .getOrCreate()

    val csv = List(("https://gitlab.com/mpata2000/datos_tdl/-/raw/main/ar_properties.csv", "ar_properties_2020_2021.csv"))

    val uploadedCsv: DataFrame = spark.read
      .format("jdbc")
      .option("driver", "org.postgresql.Driver")
      .option("url", "jdbc:postgresql://db.igdnlrrqfnwivrfldsyy.supabase.co:5432/postgres")
      .option("dbtable", "csv")
      .option("user", "postgres")
      .option("password", "+?gZMK.KFtxC@3x")
      .load()

    csv.foreach(x => {
      val url = x._1
      val outputPath = x._2

      // Check if file is already uploaded
      if (overwrite || uploadedCsv.withColumn("link", lower(col("link"))).filter(col("link") === url).count() == 0) {
        val properties = read_csv(url, outputPath)


        // TODO: Chek if id is not needed, maybe use link as primary key. Add created_at!
        val maxId: Int = uploadedCsv.agg(max("id")).head().getInt(0)
        val values: Seq[(Int, String, String)] = Seq((maxId+1, url, outputPath))
        val df = spark.createDataFrame(values).toDF("id","link", "file_name")
        val union = uploadedCsv.unionByName(df,allowMissingColumns = true)

        val mode: String = if (overwrite && (csv.indexOf(x) == 0)) "overwrite" else "append"

        union.write.format("jdbc").mode(mode).option("driver", "org.postgresql.Driver")
          .option("url", "jdbc:postgresql://db.igdnlrrqfnwivrfldsyy.supabase.co:5432/postgres")
          .option("dbtable", "csv")
          .option("user", "postgres")
          .option("password", "+?gZMK.KFtxC@3x")
          .save()

        properties.write.format("jdbc").mode(mode).option("driver", "org.postgresql.Driver")
          .option("url", "jdbc:postgresql://db.igdnlrrqfnwivrfldsyy.supabase.co:5432/postgres")
          .option("dbtable", "properties")
          .option("user", "postgres")
          .option("password", "+?gZMK.KFtxC@3x")
          .save()
      }else{
        println("File already uploaded")
      }
    })
  }

  // Read csv from path if it exists, otherwise download it from url
  // Then clean and transform the data
  def read_csv(url: String, path: String): DataFrame = {
    val spark: SparkSession = SparkSession.builder()
      .appName("Example")
      .master("local[*]")
      .getOrCreate()
    import spark.implicits._

    val outPath = "./" + path

    if (!Files.exists(Paths.get(outPath))) {
      println("Downloading file...")

      try {
        val inputStream = new URL(url).openStream()
        val outputStream = new FileOutputStream(outPath)
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

    val df = spark.read
      .option("header", "true")
      .csv(outPath)


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
    // TODO: Rename columns

    //Drop unnecessary columns
    val finalDF = updatedDF.drop("id", "ad_type", "start_date", "end_date", "lat", "lon", "l1", "l2", "l4", "l5", "l6", "price_period", "title", "description")

    // Write to postgres
    finalDF
  }
}
