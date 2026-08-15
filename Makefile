.PHONY: clean-build up app deploy

clean-build:
	./gradlew clean bootJar

up:
	docker compose --profile app up --build -d

app: clean-build up

deploy: clean-build
	docker compose --profile deploy up --build -d