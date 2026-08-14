# MHC Dashboard Backend

Spring Boot 3 + Java 21 backend for the MHC Assistant Programmer Dashboard.

## Stack
- Java 21 + Spring Boot 3.2
- PostgreSQL (via Flyway migrations)
- JWT Authentication + Spring Security
- Deployed on Render (Docker)

## Local Development

```bash
./mvnw spring-boot:run
```

## Environment Variables (Render)
| Variable | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL connection string |
| `JWT_SECRET` | Secret key for JWT signing |
| `APP_BOOTSTRAP_USERNAME` | Initial admin username |
| `APP_BOOTSTRAP_PASSWORD` | Initial admin password |
| `ALLOWED_ORIGINS` | Frontend URL for CORS (e.g. https://yourapp.netlify.app) |
| `SPRING_PROFILES_ACTIVE` | Set to `prod` |
