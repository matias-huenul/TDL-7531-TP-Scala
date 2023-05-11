all: start run

start:
	docker compose ps | grep -q scala || docker compose up -d

run:
	docker compose exec scala bash -c "source ~/.sdkman/bin/sdkman-init.sh && sbt run"

stop:
	docker compose down
