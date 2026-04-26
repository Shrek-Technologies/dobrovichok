# Dobrovichek Android (MVP)

Android client for the ward flow based on the provided references:

- home screen
- create request wizard
- searching volunteer screen
- volunteer found screen

## Run

1. Open `apps/android` as a separate project in Android Studio.
2. Sync Gradle.
3. Start backend:
   - `api-gateway` on `http://localhost:8080`
   - gateway routes to `identity-service` (8081), `user-service` (8082), `request-service` (8083)
4. Run app on emulator.

Эмулятор по умолчанию: `http://10.0.2.2:8080/` (только **api-gateway**).

В `apps/android/local.properties` при необходимости:

```properties
API_BASE_URL=http://192.168.x.x:8080/
```

Отдельные URL сервисов (8081/8082/8083) не задаются — маршрутизация на стороне gateway.

## Current API integration

- `POST /api/v1/auth/register` (register)
- `POST /api/v1/auth/login` (login)
- `POST /api/v1/requests` (create request)
- `GET /api/v1/requests/nearby` (volunteer map)
- `POST /api/v1/requests/{id}/accept` (volunteer accepts)
- `POST /api/v1/requests/{id}/cancel` (cancel request)

Request-service auth headers are passed from logged in user session:

- `X-User-Id`
- `X-User-Role`

## Yandex MapKit

Set your key in `apps/android/local.properties`:

```properties
sdk.dir=...
MAPKIT_API_KEY=d951c797-ef61-4754-8551-3af65d1798b0
```
