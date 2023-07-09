package etl.lib

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.SparkFiles
import org.apache.spark.sql.functions.{regexp_extract, regexp_replace, when, lower, col, substring_index, udf, to_date}
import org.apache.spark.sql.types.{FloatType, IntegerType, StringType}

object PropertiesETL {
    val databseFilesName: String = sys.env("DATABASE_CSVS")
    def propertiesDataUpdate(): Unit = {
        val spark: SparkSession = SparkSession.builder()
            .appName("Example")
            .master("local[*]")
            .config("spark.sql.codegen.wholeStage", "false")
            .getOrCreate()
        import spark.implicits._
        
        println(s"Reading $databseFilesName table")
        val uploadedCsv: DataFrame = spark.read
            .format("jdbc")
            .option("driver", "org.postgresql.Driver")
            .option("url", sys.env("DATABASE_URL"))
            .option("dbtable", databseFilesName)
            .option("user", sys.env("DATABASE_USER"))
            .option("password", sys.env("DATABASE_PASSWORD"))
            .load()
        
        /*// TODO: change to false
        Para la demo de como se actualiza la tabla en supabase
        val notLoadedFiles: DataFrame = uploadedCsv.filter(col("loaded") === false)

        if (notLoadedFiles.count() != 0) {
          val csvToLoad: Seq[(String, String)] = notLoadedFiles.select("url", "file_name").as[(String, String)].collect()

          csvToLoad.foreach(x => {
            val url: String = x._1
            val fileName: String = x._2
            
            // TODO: change to true
            Utils.updateLoadedColumn(url, fileName, "true")
          })
        }*/

        val notLoadedFiles: DataFrame = uploadedCsv.filter(col("loaded") === false)

        if (notLoadedFiles.count() != 0) {
            val csvToLoad: Seq[(String, String)] = notLoadedFiles.select("url", "file_name").as[(String, String)].collect()

            println("Loading csv files")
            csvToLoad.foreach(x => {
                val url: String = x._1
                val fileName: String = x._2

                println(s"Loading $fileName")
                val properties: DataFrame = read_csv(url, fileName)

                println("Saving properties table")
                properties.write.format("jdbc").mode("append").option("driver", "org.postgresql.Driver")
                    .option("url", sys.env("DATABASE_URL"))
                    .option("dbtable", sys.env("DATABASE_PROPERTIES"))
                    .option("user", sys.env("DATABASE_USER"))
                    .option("password", sys.env("DATABASE_PASSWORD"))
                    .save()
                
                println(s"Updating $databseFilesName table")
                Utils.updateLoadedColumn(url, fileName, "true")
            })
            
        } else {
            println("There are no files to load")
        }
  }

    def calculate_bedrooms(rooms_qty: String): Int = {
        if (rooms_qty == null) null

        val bedrooms_qty: Int = rooms_qty.toInt - 1

        if (bedrooms_qty > 0) bedrooms_qty else 1
    }

    def read_csv(url: String, fileName: String): DataFrame = {
        val spark = SparkSession.builder()
            .appName("PropertiesETL")
            .master("local[*]")       
            .getOrCreate()
        import spark.implicits._

        spark.sparkContext.addFile(url)

        val df = spark.read
            .option("header", "true")
            .option("inferSchema", "true")
            .csv("file://"+SparkFiles.get(fileName))
        
        val filteredDF = df.filter($"l2" === "Capital Federal" && $"currency".isin("USD", "ARS") && $"price".isNotNull && $"property_type".isin("Departamento", "PH", "Casa"))

        val columns = List("title", "description")
        val dfCleanDesc = filteredDF.withColumn("description", regexp_replace($"description", "\n", " "))

        // Fillna for surface_total
        val dfSurfaceTotal = columns.foldLeft(dfCleanDesc) { (accDF, column) =>
              accDF.withColumn("surface_total",
                when($"surface_total".isNull && lower(col(column)).contains("superficie total:"), 
                  substring_index(regexp_extract(lower(col(column)), "superficie total: \\d+", 0), " ", 1).cast(IntegerType))
                .otherwise(when($"surface_total".isNull && lower(col(column)).contains("sup. total:"),
                  substring_index(regexp_extract(lower(col(column)), "sup. total: \\d+", 0), " ", 1).cast(IntegerType))
                  .otherwise(when($"surface_total".isNull, regexp_extract(lower(col(column)), "(\\d+)\\s*m\\s*2?", 1).cast(IntegerType))
                .otherwise($"surface_total")))
              )
        }

        // Fillna for bathrooms
        val dfBathroom = columns.foldLeft(dfSurfaceTotal) { (accDF, column) =>
              accDF.withColumn("bathrooms",
                when($"bathrooms".isNull && lower(col(column)).contains("baño"), 
                    substring_index(regexp_extract(lower(col(column)), "\\d+ baños?", 0), " ", 1).cast(IntegerType))
                  .otherwise(when($"bathrooms".isNull && lower(col(column)).contains("baños:"),
                    substring_index(regexp_extract(lower(col(column)), "baños: \\d+", 0), " ", 1).cast(IntegerType))
                    .otherwise(when($"bathrooms".isNull && lower(col(column)).contains("baño completo"), 1)
                  .otherwise($"bathrooms")))
              )
        }

        // Fillna for rooms
        val dfRooms = columns.foldLeft(dfBathroom) { (accDF, column) =>
              accDF.withColumn("rooms",
                when($"rooms".isNull && lower(col(column)).contains("mono"), 1)
                  .otherwise(when($"rooms".isNull && lower(col(column)).contains("amb"),
                    substring_index(regexp_extract(lower(col(column)), "\\d+ amb", 0), " ", 1).cast(IntegerType))
                    .otherwise(when($"rooms".isNull && lower(col(column)).contains("ambientes:"), 
                    substring_index(regexp_extract(lower(col(column)), "ambientes: \\d+", 0), " ", 1).cast(IntegerType))
                  .otherwise($"rooms")))
              )
        }

        val calculate_rooms_udf = udf[Int, String](calculate_bedrooms)

        // Fillna for bedrooms
        val dfBedooms = columns.foldLeft(dfRooms) { (accDF, column) =>
              accDF.withColumn("bedrooms",
                when($"bedrooms".isNull && $"rooms".isNotNull, calculate_rooms_udf(col("rooms")))
                  .otherwise(when($"bedrooms".isNull && lower(col(column)).contains("dormitorio"),
                    substring_index(regexp_extract(lower(col(column)), "\\d+ dormitorios?", 0), " ", 1).cast(IntegerType))
                    .otherwise(when($"bedrooms".isNull && lower(col(column)).contains("dormitorios:"), 
                    substring_index(regexp_extract(lower(col(column)), "dormitorios: \\d+", 0), " ", 1).cast(IntegerType))
                  .otherwise($"bedrooms")))
              )
        }

        val newColumnsTypes = Map(
            "rooms" -> IntegerType,
            "bathrooms" -> IntegerType,
            "bedrooms" -> IntegerType,
            "surface_covered" -> FloatType,
            "surface_total" -> FloatType,
            "price" -> IntegerType)
    
        val convertedDf = newColumnsTypes.foldLeft(dfBedooms) { case (accDF, (columnName, newType)) =>
            accDF.withColumn(columnName, accDF(columnName).cast(newType))
        }

        val finalDf = convertedDf.select(to_date(col("created_on"),"yyyy-MM-dd").alias("created_on"), 
            col("l3").alias("neighborhood"), col("rooms"), col("bedrooms"), col("surface_covered"), 
            col("bathrooms"), col("surface_total"), col("price"), col("currency"), col("property_type"), 
            col("operation_type"))

        val definitiveDF = finalDf.na.drop()

        definitiveDF
    }
}