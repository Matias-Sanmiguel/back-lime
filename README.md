# Lime

API REST de un marketplace inmobiliario (estilo Argenprop): publicar avisos, buscarlos y recibir consultas.
Backend en Spring Boot 4 + JPA + SQL Server; autenticación JWT.
Alcance TPO: CRUD de propiedades, ciclo publish/pause, imágenes, inquiries e inbox del dueño.
Stack: Java 21, Maven, Spring Data JPA, Lombok, Spring Security.
Base: `http://localhost:8080/api/v1`. Repo: `back-lime`.

## Aspectos técnicos

| Capa | Stack |
|------|-------|
| Backend | Java 21 · Spring Boot · JPA · Security (JWT) |
| Base de datos | SQL Server 2022 (Docker) · H2 en tests |

Arquitectura en capas: `controller` → `service` (`@Transactional`) → `repository` (`JpaRepository`) → `model` (`@Entity`) + DTOs.

Entidades: `User`, `Property`, `PropertyImage`, `Inquiry`, `DenylistedToken`.

## Quick start

```bash
docker compose up --build
# API: http://localhost:8080
```

Solo DB + API en host (JDK 21):

```bash
docker compose up db db-init -d
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk   # ajustar al path local
./mvnw spring-boot:run
```

## API (resumen)

```text
POST   /api/v1/auth/register|login|logout
GET|PATCH /api/v1/me
GET|POST /api/v1/properties
GET|PATCH|DELETE /api/v1/properties/{id}
POST   /api/v1/properties/{id}/publish|pause
POST|PATCH|DELETE /api/v1/properties/{id}/images...
POST   /api/v1/properties/{id}/inquiries
GET|PATCH /api/v1/me/inquiries...
GET    /uploads/**
```

Detalle: [`docs/endpoints.md`](docs/endpoints.md).

### Por qué `PATCH` y no `PUT`

Usamos **`PATCH`** (actualización parcial) en lugar de **`PUT`** (reemplazo total del recurso):

- En avisos y perfil (`/properties/{id}`, `/me`, imágenes, marcar consulta leída) el cliente suele enviar **solo los campos que cambian**, no el documento completo.
- Un `PUT` semántico exigiría reenviar todo el body; omitir un campo podría interpretarse como borrarlo o forzar defaults. Con `PATCH` + DTOs parciales (`UpdatePropertyRequest`, `UpdateMeRequest`) eso no pasa.
- Cumple REST moderno (RFC 5789): `GET` lectura, `POST` alta/acciones, `PATCH` modificación, `DELETE` baja. El verbo de escritura existe; elegimos el que matchea el caso de uso inmobiliario.

## Estructura

```text
back-lime/
├── src/           # Spring Boot
├── docs/
├── compose.yaml
├── Dockerfile
└── pom.xml
```
