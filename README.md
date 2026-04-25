# dobrovichek

## Request Service

1. Start PostgreSQL:

```powershell
docker compose up -d request-service-postgres
```

2. Run `ru.dobrovichek.request.RequestServiceApplication` from IntelliJ IDEA.

3. Open Swagger UI:

`http://localhost:8083/swagger-ui.html`

4. Open OpenAPI JSON:

`http://localhost:8083/v3/api-docs`

Default local database settings:

- host: `localhost`
- port: `5433`
- database: `dobrovichek_request`
- username: `dobrovichek`
- password: `dobrovichek`

If needed, override them with environment variables:

- `REQUEST_DB_HOST`
- `REQUEST_DB_PORT`
- `REQUEST_DB_NAME`
- `REQUEST_DB_USERNAME`
- `REQUEST_DB_PASSWORD`

## Android App (Ward MVP)

Android app prototype is located in `apps/android`.

Open `apps/android` as a standalone Gradle project in Android Studio and run it on an emulator.
The app is preconfigured to call request-service at `http://10.0.2.2:8083/`.
