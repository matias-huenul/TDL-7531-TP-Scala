all: start run

start:
	docker-compose up -d

assembly:
	docker-compose exec scala bash -c "source ~/.sdkman/bin/sdkman-init.sh && sbt assembly"

run:
	docker compose exec scala bash -c "source ~/.sdkman/bin/sdkman-init.sh && sbt run"

runAssembly: start assembly
	docker-compose exec scala bash -c "~/spark-3.4.0-bin-hadoop3/bin/spark-submit --class etl.Main --master local[*] --conf spark.driver.extraJavaOptions=-Dlog4j.configuration=file:src/main/resources/log4j.properties --conf spark.executor.extraJavaOptions=-Dlog4j.configuration=file:src/main/resources/log4j.properties target/scala-2.12/etl-assembly-1.0.jar"

clean:
	docker-compose exec scala bash -c "source ~/.sdkman/bin/sdkman-init.sh && sbt clean"

stop:
	docker-compose down
