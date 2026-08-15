# my-bank-back-app

Мультимодульное микросервисное приложение **«Банк»** с веб-интерфейсом, OAuth2/OIDC-аутентификацией через Keycloak, Spring Cloud Gateway, Eureka Service Discovery и Spring Cloud Config.

---

## 🚀 О проекте

Приложение позволяет клиенту банка:

- редактировать данные своего аккаунта (ФИО, дата рождения);
- пополнять и снимать виртуальные деньги со счёта;
- переводить деньги на счёт другого пользователя.

### Состав приложения

| Модуль                  | Назначение                                      | Порт  |
|-------------------------|--------------------------------------------------|-------|
| `front-ui-service`      | Веб-интерфейс (Thymeleaf)                        | 8085  |
| `api-gateway`           | Spring Cloud Gateway + JWT Token Relay           | 8080  |
| `account-service`       | Управление аккаунтами и балансами                | 8081  |
| `cash-service`          | Пополнение / снятие средств                      | 8082  |
| `transfer-service`      | Переводы между счетами                           | 8083  |
| `notification-service`  | Уведомления о операциях                          | 8084  |
| `config-server`         | Spring Cloud Config Server                       | 8888  |
| `discovery-server`      | Netflix Eureka Server                            | 8761  |
| `shared`                | Общие компоненты (в т.ч. Circuit Breaker)        | —     |

Инфраструктура:

- **PostgreSQL** — персистентное хранилище
- **Keycloak** — OAuth 2.0 / OIDC сервер авторизации

---

## 🛠 Технологический стек

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 3.5.15
- **Cloud:** Spring Cloud 2025.0.2
- **Web:** Spring Web MVC, Thymeleaf
- **Gateway:** Spring Cloud Gateway (WebFlux)
- **Service Discovery:** Netflix Eureka
- **Config:** Spring Cloud Config
- **Security:** Spring Security OAuth2 Client + Resource Server, Keycloak
- **Data:** Spring Data JPA + PostgreSQL + Flyway
- **Сборка:** Gradle (Version Catalog)
- **Контейнеризация:** Docker, Docker Compose
- **Тестирование:** JUnit 5, Spring Boot Test, Mockito

---

## ⚙️ Переменные окружения

Заполните файл `.env.my-bank` в корне проекта:

```bash
KEYCLOAK_ADMIN_USERNAME=<логин>
KEYCLOAK_ADMIN_PASSWORD=<пароль>

FRONT_UI_SERVICE_CLIENT_SECRET=<секрет_из_realm>
CASH_SERVICE_CLIENT_SECRET=<секрет_из_realm>
TRANSFER_SERVICE_CLIENT_SECRET=<секрет_из_realm>
ACCOUNT_SERVICE_CLIENT_SECRET=<секрет_из_realm>
NOTIFICATION_SERVICE_CLIENT_SECRET=<секрет_из_realm>
```

Секреты клиентов должны совпадать со значениями в /keycloak/realms/my-bank-realm-realm.json (поля secret).

---

## Тестовые пользователи Keycloak

| Логин     | Пароль    | Роли                                                            |
|-----------|-----------|-----------------------------------------------------------------|
| `dmitry`  | `dmitry`  | `USER, ACCOUNT_READ, ACCOUNT_WRITE, CASH_WRITE, TRANSFER_WRITE` |
| `alexey`  | `alexey`  | `USER, ACCOUNT_READ, ACCOUNT_WRITE, CASH_WRITE, TRANSFER_WRITE` |
| `elena`   | `elena`   | `USER, ACCOUNT_READ, ACCOUNT_WRITE, CASH_WRITE, TRANSFER_WRITE` |

## 🐳 Запуск через Docker Compose

### 1. Сборка всех сервисов

```bash
./gradlew clean bootJar
```

### 2. Запуск полной инфраструктуры + всех сервисов

```bash
docker compose --profile full up --build -d
```

### 3. Проверка статуса

```bash
docker compose --profile full up --build -d
```

### 4. Просмотр логов

```bash
docker compose --profile full logs -f
```

### 5. Остановка

```bash
docker compose --profile full down
```

### 6. Сброс локальных данных

```bash
docker compose down --volumes
```

### Полезные URL после запуска

| Сервис               | URL                     |
|----------------------|-------------------------|
| `Front UI Service`   | `http://localhost:8085` |
| `API Gateway`        | `http://localhost:8080` |
| `Keycloak Admin`     | `http://localhost:8180` |
| `Eureka Dashboard`   | `http://localhost:8761` |
| `Config Server`      | `http://localhost:8888` |

---

## 🔧 Как внести изменения

- **Создайте новую ветку: git checkout -b feature/название**
- **Внесите изменения**
- **Запустите тесты: ./gradlew test**
- **Соберите проект: ./gradlew clean bootJar**
- **Создайте Pull Request**

---