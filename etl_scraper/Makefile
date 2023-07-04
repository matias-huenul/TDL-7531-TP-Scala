all: start run

start:
	docker compose up -d

run:
	docker compose exec scala bash -c "/usr/share/scala/bin/scala target/scala-2.13/etl_scraper-assembly-1.0.jar"

runSbt:
	docker compose exec scala bash -c "/ops/sbt/bin/sbt run"

stop:
	docker compose down
