.PHONY: clean-build up full

clean-build:
	./gradlew clean bootJar

up:
	docker compose --profile full up --build -d

full: clean-build up