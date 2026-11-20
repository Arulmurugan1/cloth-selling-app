# cloth-selling-app

A cloth selling web app built as Spring Boot microservices with a React/Vite frontend. It includes Docker Compose for local runs, Kubernetes manifests, and OpenTelemetry, Prometheus, and Grafana observability.

## Architecture

| Component | Tech | Port | Role |
|---|---|---|---|
| `api-gateway` | Spring Boot 4.1, Spring Security, JWT | 8080 | Single entry point: auth, rate limiting, login attempt limits, session limits, routing to backend services |
| `order-service` | Spring Boot, PostgreSQL | 8081 | Orders; calls inventory over the internal API |
| `inventory-service` | Spring Boot, PostgreSQL, Spring Batch | 8082 | Stock management and CSV batch import |
| `frontend` | React 19, Vite, nginx | 5173 (Compose) | Web UI |
| PostgreSQL 17 / Redis 8 | Data and sessions | 5432 / 6379 | Shared infrastructure |
| OpenTelemetry, Jaeger, Prometheus, Grafana | Observability | 4317/4318, 16686, 9090, 3000 | Tracing, metrics, dashboards |

Services authenticate to each other with the `X-Internal-Key` header. Users authenticate with JWT tokens issued by the gateway.

## Prerequisites

- JDK 21
- Maven 3.9+
- Node.js 24+ and npm
- Docker Desktop (for the full stack)

## Configuration

Secrets are never stored in the repository. Copy the template and fill in real values:

```bash
cp .env.example .env
```

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | PostgreSQL password |
| `INTERNAL_API_KEY` | Shared key for service-to-service calls |
| `JWT_SECRET` | HMAC key used to sign JWTs |
| `SUPERADMIN_PASSWORD` | Password for the bootstrap super admin account (must be 4–12 characters, the login validation limit) |

`.env` is gitignored. Docker Compose fails fast if any of these are missing.

## Run with Docker Compose

```bash
docker compose up --build
```

Once running:

- Frontend: http://localhost:5173
- API gateway: http://localhost:8090 (container port 8080; host 8080 is often taken on Windows)
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686

The super admin account is `superadmin@clothapp.local`, with the password from `SUPERADMIN_PASSWORD`. It is created on first start of the gateway if it does not already exist.

## Run locally (without Docker)

Start PostgreSQL and Redis yourself, then export the variables from `.env` (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `INTERNAL_API_KEY`, `JWT_SECRET`, `APP_BOOTSTRAP_SUPERADMIN_PASSWORD`). Then:

```bash
# Backend (from the repo root)
mvn -pl api-gateway -am spring-boot:run
mvn -pl order-service -am spring-boot:run
mvn -pl inventory-service -am spring-boot:run

# Frontend
cd frontend
npm ci
npm run dev
```

## Build and test

```bash
# Backend: build and run all tests
mvn -B -U clean verify

# Frontend: production build (output in frontend/dist)
cd frontend
npm ci
npm run build
```

## Project layout

```
api-gateway/          Gateway, JWT auth, rate limiting, routing
order-service/        Order API and persistence
inventory-service/    Inventory API, CSV batch import
frontend/             React + Vite app and its Dockerfile
k8s/                  Kubernetes manifests (ConfigMap, workloads, secret template)
observability/        OpenTelemetry collector, Prometheus config
docker-compose.yml    Local full stack
Jenkinsfile           CI pipeline (Windows agent: backend build, frontend build, optional SonarQube)
```

## Kubernetes

Create the secret from your real values, not from the template:

```bash
kubectl create secret generic clothapp-secrets \
  --from-literal=DB_PASSWORD=... \
  --from-literal=INTERNAL_API_KEY=... \
  --from-literal=JWT_SECRET=... \
  --from-literal=APP_BOOTSTRAP_SUPERADMIN_PASSWORD=...
```

Then apply the manifests:

```bash
kubectl apply -f k8s/configmap.yaml -f k8s/workloads.yaml
```

`k8s/secret.example.yaml` contains `REPLACE_ME` placeholders and must not be applied as-is.

## CI

The `Jenkinsfile` builds the backend with `mvn -B -U clean verify`, builds the frontend with `npm ci` and `npm run build`, and optionally runs SonarQube analysis when `SONAR_HOST_URL` is set. It archives the jars and `frontend/dist`.

## Security notes

- Do not commit `.env`, real Kubernetes secrets, or keys.
- Rotate `JWT_SECRET`, `INTERNAL_API_KEY`, and `SUPERADMIN_PASSWORD` if they are exposed. Existing super admin rows keep their old password hash, so changing the variable only affects new installs.
