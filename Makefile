all: start assembly run

start:
	docker compose up -d

assembly:
	docker compose exec scala bash -c "source ~/.sdkman/bin/sdkman-init.sh && sbt assembly"

# run:
# 	docker compose exec scala bash -c "source ~/.sdkman/bin/sdkman-init.sh && sbt run"

run:
	docker compose exec scala bash -c "~/spark-3.4.0-bin-hadoop3/bin/spark-submit --class example.Hello --master local[*] target/scala-2.12/sample-assembly-1.0.jar"

clean:
	docker compose exec scala bash -c "source ~/.sdkman/bin/sdkman-init.sh && sbt clean"

stop:
	docker compose down
