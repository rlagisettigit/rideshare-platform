# Waypoint — Ride-Sharing Platform

A scalable ride-sharing platform skeleton: drivers publish rides, passengers discover and
book seats along any valid pickup/drop-off segment of a route. Built against the SRS covering
registration, driver/vehicle KYC, ride publishing with H3-indexed route search, atomic seat
booking, chat, reviews, notifications, and an admin/reporting surface.

## What this is

A **full backend module skeleton** (Spring Boot) + **MySQL schema** + **React frontend**,
scoped to be genuinely useful as a foundation to build on — not a finished, load-tested
production system. Core flows (auth, ride publish + H3 routing, search, atomic booking) have
real logic. Peripheral modules (chat, reviews, admin, reporting, payments) are scaffolded with
working CRUD/data-shapes and clearly marked integration points (`TODO`) where you'd plug in a
real map/payment/SMS provider.

## Stack

- **Backend**: Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, Flyway,
  Redis, Kafka, Uber H3 (spatial indexing), springdoc-openapi
- **DB**: MySQL 8
- **Frontend**: React 18 + Vite, react-router, axios
- **Infra**: Docker Compose (MySQL, Redis, Kafka, Kafka UI)

## Project layout

```
rideshare-platform/
├── backend/                   Spring Boot app (Maven)
│   └── src/main/java/com/rideshare/platform/
│       ├── auth/               registration, login (email+password, mobile+OTP, Google/Apple stub), JWT
│       ├── user/                profile management
│       ├── driver/              KYC onboarding, availability
│       ├── vehicle/             registration + approval
│       ├── ride/                 ride publish/cancel + validation
│       ├── route/                H3Service, RouteService, RoutingProvider (Mappls abstraction)
│       ├── search/               H3-backed ride search + ranking
│       ├── booking/              atomic seat allocation, cancellation
│       ├── notification/         Push/SMS/Email/In-App dispatch + Kafka event listeners
│       ├── chat/                  STOMP/WebSocket passenger<->driver chat
│       ├── review/                ratings + async average recalculation
│       ├── payment/ wallet/ coupon/   idempotent payment stub, wallet ledger, coupons
│       ├── admin/                 dashboard, driver/vehicle review, configuration
│       ├── reporting/             daily rides, completion %, peak hours, driver earnings
│       ├── audit/                 @Audited AOP aspect + immutable audit_logs
│       ├── security/ config/      JWT, Redis, Kafka, OpenAPI, CORS
│       └── common/                ApiResponse envelope, standardized error format
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/V1__init.sql     full schema incl. all indexes from the SRS
├── frontend/                  React + Vite app
│   └── src/{api,context,components,pages}
└── docker-compose.yml         MySQL + Redis + Kafka + Kafka UI for local dev
```

## Running it locally

### 1. Start infrastructure

```bash
docker compose up -d
```

This brings up MySQL (`localhost:3306`, db `rideshare` / user `rideshare` / pass `rideshare`),
Redis (`localhost:6379`), Kafka (`localhost:9092`), and Kafka UI (`localhost:8081`).

### 2. Run the backend

```bash
cd backend
mvn spring-boot:run
```

Flyway applies `V1__init.sql` automatically on startup. The API is at `http://localhost:8080`,
Swagger UI at `http://localhost:8080/swagger-ui.html`.

Set `MAPPLS_API_KEY` as an env var to use the real Mappls Directions API; without it,
`MapplsRoutingProvider` falls back to a deterministic straight-line route generator so the
full H3 → search → booking pipeline is exercisable without live map credentials.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Opens at `http://localhost:5173`, proxying `/api/v1/*` to the backend.

## Design choices worth knowing about

- **Route indexing (Section 7)**: on publish, the ride's polyline is decoded into ordered
  waypoints, each converted to an H3 cell (`app.h3.resolution`, default 9), persisted to
  `ride_route_points`, and indexed into Redis as `route_index:{h3Cell} -> Set<rideId>`.
- **Search (Section 8)**: candidate rides are shortlisted via a `gridDisk` lookup around the
  passenger's pickup H3 cell (O(1) per cell) before applying the full validation/ranking
  pipeline — this is what keeps search off a full table scan.
- **Booking (Section 9)**: seat decrement happens inside a transaction against a
  `PESSIMISTIC_WRITE`-locked `Ride` row, so concurrent booking requests can't oversell seats.
- **Error format (Section 21)** and **correlation/trace IDs (Section 20)** match the SRS
  exactly — see `common/exception/GlobalExceptionHandler.java` and `common/CorrelationIdFilter.java`.
- **Audit (Section 18)**: `@Audited` is a marker annotation + AOP aspect; wire it onto the
  login/ride-publish/booking/payment/refund/admin-action service methods it's designed for.

## What's intentionally left as an integration point

- **Map provider**: `RoutingProvider` interface + `MapplsRoutingProvider` — swap in a real
  Mappls/Google/OSRM HTTP call and polyline decoder without touching `RouteService`.
- **Payment Gateway, SMS, Email, Push**: service classes exist (`PaymentService`,
  `NotificationDispatchService`) with a single call-site to wire a real SDK behind.
- **Google/Apple Sign-In**: `AuthService.loginWithSocialProvider` throws a clear "not
  configured" error until ID-token verification is wired in.
- Admin sub-modules (Support Tickets, Coupons UI, full Reports UI) have backend data models
  and endpoints but no dedicated frontend screens yet — `AdminDashboard.jsx` covers the
  summary tile only.

## Not included (be aware before treating this as production-ready)

Load testing against the stated P95 targets (Section 24), production secrets management,
rate limiting middleware, CI/CD, observability/tracing wiring beyond correlation IDs in logs,
and end-to-end tests. The backend was written but not compiled in this environment (no Maven/
network access here) — run `mvn compile` locally first and treat that as your starting point
for a review pass, not a guarantee it's warning-free.
