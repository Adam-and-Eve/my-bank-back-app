# my-bank-back-app

Мультимодульное микросервисное приложение «Банк» с веб-интерфейсом, OAuth2/OIDC-аутентификацией через Keycloak, асинхронным взаимодействием через Apache Kafka и Kubernetes-развёртыванием.

---

## 🚀 О проекте

Проект реализует полный цикл CI/CD:
- сборка Java-приложения;
- запуск автоматических тестов;
- сборка Docker-образов;
- публикация Docker-образов;
- развёртывание приложения в Kubernetes через Helm;
- управление секретами через SOPS;
- автоматизация доставки через Jenkins Pipeline.

Приложение позволяет пользователю:

- авторизоваться в системе;
- управлять личным аккаунтом;
- пополнять и снимать виртуальные деньги;
- выполнять переводы между счетами;
- переводить деньги другим пользователям;
- получать актуальные курсы валют.

### Состав приложения

| Модуль                       | Назначение                        | Порт |
|------------------------------|-----------------------------------|------|
| `front-ui-service`           | Веб-интерфейс (Thymeleaf)         | 8085 |
| `api-gateway`                | API Gateway                       | 8080 |
| `account-service`            | Управление аккаунтами и балансами | 8081 |
| `cash-service`               | Пополнение / снятие средств       | 8082 |
| `transfer-service`           | Переводы между счетами            | 8083 |
| `notification-service`       | Уведомления о операциях           | 8084 |
| `exchange-service`           | Курсы валют                       | 8886 |
| `exchange-generator-service` | Генерация курсов валют            | 8087 |
| `blocker-service`            | Проверка подозрительных операций  | 8088 |
| `shared`                     | Общие компоненты                  | —    |

Инфраструктурные компоненты:

| Компонент              | Назначение                                |
|------------------------|-------------------------------------------|
| `PostgreSQL `          | Хранилище данных                          |
| `Keycloak`             | OAuth2/OIDC сервер авторизации            |
| `Apache Kafka`         | Брокер сообщений для асинхронного общения |
| `NGINX Gateway Fabric` | Реализация Kubernetes Gateway API         |
| `Jenkins`              | CI/CD система                             |

---

## 🛠 Технологический стек

## Backend

- Java 21
- Spring Boot 3.5.15
- Spring Security OAuth2 Resource Server
- Spring Security OAuth2 Client
- Spring Kafka
- Spring Data JPA
- PostgreSQL
- Flyway
- JUnit 5
- Mockito

## Kubernetes

- Kubernetes
- Kind
- Helm 3
- Gateway API
- NGINX Gateway Fabric
- Apache Kafka (KRaft mode)

## CI/CD

- Jenkins Pipeline
- Docker
- Docker Registry
- SOPS
- age

---

# 📨 Асинхронное взаимодействие (Apache Kafka)

Kafka выступает в роли асинхронной шины данных, разделяя основные банковские сервисы и сервис уведомлений. Сервис `notification-service` работает исключительно как консьюмер событий.

Архитектура событий:

- Продюсеры: Сервисы `account-service`, `cash-service` и `transfer-service`
- Консьюмер: `notification-service` прослушивает топик и логирует уведомления в application log.
- Топики:
  - Основной топик: `bank.notification` (3 партиции)
  - Топик для сбойных сообщений (DLT): `bank.notification.dlt` (3 партиции, срок хранения — 7 дней).
- Конфигурация: Автоматическое создание топиков отключено, они инициализируются централизованно через Spring KafkaAdmin. Кластер Kafka работает в режиме single-node KRaft (без ZooKeeper). В качестве consumer group используется bank-notification.

Гарантии доставки и обработка ошибок:

- Порядок сообщений: В качестве ключа (message key) передается `recipientLogin`. Это гарантирует, что все уведомления для конкретного пользователя попадут в одну партицию и будут обработаны в строгом хронологическом порядке.
- Retry-логика: При временных сбоях обработки `notification-service` делает 3 попытки: первичная обработка + 2 повтора с задержкой в 1 секунду (FixedBackOff(1000L, 2L)). Если все попытки исчерпаны, сообщение перенаправляется в DLT. Ошибки валидации и невалидный JSON отправляются в DLT сразу, без повторных попыток.
- At-least-once: Смещение (offset) фиксируется исключительно после успешной обработки сообщения или его успешной публикации в DLT. Консьюмер спроектирован с учетом идемпотентности, чтобы безопасно обрабатывать дубликаты в случае сбоя.
- Транзакционность (Eventual Consistency): Фиксация транзакции в БД и отправка сообщения в Kafka не обернуты в распределенную транзакцию (паттерн Transactional Outbox не применяется). В случае сбоя отправки в Kafka, успешная операция в БД не откатывается, а ошибка логируется обработчиком продюсера.

---

# ☸ Kubernetes архитектура

## Cluster

Локальный Kubernetes-кластер создаётся с помощью Kind.

```bash
powershell -ExecutionPolicy Bypass -File .\kubernetes\scripts\kind-bootstrap.ps1
```

Скрипт выполняет:

- создание Kind cluster;
- установку Gateway API CRD;
- установку NGINX Gateway Fabric.

---

# 📦 Helm структура

Проект использует зонтичный Helm chart:

```bash
├── charts/
│   ├── keycloak
│   ├── postgresql
│   ├── kafka
│   └── spring-service
├── my-bank
│
└── values
      └── services
              ├── account-service.yaml
              └── ...
```

## Umbrella chart

Главный chart `helm/my-bank` содержит:
- все Spring Boot сервисы как subcharts;
- PostgreSQL StatefulSet;
- Keycloak deployment;
- Kafka StatefulSet;
- Gateway API ресурсы.

Каждый сервис может быть развернут отдельно или вместе через umbrella chart.

---

# 🔐 Управление секретами

Секреты хранятся в зашифрованном виде с использованием SOPS и age.
Расшифровка выполняется только внутри Jenkins Pipeline.

```bash
envs/secrets/
│
├── values-secrets-dev.enc.yaml
├── values-secrets-test.enc.yaml
├── values-secrets-prod.enc.yaml
└── my-bank-realm-realm.enc.json
```

### Создание и шифрование секретов

Для создания зашифрованных файлов используйте следующие команды:

```bash
cmd /c "sops --encrypt ./helm/my-bank/secrets/values-secrets-dev.yaml > ./envs/secrets/values-secrets-dev.enc.yaml"
```

```bash
cmd /c "sops --encrypt ./helm/my-bank/secrets/values-secrets-test.yaml > ./envs/secrets/values-secrets-test.enc.yaml"
```

```bash
cmd /c "sops --encrypt ./helm/my-bank/secrets/values-secrets-prod.yaml > ./envs/secrets/values-secrets-prod.enc.yaml"
```

```bash
cmd /c "sops --encrypt ./keycloak/realms/my-bank-realm-realm.json > ./envs/secrets/my-bank-realm-realm.enc.json"
```

Пример исходного (незашифрованного) файла секрета `values-secrets.yaml`:

```bash
serviceCredentials:
  BANK_SERVICES_FRONT_UI_SERVICE_CLIENT_SECRET:
  BANK_SERVICES_ACCOUNT_SERVICE_CLIENT_SECRET:
  BANK_SERVICES_CASH_SERVICE_CLIENT_SECRET:
  BANK_SERVICES_TRANSFER_SERVICE_CLIENT_SECRET:
  BANK_SERVICES_NOTIFICATION_SERVICE_CLIENT_SECRET:
  BANK_SERVICES_EXCHANGE_GENERATOR_SERVICE_CLIENT_SECRET:

postgresqlCredentials:
  password:

keycloakCredentials:
  adminUsername:
  adminPassword:
```

---

## ⚙️ Переменные окружения

Заполните файл .env.my-bank в корне проекта перед локальным запуском:

```bash
LOGGING_LEVEL_ROOT=INFO

KC_BOOTSTRAP_ADMIN_USERNAME=__ADMIN_USERNAME__
KC_BOOTSTRAP_ADMIN_PASSWORD=__ADMIN_PASSWORD__
KC_HOSTNAME=http://localhost:8180
KC_HOSTNAME_STRICT=false
KC_HTTP_PORT=8080

BANK_KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/my-bank-realm
BANK_KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/my-bank-realm/protocol/openid-connect/certs
BANK_KEYCLOAK_TOKEN_URI=http://keycloak:8080/realms/my-bank-realm/protocol/openid-connect/token
BANK_KEYCLOAK_USER_INFO_URI=http://keycloak:8080/realms/my-bank-realm/protocol/openid-connect/userinfo
BANK_KEYCLOAK_AUTHORIZATION_URI=http://localhost:8180/realms/my-bank-realm/protocol/openid-connect/auth
BANK_KEYCLOAK_REDIRECT_URI=http://localhost:8085/login/oauth2/code/{registrationId}
BANK_KEYCLOAK_END_SESSION_URI=http://localhost:8180/realms/my-bank-realm/protocol/openid-connect/logout

BANK_KEYCLOAK_REALM_DIRECTORY=./envs/runtime/

KAFKA_CLUSTER_ID=04xAf2BWSNChNCCMmMy3CA
KAFKA_NODE_ID=1
KAFKA_PROCESS_ROLES=broker,controller
KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT
KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1
KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0
KAFKA_NUM_PARTITIONS=3
KAFKA_AUTO_CREATE_TOPICS_ENABLE="false"
KAFKA_LOG_DIRS=/var/lib/kafka/data

SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
BANK_KAFKA_NOTIFICATION_TOPIC=bank.notification

BANK_SERVICES_FRONT_UI_SERVICE_CLIENT_SECRET=__FRONT_UI_SERVICE_CLIENT_SECRET__
BANK_SERVICES_ACCOUNT_SERVICE_CLIENT_SECRET=__ACCOUNT_SERVICE_CLIENT_SECRET__
BANK_SERVICES_CASH_SERVICE_CLIENT_SECRET=__CASH_SERVICE_CLIENT_SECRET__
BANK_SERVICES_TRANSFER_SERVICE_CLIENT_SECRET=TRANSFER_SERVICE_CLIENT_SECRET__
BANK_SERVICES_NOTIFICATION_SERVICE_CLIENT_SECRET=__NOTIFICATION_SERVICE_CLIENT_SECRET__
BANK_SERVICES_EXCHANGE_GENERATOR_SERVICE_CLIENT_SECRET=__EXCHANGE_GENERATOR_SERVICE_CLIENT_SECRET__

BANK_PUBLIC_BASE_URL=http://localhost:8085

BANK_SERVICES_FRONT_UI_SERVICE_BASE_URL=http://front-ui-service:8085
BANK_SERVICES_ACCOUNT_SERVICE_BASE_URL=http://account-service:8081
BANK_SERVICES_CASH_SERVICE_BASE_URL=http://cash-service:8082
BANK_SERVICES_EXCHANGE_GENERATOR_BASE_URL=http://exchange-generator-service:8087
BANK_SERVICES_EXCHANGE_SERVICE_BASE_URL=http://exchange-service:8086
BANK_SERVICES_TRANSFER_SERVICE_BASE_URL=http://transfer-service:8083
BANK_SERVICES_BLOCKER_SERVICE_BASE_URL=http://blocker-service:8088

BANK_SERVICES_BLOCKER_SERVICE_MAX_AMOUNT="100000.00"
BANK_SERVICES_EXCHANGE_GENERATOR_SERVICES_FIXED_DELAY_MS="1000"

JENKINS_ADMIN_PASSWORD=__JENKINS_ADMIN_PASSWORD__
```

---

## Тестовые пользователи Keycloak

| Логин     | Пароль    | Роли                                                            |
|-----------|-----------|-----------------------------------------------------------------|
| `dmitry`  | `dmitry`  | `USER, ACCOUNT_READ, ACCOUNT_WRITE, CASH_WRITE, TRANSFER_WRITE` |
| `alexey`  | `alexey`  | `USER, ACCOUNT_READ, ACCOUNT_WRITE, CASH_WRITE, TRANSFER_WRITE` |
| `elena`   | `elena`   | `USER, ACCOUNT_READ, ACCOUNT_WRITE, CASH_WRITE, TRANSFER_WRITE` |


---

## 🐳 Локальный запуск через Docker Compose

### 1. Сборка всех сервисов

```bash
./gradlew clean bootJar
```

### 2. Запуск полной инфраструктуры + всех сервисов

```bash
docker compose --profile app --env-file .env.my-bank up --build -d
```

### 3. Проверка статуса

```bash
docker compose --profile app ps
```

### 4. Просмотр логов

```bash
docker compose --profile app logs -f
```

### 5. Остановка

```bash
docker compose --profile app down
```

### 6. Сброс локальных данных

```bash
docker compose down --volumes
```

---

# 🔄 Jenkins CI/CD

Jenkins используется для:

- проверки проекта;
- запуска тестов;
- сборки Docker images;
- публикации images;
- Helm deployment.

Jenkins запускается отдельным скриптом:

```bash
powershell -ExecutionPolicy Bypass `
-File .\jenkins\scripts\start-jenkins.ps1
```

После запуска:
http://localhost:8090

---

# Jenkins Credentials

Необходимо создать следующие Credentials:

### my-bank-sops-age-key
- тип: `Secret file`
- описание: Файл с публичным и приватным ключом age для SOPS

### my-bank-kubeconfig
- тип: `Secret file`
- описание: Kubernetes kubeconfig (cоздаётся с помощью скрипта)

```bash
powershell -ExecutionPolicy Bypass `
-File .\kubernetes\scripts\create-jenkins-kubeconfig.ps1
```

### my-bank-registry-credentials
- тип: `Username with password`
- описание: Используется для публикации Docker images

---

# Jenkins Pipeline

В проекте предусмотрены два типа Pipeline:

### Service Pipeline

Используется для отдельного микросервиса.

Этапы:
- validate
- java tests
- bootJar
- docker build
- docker push
- helm lint
- helm template
- deploy test
- manual approval
- deploy prod

### Umbrella Pipeline

Используется для полного приложения.

Выполняет:
- сборку всех сервисов;
- создание всех Docker images;
- deployment umbrella Helm chart.

---

# Запуск Pipeline Jenkins

### 1. Создать Pipeline: `New Item -> Pipeline`

### 2. Выбрать в `Definition`: `Pipeline script from SCM`

### 2. Выбрать в SCM: `Git`

### 3. Выбрать в `Repository URL`:

```bash
https://github.com/Adam-and-Eve/my-bank-back-app.git
```

### 4. Выбрать в `Branch Specifier (blank for 'any')`:

```bash
*/module_three_sprint_eleven_branch
```

### 5. Сохранить изменения

### 6. Перейти в `Build with Parameters`

### 7. Указать в `IMAGE_REGISTRY`:

```bash
docker.io/<docker-login>
```

### 8. Указать в `IMAGE_TAG`: `Имя тега`

### 9. Выбрать параметры:
- `BUILD_IMAGES`
- `PUSH_IMAGES`
- `DEPLOY_TEST`
- `DEPLOY_PROD`

### 10. Запустить: `Build`

### 11. Проверить:

```bash
kubectl get pods -n prod
```

```bash
kubectl port-forward -n prod svc/my-bank-gateway-nginx 8080:80
```

http://localhost:8080

### 12. Удалить и очистить:

```bash
helm uninstall my-bank -n test
```

```bash
kubectl delete namespace test
```

```bash
helm uninstall my-bank -n prod
```

```bash
kubectl delete namespace prod
```

```bash
kind delete cluster
```

```bash
powershell -ExecutionPolicy Bypass `
-File .\jenkins\scripts\stop-jenkins.ps1
```

---

# Kubernetes Deployment вручную

Обновление зависимостей:

```bash
helm dependency update helm/my-bank
```

Проверка:

```bash
helm lint helm/my-bank
```

Рендер:

```bash
helm template my-bank . `
  -f values-test.yaml `
  -f secrets/values-secrets-test.yaml `
  -f ../values/services/account-service.yaml `
  -f ../values/services/api-gateway.yaml `
  -f ../values/services/blocker-service.yaml `
  -f ../values/services/cash-service.yaml `
  -f ../values/services/exchange-generator-service.yaml `
  -f ../values/services/exchange-service.yaml `
  -f ../values/services/front-ui-service.yaml `
  -f ../values/services/notification-service.yaml `
  -f ../values/services/transfer-service.yaml `
  > rendered.yaml
```

Установка:

```bash
helm upgrade --install my-bank . `
  -n my-bank `
  -f values-test.yaml `
  -f ../values/services/account-service.yaml `
  -f ../values/services/api-gateway.yaml `
  -f ../values/services/blocker-service.yaml `
  -f ../values/services/cash-service.yaml `
  -f ../values/services/exchange-generator-service.yaml `
  -f ../values/services/exchange-service.yaml `
  -f ../values/services/front-ui-service.yaml `
  -f ../values/services/notification-service.yaml `
  -f ../values/services/transfer-service.yaml `
  -f ../../envs/dev/runtime/values-secrets-test.yaml
```

Проверка:

```bash
kubectl get pods -n my-bank
```

```bash
kubectl port-forward -n my-bank svc/my-bank-gateway-nginx 8080:80
```

http://localhost:8080

---

## 🔧 Как внести изменения

- **Создайте новую ветку: git checkout -b feature/название**
- **Внесите изменения**
- **Запустите тесты: ./gradlew test**
- **Соберите проект: ./gradlew clean bootJar**
- **Создайте Pull Request**

---