# API — Endpoints

Base: `http://localhost:8080` · Prefijo: `/api/v1` · JSON · Errores: [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457).

Auth: header `Authorization: Bearer <token>` cuando la fila dice JWT. El login/register devuelven el campo `token`.

Actualizado: **8 sep 2026**. Contrato v1 en `main` (`back-lime`): **20 REST + `GET /uploads/**`**.

> **PATCH vs PUT:** la API usa `PATCH` para actualizaciones parciales (avisos, perfil, imágenes, marcar consulta). Justificación en el [README](../README.md).

---

## Inventario

| # | Estado | Método | Ruta | Auth | Notas |
|---|--------|--------|------|------|-------|
| 1 | Hecho | `GET` | `/properties` | Público | Solo `PUBLISHED`; filtros `city`, `province`, `type`, `operation`, `minPrice`, `maxPrice`, `minBedrooms`, `minBathrooms` |
| 2 | Hecho | `POST` | `/properties` | JWT · `AGENCY`/`ADMIN` | Crea `DRAFT`; `owner` = usuario del token · **403** si `USER` |
| 3 | Hecho | `GET` | `/properties/{id}` | Mixto | Público solo `PUBLISHED`; dueño JWT ve `DRAFT`/`PAUSED` |
| 4 | Hecho | `DELETE` | `/properties/{id}` | JWT · `AGENCY`/`ADMIN` · dueño | Soft delete (`deletedAt`) · **204** |
| 5 | Hecho | `PATCH` | `/properties/{id}` | JWT · `AGENCY`/`ADMIN` · dueño | Actualización parcial |
| 6 | Hecho | `POST` | `/properties/{id}/publish` | JWT · `AGENCY`/`ADMIN` · dueño | Valida campos mínimos · **400** si incompleto |
| 7 | Hecho | `POST` | `/properties/{id}/pause` | JWT · `AGENCY`/`ADMIN` · dueño | |
| 8 | Hecho | `POST` | `/auth/register` | Público | Requiere `birthDate` + `sex` · **201** · **409** email dup · **400** `ADMIN` |
| 9 | Hecho | `POST` | `/auth/login` | Público | **200** + JWT · **401** password mala |
| 10 | Hecho | `POST` | `/auth/logout` | JWT | Denylist `jti` · **204** |
| 11 | Hecho | `GET` | `/me` | JWT | Perfil |
| 12 | Hecho | `PATCH` | `/me` | JWT | Nombre / agency / password (parcial) |
| 13 | Hecho | `GET` | `/me/properties` | JWT | Página de avisos propios + filtros |
| 14 | Hecho | `POST` | `/properties/{id}/images` | JWT · `AGENCY`/`ADMIN` · dueño | Multipart `file` (jpeg/png/webp) · **201** |
| 15 | Hecho | `PATCH` | `/properties/{id}/images/{imageId}` | JWT · `AGENCY`/`ADMIN` · dueño | Reemplaza archivo |
| 16 | Hecho | `DELETE` | `/properties/{id}/images/{imageId}` | JWT · `AGENCY`/`ADMIN` · dueño | **204** |
| 17 | Hecho | `POST` | `/properties/{id}/inquiries` | Público | Solo si aviso `PUBLISHED` · **201** · **409** si no |
| 18 | Hecho | `GET` | `/me/inquiries` | JWT | Página + `unreadCount`; query `unreadOnly`, `propertyId` |
| 19 | Hecho | `GET` | `/me/inquiries/{inquiryId}` | JWT | Detalle · **404** si no es del dueño |
| 20 | Hecho | `PATCH` | `/me/inquiries/{inquiryId}` | JWT | Body `{ "read": true }` · **400** si `false` |
| — | Hecho | `GET` | `/uploads/**` | Público | ResourceHandler (no es `@RestController`) |

### Capas

`Controller` → `Service` (`@Transactional`) → `Repository` (`@Repository` + `JpaRepository`) → `@Entity` + DTOs.

Inbox: `InquiryController` → `InquiryService`.

### Pantallas → endpoints

| Pantalla | Endpoints |
|----------|-----------|
| Home / Buscar | `GET /properties` |
| Ficha | `GET /properties/{id}`, `POST .../inquiries`, `GET /uploads/**` |
| Login / Registro | `POST /auth/login`, `POST /auth/register` |
| Header / F5 | `GET /me` |
| Logout | `POST /auth/logout` |
| Perfil | `PATCH /me` |
| Panel | `GET /me/properties`, publish, pause, `DELETE /properties/{id}` |
| Nuevo / editar aviso | `POST /properties`, `PATCH /properties/{id}`, images, publish |
| Consultas | `GET /me/inquiries`, `GET .../{id}`, `PATCH .../{id}` |

### Fuera de v1

Refresh token, cambiar email, `PUT` de aviso, favoritos, mapa, chat, admin, pagos.

---

## Convenciones

* Prefijo `/api/v1`. IDs `Long`. Fechas ISO-8601 UTC (`Instant`).
* Listados: `PageResponse<T>` (o `InquiryInboxResponse` en inbox), orden default `createdAt DESC`.
* Soft delete en avisos (`deletedAt`). Nunca hard delete de properties.
* JSON: `Content-Type: application/json`. Fotos: `multipart/form-data` campo `file`.
* JWT: campo respuesta `token`, `tokenType: Bearer`, `expiresIn: 86400` (HS256). Password: bcrypt.

### Errores (RFC 9457)

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "minPrice cannot be greater than maxPrice",
  "instance": "/api/v1/properties"
}
```

| Código | Cuándo |
|--------|--------|
| `400` | Validación, MIME inválido, publish incompleto, `read: false` |
| `401` | Sin token, token inválido/denylist, login fallido |
| `403` | Token OK pero no es el dueño |
| `404` | No existe, soft-deleted, DRAFT oculto a visitante, consulta ajena |
| `409` | Email repetido, inquiry sobre no-`PUBLISHED`, transición inválida |

### Quién puede pegarle

Público: `GET /properties`, `GET /properties/{id}` (solo publicados), `POST /auth/register`, `POST /auth/login`, `POST .../inquiries`, `GET /uploads/**`.

Todo lo demás: JWT. Mutaciones de aviso/imagen: además dueño (**403** si no).

---

## DTOs (resumen)

### PageResponse<T>

`content`, `page`, `size`, `totalElements`, `totalPages`

### InquiryInboxResponse

Igual que página + `unreadCount` (total no leídas del dueño).

### AuthResponse

`token`, `tokenType`, `expiresIn`, `user` (`UserResponse`)

### UserResponse

`id`, `email`, `name`, `role`, `agencyName` (opcional)

### RegisterRequest

`email`, `password`, `name`, `birthDate`, `sex` (`MALE`/`FEMALE`/`OTHER`), `role` opcional (`USER`/`AGENCY`; no `ADMIN`), `agencyName` si agency.

### PropertyResponse

Aviso + `owner` (`OwnerResponse`) + timestamps / `status`.

### InquiryResponse

`id`, `propertyId`, `propertyTitle`, `name`, `email`, `phone`, `message`, `readAt`, `createdAt`

### ImageResponse

`id`, `url`, `createdAt` (y campos de update según implementación)

### Enums

* **PropertyType:** `APARTMENT` · `HOUSE` · `LAND` · `COMMERCIAL` · `OTHER`
* **OperationType:** `SALE` · `RENT` · `TEMPORARY_RENT`
* **PropertyStatus:** `DRAFT` · `PUBLISHED` · `PAUSED`
* **UserRole:** `USER` · `AGENCY` · `ADMIN`
* **Sex:** `MALE` · `FEMALE` · `OTHER`

---

## Auth

### `POST /api/v1/auth/register` · público

**201** `AuthResponse` · **409** email · **400** `role=ADMIN` / validación

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"a@example.com","password":"password123","name":"Ana","birthDate":"1998-05-10","sex":"FEMALE"}'
```

### `POST /api/v1/auth/login` · público

**200** `AuthResponse` · **401** credenciales

### `POST /api/v1/auth/logout` · JWT

**204**

Roles en el token (`ROLE_USER` / `ROLE_AGENCY` / `ROLE_ADMIN`):

* **Público:** buscar avisos, ficha publicada, register, login, mandar consulta, `/uploads`.
* **Cualquier autenticado (`USER`, `AGENCY`, `ADMIN`):** `/me`, logout, inbox.
* **`AGENCY` o `ADMIN`:** crear/editar/borrar/publicar/pausar avisos y fotos. Un `USER` logueado recibe **403**.
* Además, el service chequea **dueño** (otro `AGENCY` no edita el aviso ajeno → **403**).

---

## Perfil

### `GET /api/v1/me` · JWT → **200** `UserResponse` · **401** sin token

### `PATCH /api/v1/me` · JWT

Body parcial: `name`, `agencyName`, y/o `currentPassword` + `newPassword`.

**200** · **403** password actual mala · **401** sin token

### `GET /api/v1/me/properties` · JWT

Paginado. Filtros opcionales: `city`, `province`, `type`, `operation`, `status`, precios, `minBedrooms`, `minBathrooms`.

---

## Properties

### `GET /api/v1/properties` · público

Solo **`PUBLISHED`** (aunque mandes `status`). Queries: `page`, `size`, `city`, `province`, `type`, `operation`, `minPrice`, `maxPrice`, `minBedrooms`, `minBathrooms`.

**200** `PageResponse<PropertyResponse>` · **400** `minPrice > maxPrice`

```bash
curl "http://localhost:8080/api/v1/properties?city=CABA&province=Buenos%20Aires&minBedrooms=2&operation=SALE"
```

### `POST /api/v1/properties` · JWT → **201** `DRAFT` + `Location`

### `GET /api/v1/properties/{id}` · mixto

Visitante: solo `PUBLISHED` (**404** si no). Dueño con JWT: ve sus `DRAFT`/`PAUSED`.

### `PATCH /api/v1/properties/{id}` · JWT dueño → **200** · **403** ajeno · **401** sin token

### `DELETE /api/v1/properties/{id}` · JWT dueño → **204**

### `POST /api/v1/properties/{id}/publish` · JWT dueño

Requiere title, price, currency, city, type, operation. **200** `PUBLISHED` · **400** incompleto · **403** ajeno

### `POST /api/v1/properties/{id}/pause` · JWT dueño → **200** `PAUSED` · **409** si ya pausada / transición inválida

---

## Imágenes

### `POST /api/v1/properties/{id}/images` · JWT dueño · multipart `file`

MIME: jpeg / png / webp. **201** `ImageResponse` · **400** MIME · **401**/**403**

### `PATCH /api/v1/properties/{id}/images/{imageId}` · JWT dueño · multipart `file` → **200**

### `DELETE /api/v1/properties/{id}/images/{imageId}` · JWT dueño → **204**

### `GET /uploads/{filename}` · público → archivo estático (**200**)

---

## Consultas (leads + inbox)

### `POST /api/v1/properties/{id}/inquiries` · público

Body: `name`, `email`, `phone?`, `message`. Solo si el aviso está **`PUBLISHED`**.

**201** `InquiryResponse` (incluye `propertyTitle`) · **409** si `DRAFT`/`PAUSED`

### `GET /api/v1/me/inquiries` · JWT

Queries: `page`, `size`, `unreadOnly` (default `false`), `propertyId` (debe ser propio · **404** si no).

**200** `InquiryInboxResponse` con `unreadCount`

### `GET /api/v1/me/inquiries/{inquiryId}` · JWT → **200** · **404** ajena · **401** sin token

### `PATCH /api/v1/me/inquiries/{inquiryId}` · JWT

```json
{ "read": true }
```

**200** + `readAt` · **400** si `read: false` · idempotente si ya estaba leída

```bash
curl -X PATCH http://localhost:8080/api/v1/me/inquiries/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"read":true}'
```
