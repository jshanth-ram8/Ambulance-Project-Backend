# Ambulance System: Backend (Spring Boot API)

REST API for the Emergency Ambulance Booking & Tracking System. SQLite by default, MySQL optional.
The frontend is a separate project (its own repo/zip) that talks to this API over HTTPS. It needs this API's public URL as its `BACKEND_URL`.

## Environment variables

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `FRONTEND_URL` | **yes (production)** | `http://localhost:3000` | Origin(s) allowed by CORS. Comma-separated for several. |
| `COOKIE_SAME_SITE` | production, cross-domain | `lax` | Set to `none` when frontend and backend are on different domains. |
| `COOKIE_SECURE` | production, cross-domain | `false` | Set to `true` (required with `none`). Needs HTTPS. |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | recommended | `admin@ambulance.com` / `admin123` | First admin account. Change them. |
| `SPRING_PROFILES_ACTIVE` | optional | (SQLite) | `mysql` to use MySQL |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | with `mysql` | local MySQL | MySQL connection |
| `PORT` | no | `8080` | Usually set by the host |

See `.env.example`.

## Run locally

Needs Java 17 and Maven.

```bash
mvn spring-boot:run
```

API on http://localhost:8080 (check http://localhost:8080/health). With the defaults it accepts requests from a frontend
at `http://localhost:3000`, which is where the frontend's `npm run dev` serves it.
SQLite creates `ambulance.db` in the folder you run from. Delete it to start fresh.

### MySQL instead

```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL="jdbc:mysql://localhost:3306/ambulance_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
export DB_USER=root
export DB_PASSWORD=your-password
mvn spring-boot:run
```

## Docker

```bash
docker build -t ambulance-backend .
docker run -p 8080:8080 \
  -e FRONTEND_URL=http://localhost:3000 \
  ambulance-backend
```

## Deploy (e.g. Render, Railway, Fly.io)

1. Create a Web Service from this repo, runtime Docker (or use the included `render.yaml` as a Blueprint).
2. Set the environment variables above. In production that means at least:
   `FRONTEND_URL`, `COOKIE_SAME_SITE=none`, `COOKIE_SECURE=true`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`
   (plus the MySQL variables if you want data to survive restarts).
3. Health check path: `/health`.

You get a public URL like `https://ambulance-backend.onrender.com`. That is the `BACKEND_URL` for the frontend.
If you deploy the backend before the frontend, put any placeholder in `FRONTEND_URL`, then update it once the
frontend has its URL and redeploy.

### Cookies across domains (important)

Login uses a session cookie. When the frontend and backend live on different domains, the cookie is a
"third-party" cookie, so it must be `SameSite=None; Secure` (that's what the two cookie variables do), and the
frontend sends requests with `credentials: 'include'`.

Some browsers block third-party cookies no matter what the server sets: **Safari (macOS and every iOS browser),
Firefox with strict tracking protection, and private/incognito windows in Chrome.** If the frontend and backend
sit on unrelated domains (e.g. `*.vercel.app` and `*.onrender.com`), login will appear to succeed and then
bounce back to the login page in those browsers.

The fix with no code changes is to host both on the **same parent domain** with custom domains, e.g.
`app.example.com` (frontend) and `api.example.com` (backend). They are then "same-site", cookies work in every
browser, and you can leave `COOKIE_SAME_SITE` at `lax`. (Keep `COOKIE_SECURE=true`.)

## How it works

- Patient books: the first free ambulance that has a driver is assigned and marked busy.
- Driver moves the trip forward one step at a time:
  Booking confirmed, Ambulance dispatched, Patient picked up, Trip completed.
  Completing the trip makes the ambulance free again.
- Patient can cancel until the patient is picked up. The ambulance is released.
- Login uses an `HttpSession` cookie. Passwords are stored as BCrypt hashes.
  `AuthInterceptor` enforces roles: `/api/admin/**` is ADMIN, `/api/driver/**` is DRIVER, `/api/bookings/**` is USER.
- `CorsConfig` is a servlet filter that allows the origins in `FRONTEND_URL` (with credentials) and answers the
  browser's preflight requests before the auth interceptor sees them.

## API

| Method | Path | Who |
|--------|------|-----|
| GET | /health | anyone |
| POST | /api/auth/register, /login, /logout | anyone |
| GET | /api/auth/me | logged in |
| GET | /api/hospitals | logged in |
| POST | /api/bookings | patient |
| GET | /api/bookings/my | patient |
| PUT | /api/bookings/{id}/cancel | patient |
| GET | /api/driver/me, /api/driver/bookings | driver |
| PUT | /api/driver/availability | driver |
| PUT | /api/driver/bookings/{id}/status | driver |
| GET | /api/admin/users, /ambulances, /bookings | admin |
| POST | /api/admin/drivers, /ambulances, /hospitals | admin |
| PUT | /api/admin/ambulances/{id}/driver | admin |
| DELETE | /api/admin/users/{id}, /ambulances/{id}, /hospitals/{id} | admin |

## Project layout

```
src/main/java/com/ambulance/
  config/      AppConfig, CorsConfig, AuthInterceptor, CurrentUser, DataSeeder
  controller/  Health, Auth, Hospital, Booking, Driver, Admin
  service/     BookingService (assignment and status rules)
  repository/  Spring Data JPA repositories
  model/       User, Hospital, Ambulance, Booking, Role, BookingStatus
  dto/         Request and response records
  exception/   ApiException, GlobalExceptionHandler
src/main/resources/
  application.properties, application-mysql.properties
```
