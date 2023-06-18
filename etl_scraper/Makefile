all: start run

start:
	docker compose up -d

assembly:
	docker compose exec scala bash -c "source ~/.sdkman/bin/sdkman-init.sh && sbt assembly"

run:
	docker compose exec scala bash -c "source ~/.sdkman/bin/sdkman-init.sh && sbt run"

clean:
	docker compose exec scala bash -c "source ~/.sdkman/bin/sdkman-init.sh && sbt clean"

stop:
	docker compose down
