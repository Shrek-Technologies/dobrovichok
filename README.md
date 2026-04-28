# Добровичок
Мобильное приложение, связывающее тех, кому нужна помощь с теми, кто готов помочь.

## Функциональность

### Нуждающийся в помощи
- Регистрация и авторизация
- Создание заявок на помощь
- Просмотр статуса заявки
- Push-уведомления по изменениям в заявке

### Волонтер
- Регистрация и авторизация
- Просмотр актуальных заявок на карте
- Отклик на заявку и принятие в работу
- Рейтинг и истории выполненных заявок

## Технологический стек
- Backend: Java 17, Spring Boot, Spring Security, Spring Cloud Gateway
- Архитектура: микросервисы (Identity Service, User Service, Request Service, Notification Service)
- БД и миграции: PostgreSQL, Liquibase
- Интеграции: REST (HTTP/HTTPS), RabbitMQ (AMQP)
- Мобильный клиент: Android (Kotlin, Jetpack Compose)
- Карты и геолокация: Yandex MapKit
- Push-уведомления: Firebase Cloud Messaging (FCM)
- Сборка и окружение: Gradle (Kotlin DSL), Docker Compose

## Запуск
### Backend

1. Инфраструктура (PostgreSQL, RabbitMQ):

```powershell
docker compose up -d
```

2. При необходимости переопределить переменные окружения:
- `JWT_SECRET`
- `INTERNAL_API_TOKEN`
- `REQUEST_DB_HOST`, `REQUEST_DB_PORT`, `REQUEST_DB_NAME`, `REQUEST_DB_USERNAME`, `REQUEST_DB_PASSWORD`
- `USER_DB_HOST`, `USER_DB_PORT`, `USER_DB_NAME`, `USER_DB_USERNAME`, `USER_DB_PASSWORD`
- `RABBITMQ_PORT` (по умолчанию `5672`)

3. Поднять сервисы:
- `IdentityServiceApplication`
- `UserServiceApplication`
- `RequestServiceApplication`
- `NotificationServiceApplication`
- `ApiGatewayApplication`

4. Swagger:
- общий Swagger через gateway: `http://localhost:8080/swagger-ui.html`

## Android App

Клиент находится в `apps/android`.
> [!IMPORTANT]
> Не забудьте указать API-ключ Yandex MapKit и адрес backend-сервера в `apps/android/local.properties`.