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
