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
   - `identity-service` on `http://localhost:8081`
   - `request-service` on `http://localhost:8083`
4. Run app on emulator.

The app uses `http://10.0.2.2:8083/` to access backend from emulator.

## Current API integration

- `POST /api/v1/auth/register` (register)
- `POST /api/v1/auth/login` (login)
- `POST /api/v1/requests` (create request)
- `POST /api/v1/requests/{id}/cancel` (cancel request)

Request-service auth headers are passed from logged in user session:

- `X-User-Id`
- `X-User-Role`
