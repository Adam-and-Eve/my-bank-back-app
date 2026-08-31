# my-bank-back-app

Мультимодульное микросервисное приложение «Банк» с веб-интерфейсом, OAuth2/OIDC-аутентификацией через Keycloak, асинхронным взаимодействием через Apache Kafka, Kubernetes-развёртыванием и полноценным стеком Observability (Zipkin, Prometheus, Grafana, ELK).

---

## 🚀 О проекте

Проект реализует полный цикл CI/CD:
- сборка Java-приложения и Docker-образов;
- запуск автоматических тестов (Unit, Contract, Infrastructure);
- публикация Docker-образов;
- развёртывание приложения в два изолированных Kubernetes-кластера (Test и Prod) через Helm;
- управление секретами через SOPS;
- автоматизация доставки через Jenkins Pipeline;
- распределённая трассировка запросов (Zipkin);
- сбор, хранение и визуализация метрик (Prometheus + Grafana);
- централизованное логирование бизнес-операций (ELK Stack).

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
| `exchange-service`           | Курсы валют                       | 8086 |
| `exchange-generator-service` | Генерация курсов валют            | 8087 |
| `blocker-service`            | Проверка подозрительных операций  | 8088 |
| `shared`                     | Общие компоненты                  | —    |

Инфраструктурные компоненты:

| Компонент                               | Назначение                                     |
|-----------------------------------------|------------------------------------------------|
| `PostgreSQL `                           | Хранилище данных                               |
| `Keycloak`                              | OAuth2/OIDC сервер авторизации                 |
| `Apache Kafka`                          | Брокер сообщений для асинхронного общения      |
| `NGINX Gateway Fabric`                  | Реализация Kubernetes Gateway API              |
| `Zipkin`                                | Система распределённых трассировок             |
| `Prometheus + Grafana`                  | Сбор метрик, алертинг и визуализация дашбордов |
| `Elasticsearch, Logstash, Kibana (ELK)` | Централизованное логирование                   |
| `Jenkins`                               | CI/CD система                                  |

---

## 🛠 Технологический стек

### Backend

- Java 21, Spring Boot 3.5.15
- Spring Security OAuth2 (Resource Server & Client)
- Spring Kafka, Spring Data JPA, PostgreSQL, Flyway
- JUnit 5, Mockito
- Observability: Micrometer, Micrometer Tracing (Brave), Slf4j + Log4j2/Logback

### Kubernetes & CI/CD

- Kubernetes (Kind), Helm, Gateway API
- NGINX Gateway Fabric
- Jenkins Pipeline, Docker, Docker Registry, SOPS, age

---

## 📨 Архитектура Kafka и гарантии доставки

Kafka выступает в роли асинхронной шины данных, разделяя основные банковские сервисы и сервис уведомлений. Сервис `notification-service` работает исключительно как консьюмер событий.

Архитектура событий:

- Продюсеры: Сервисы `account-service`, `cash-service` и `transfer-service` (генерация событий интегрирована через паттерн Transactional Outbox).
- Консьюмер: `notification-service` прослушивает топик и обрабатывает уведомления.
- Топики:
  - Основной топик: my.bank.notification (3 партиции)
  - Топик для сбойных сообщений (DLT): `bank.notification.dlt` (3 партиции, срок хранения - 7 дней).
- Конфигурация: Автоматическое создание топиков отключено, они инициализируются централизованно через Spring KafkaAdmin. Кластер Kafka работает в режиме single-node KRaft (без ZooKeeper), consumer group - `bank-notification`.

Гарантии доставки и обработка ошибок

- Порядок сообщений: В качестве ключа (message key) передается `recipientLogin`. Это гарантирует, что все уведомления для конкретного пользователя попадут в одну партицию и будут обработаны в строгом хронологическом порядке.
- Retry-логика: При временных сбоях обработки `notification-service` делает попытки с задержкой (FixedBackOff). При исчерпании попыток сообщение перенаправляется в DLT. Ошибки валидации и невалидный JSON отправляются в DLT сразу.
- At-least-once и Идемпотентность: Смещение (offset) фиксируется исключительно после успешной обработки сообщения или его успешной публикации в DLT. Консьюмер спроектирован с учетом идемпотентности для безопасной обработки дубликатов.
- Транзакционность (Transactional Outbox): Для обеспечения надежности доставки и устранения проблемы dual-write используется паттерн Transactional Outbox. События уведомлений сохраняются в БД атомарно вместе с бизнес-данными операции в рамках единой транзакции сервиса, после чего асинхронно доставляются в брокер через издатель `KafkaNotificationEventPublisher`, обеспечивая гарантии `At-Least-Once` без потери сообщений при сбоях сети или брокера.

---

## 🔭 Observability (Мониторинг, логирование, трейсинг)

### Распределённая трассировка (Zipkin)
- Все микросервисы и Front UI поставляют трейсы HTTP-запросов, обращений к БД и Apache Kafka через Micrometer Tracing.
- Проброс контекста (`traceId`, `spanId`, `parentSpanId`) осуществляется через стандартные HTTP-заголовки. Front UI выступает инициатором генерации первичного `traceId`.

### Метрики и Алерты (Prometheus + Grafana)
- Сбор базовых JVM и HTTP метрик (RPS, 4xx, 5xx, персентили) реализован через Spring Boot Actuator.
- Внедрены кастомные бизнес-метрики:
  - Неуспешные попытки снятия и перевода средств (с группировкой по логинам).
  - Неуспешные доставки уведомлений (с группировкой по логину).
- В Grafana преднастроены дашборды: Bank HTTP Overview, Bank JVM Overview, Bank Business Failures. Настроены алерты по пороговым значениям (Prometheus).

### Централизованное логирование (ELK Stack)
- Логи передаются в едином формате (JSON/структурированный текст) через Slf4j.
- Интеграция с Zipkin: каждый лог содержит traceId и spanId для сквозного поиска в Kibana.
- Logstash выполняет роль агрегатора и фильтра, Elasticsearch хранит индексы, Kibana обеспечивает визуализацию через Data View bank-logs.

### Учебные ограничения среды
- Zipkin хранит трейсы in-memory.
- Elasticsearch развернут в режиме single-node без security-плагинов.
- Prometheus имеет retention 24 часа. Конфигурация инфраструктуры адаптирована для локальной разработки и тестов, а не для Highload Production.

---

## 🚨 Runbook алертов

При срабатывании алертов в Prometheus/Grafana используйте следующие инструкции для диагностики:

- `Bank HTTP 5xx ratio high`

  Проверить приложение из label `application`, последние HTTP 5xx и связанные логи/трейсы. Сопоставить рост ошибок с недоступностью зависимостей (БД, Kafka, соседние сервисы).
- `Bank HTTP p95 latency high`

  Проверить медленные URI, загрузку JVM (GC pauses, memory) и внешние HTTP/JDBC-вызовы приложения через Zipkin.
- `Bank withdrawal failures high`

  Проверить причины отказов снятия (недостаточно средств, блокировки) и исключить массовые невалидные запросы (потенциальный фрод).

- `Bank transfer failures high`

  Проверить причины отказов переводов и доступность смежных сервисов: `account-service`, `exchange-service` и `blocker-service`.

- `Bank notification delivery failed`

  Проверить топик DLT, consumer lag в Kafka и ошибку окончательной обработки в `notification-service`.

---

## 🐳 Локальный запуск через Docker Compose

### Переменные окружения

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

### Тестовые пользователи Keycloak

| Логин     | Пароль    | Роли                                                            |
|-----------|-----------|-----------------------------------------------------------------|
| `dmitry`  | `dmitry`  | `USER, ACCOUNT_READ, ACCOUNT_WRITE, CASH_WRITE, TRANSFER_WRITE` |
| `alexey`  | `alexey`  | `USER, ACCOUNT_READ, ACCOUNT_WRITE, CASH_WRITE, TRANSFER_WRITE` |
| `elena`   | `elena`   | `USER, ACCOUNT_READ, ACCOUNT_WRITE, CASH_WRITE, TRANSFER_WRITE` |

### Запуск среды

#### 1. Сборка всех сервисов

```bash
./gradlew clean bootJar
```

#### 2. Запуск полной инфраструктуры + всех сервисов

```bash
docker compose --profile app --env-file .env.my-bank up --build -d
```

#### 3. Проверка статуса

```bash
docker compose --profile app ps
```

#### 4. Просмотр логов

```bash
docker compose --profile app logs -f
```

#### 5. Остановка

```bash
docker compose --profile app down
```

#### 6. Сброс локальных данных

```bash
docker compose down --volumes
```

---

## ☸ Подготовка Kubernetes

Для строгой изоляции инфраструктуры локальная среда разделена на два независимых кластера Kind: `Test` и `Prod`. Скрипт автоматически устанавливает Gateway API CRD и NGINX Gateway Fabric в целевой кластер.

#### Создание тестового контура:

```bash
powershell -ExecutionPolicy Bypass -File .\kubernetes\scripts\kind-bootstrap.ps1 -ClusterName test
```

#### Создание продуктового контура:

```bash
powershell -ExecutionPolicy Bypass -File .\kubernetes\scripts\kind-bootstrap.ps1 -ClusterName prod
```

Переключение между кластерами локально: `kubectl config use-context kind-test` или `kubectl config use-context kind-prod`.

---

## 📦 Helm структура

Проект использует зонтичный Helm chart (`my-bank`), объединяющий микросервисы и инфраструктуру:

```bash
├── charts/
│   ├── keycloak
│   ├── postgresql
│   ├── kafka
│   ├── kibana
│   ├── logstash
│   ├── spring-service
│   └── zipkin
├── my-bank/
├── values/
│      └── services/
│             ├── account-service.yaml
│             └── ...
└── scripts/
```

---

## 🔐 Управление секретами и SOPS

Секреты зашифрованы с использованием SOPS и age. Расшифровка выполняется строго внутри пайплайна.

```bash
envs/secrets/
│
├── values-secrets-dev.enc.yaml
├── values-secrets-test.enc.yaml
├── values-secrets-prod.enc.yaml
└── my-bank-realm-realm.enc.json
```

### Создание и шифрование секретов (SOPS)

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
  BANK_SERVICES_FRONT_UI_SERVICE_CLIENT_SECRET: "__FRONT_UI_SERVICE_CLIENT_SECRET__"
  BANK_SERVICES_ACCOUNT_SERVICE_CLIENT_SECRET: "__ACCOUNT_SERVICE_CLIENT_SECRET__"
  BANK_SERVICES_CASH_SERVICE_CLIENT_SECRET: "__CASH_SERVICE_CLIENT_SECRET__"
  BANK_SERVICES_TRANSFER_SERVICE_CLIENT_SECRET: "__TRANSFER_SERVICE_CLIENT_SECRET__"
  BANK_SERVICES_NOTIFICATION_SERVICE_CLIENT_SECRET: "__NOTIFICATION_SERVICE_CLIENT_SECRET__"
  BANK_SERVICES_EXCHANGE_GENERATOR_SERVICE_CLIENT_SECRET: "__EXCHANGE_GENERATOR_SERVICE_CLIENT_SECRET__"

postgresqlCredentials:
  password: "__POSTGRESQL_PASSWORD__"

keycloakCredentials:
  adminUsername: "admin"
  adminPassword: "__KEYCLOAK_ADMIN_PASSWORD__"

grafanaCredentials:
  adminUsername: "admin"
  adminPassword: "__GRAFANA_ADMIN_PASSWORD__"
```

---

## 🔄 Запуск Jenkins CI/CD

Jenkins запускается вне Kubernetes через Docker:

```bash
powershell -ExecutionPolicy Bypass -File .\jenkins\scripts\start-jenkins.ps1
```

После запуска интерфейс доступен по адресу: http://localhost:8090

### Создание учетных данных в Jenkins

Для работы Jenkins Pipeline необходимо зайти в `Manage Jenkins -> Credentials` и создать 4 глобальных секрета:

#### `my-bank-registry-credentials`
- тип: `Username with password`
- описание: Логин и токен от вашего хранилища образов. Используется на стадии Image push для отправки собранных микросервисов в реестр.

#### `my-bank-sops-age-key`
- тип: `Secret file`
- описание: Ваш приватный ключ `age` (обычно `keys.txt`), который используется на стадиях Prepare test secrets и Prepare prod secrets для расшифровки файлов `*.enc.yaml`

#### `my-bank-kubeconfig-test`
- тип: `Secret file`
- описание: Сгенерированный скриптом файл jenkins-kubeconfig-test.yaml

```bash
powershell -ExecutionPolicy Bypass -File .\kubernetes\scripts\create-jenkins-kubeconfig.ps1 -ClusterName test
```

#### `my-bank-kubeconfig-prod`
- тип: `Secret file`
- описание: Сгенерированный скриптом файл jenkins-kubeconfig-prod.yaml

```bash
powershell -ExecutionPolicy Bypass -File .\kubernetes\scripts\create-jenkins-kubeconfig.ps1 -ClusterName prod
```

### ⚙️ Настройка Umbrella Pipeline

Пайплайн позволяет собрать все образы, прогнать тесты инфраструктуры, развернуть зонтичный чарт в кластер `test` и (после ручного аппрува) в` prod`.

#### 1. Создать Pipeline: `New Item -> Pipeline`

#### 2. Выбрать в `Definition`: `Pipeline script from SCM`

#### 2. Выбрать в SCM: `Git`

#### 3. Выбрать в `Repository URL`:

```bash
https://github.com/Adam-and-Eve/my-bank-back-app.git
```

#### 4. Выбрать в `Branch Specifier (blank for 'any')`:

```bash
*/module_three_sprint_twelve_branch
```

#### 5. Сохранить изменения

#### 6. Перейти в `Build with Parameters`

#### 7. Указать в `IMAGE_REGISTRY`:

```bash
docker.io/<docker-login>
```

#### 8. Указать в `IMAGE_TAG`: `Имя тега`

#### 9. Выбрать параметры:
- `BUILD_IMAGES`
- `PUSH_IMAGES`
- `DEPLOY_TEST`
- `DEPLOY_PROD`

#### 10. Запустить: `Build`

#### 11. Проверить развертывание:

##### Проверить состояние ресурсов в тестовом кластере:

```bash
kubectl --context kind-test get all,ingress,httproute --namespace test
```

```bash
kubectl --context kind-test port-forward -n test svc/my-bank-gateway-nginx 8080:80
```

URL: http://localhost:8080

##### Проверить состояние ресурсов в production-кластере:

```bash
kubectl --context kind-prod get all,ingress,httproute --namespace prod
```

```bash
kubectl --context kind-prod port-forward -n prod svc/my-bank-gateway-nginx 8080:80
```

URL: http://localhost:8080

##### Проверить состояние ресурсов в production-кластере:

```bash
kubectl --context kind-prod get all,ingress,httproute --namespace prod
```

#### 12. Удалить и очистить:

##### Удалить приложение из тестового кластера:

```bash
helm uninstall my-bank --kube-context kind-test --namespace test
```

##### Удалить приложение из production-кластера:

```bash
helm uninstall my-bank --kube-context kind-prod --namespace prod
```

##### Удалить кластеры:

```bash
kind delete cluster --name test
```

```bash
kind delete cluster --name prod
```

##### Остановить Jenkins:

```bash
powershell -ExecutionPolicy Bypass ` -File .\jenkins\scripts\stop-jenkins.ps1
```

---

# 🩺 Доступ к интерфейсам Observability

Интерфейсы инфраструктуры не торчат наружу и доступны только через локальный port-forward (пример для тестового кластера `test`):

### Zipkin (Трассировка)

```bash
kubectl --context kind-test port-forward -n test service/zipkin 9411:9411
```

URL: http://localhost:9411

### Grafana (Метрики и Дашборды)

```bash
kubectl --context kind-test port-forward -n test service/grafana 3000:80
```

URL: http://localhost:3000

### Kibana (Логи)

```bash
kubectl --context kind-test port-forward -n test service/kibana 5601:5601
```

URL: http://localhost:5601

### Prometheus (Алерты и сырые метрики)

```bash
kubectl --context kind-test port-forward -n test service/prometheus-operated 9090:9090
```

URL: http://localhost:9090

---

## Kubernetes Deployment вручную

### Обновление зависимостей:

```bash
helm dependency update helm/my-bank
```

### Проверка:

```bash
helm lint helm/my-bank
```

### Рендер:

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

### Установка:

```bash
helm upgrade --install my-bank . `
  -n test `
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

### Проверка:

```bash
kubectl --context kind-test get pods -n test
```

```bash
kubectl --context kind-test port-forward -n test svc/my-bank-gateway-nginx 8080:80
```

http://localhost:8080

---

## 🔧 Как внести изменения

- **Создайте новую ветку: git checkout -b feature/название**
- **Внесите изменения**
- **Запустите тесты: ./gradlew test**
- **Соберите проект: ./gradlew clean bootJar**
- **Создайте Pull Request**