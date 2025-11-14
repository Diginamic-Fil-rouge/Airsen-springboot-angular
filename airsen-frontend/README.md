# Airsen Frontend

Angular 20 single-page application powering the Airsen dashboards, interactive map, forums and alert flows. The app speaks to the Spring Boot API at `http://localhost:8080/api/v1` (configurable via environment files).

---

## 1. Tech Stack

| Area | Stack |
| --- | --- |
| Framework | Angular 20, Angular Material, Angular CDK |
| Maps and Charts | Leaflet, leaflet.markercluster, Chart.js via `ng2-charts` |
| Tooling | Node.js 22, npm 10, Angular CLI, SCSS |
| Testing | Jasmine + Karma (unit), placeholder `ng e2e` target |
| Packaging | Angular CLI builder, multi-stage Dockerfile served by Nginx |

Project layout:

```
airsen-frontend/
├── src/app/                      # Feature modules, shared components
├── src/environments/             # Dev and prod configs
├── Dockerfile                    # Used by docker compose and prod builds
├── angular.json                  # CLI targets (development, production, docker)
└── package.json                  # Scripts and dependencies
```

---

## 2. Prerequisites

| Tool | Version | Notes |
| --- | --- | --- |
| Node.js | >= 22.0.0 | Use nvm or Volta to match `package.json` engines |
| npm | >= 10.0.0 | Bundled with Node 22 |
| Angular CLI | `npm install -g @angular/cli@20` (optional, local `ng` via npm scripts) |
| Backend API | Should be reachable at `http://localhost:8080/api/v1` during development |
| Docker (optional) | Used when running the `web` service from `docker-compose.yml` |

---

## 3. Environment Configuration

Angular uses TypeScript environment files:

- `src/environments/environment.ts` (development defaults)
- `src/environments/environment.prod.ts` (production overrides)

Key settings:

| Setting | Description |
| --- | --- |
| `apiUrl` | Base path to the backend API |
| `mapbox.accessToken` | Mapbox token (optional but recommended for premium tiles) |
| `features.enableMockData`, `features.enableAnalytics` | Feature flags |

When building inside Docker, `API_BASE_URL` is passed as a build-time environment variable via `docker-compose.yml`.

---

## 4. Install Dependencies

```bash
cd airsen-frontend
npm install
```

---

## 5. Local Development

1. **Start backend and databases** (from repository root):
   ```bash
   docker compose --profile backend up -d
   ```
   or run the backend manually following `airsen-backend/README.md`.

2. **Run Angular dev server**:
   ```bash
   cd airsen-frontend
   npm start          # alias for ng serve --configuration development
   # or with custom host/port
   ng serve --host=0.0.0.0 --port=4200
   ```

   Dev server runs on `http://localhost:4200` with live reload enabled, pointing at the API URL defined in `environment.ts`.

3. **Run via Docker (frontend only)**:
   ```bash
   docker compose --profile frontend up web
   ```
   The compose file mounts `src/` so code changes still trigger rebuilds. `CHOKIDAR_USEPOLLING=true` is already set for reliable file watching.

4. **Shared design tokens** live under `src/styles/design-system.scss`. Components default to the module-based pattern (`standalone: false`) and are grouped inside `src/app/features`.

---

## 6. Testing and Quality

| Command | Description |
| --- | --- |
| `npm test` | Runs Karma/Jasmine specs in Chrome (watch mode). For CI, add `-- --watch=false --browsers=ChromeHeadless`. |
| `npm run e2e` | Placeholder `ng e2e` target (configure Cypress, Playwright or WebDriver builder before use). |
| `npm run lint` | Calls `ng lint`, but Angular ESLint is not yet configured. Run `ng add @angular-eslint/schematics` and create lint targets before relying on this command. |

Unit test coverage output (if enabled) is written under `coverage/`.

---

## 7. Production Build and Deployment

### CLI build
```bash
npm run build        # ng build --configuration=production
# Outputs dist/airsen-frontend/
```

### Docker-specific build
```bash
npm run build:docker # ng build --configuration=docker
```
The `docker` configuration enforces stricter bundle budgets and disables source maps.

### Compose production stack
From repository root:
```bash
API_TARGET=prod WEB_TARGET=prod docker compose --profile prod up -d --build
```
This compiles the Angular app, serves it through Nginx, and runs the backend plus databases behind the reverse proxy.

### Standalone container
```bash
docker build -t airsen-web:prod -f Dockerfile .
docker run -p 8081:80 \
  -e API_BASE_URL=https://api.example.com/api/v1 \
  airsen-web:prod
```

---

## 8. Troubleshooting

| Issue | Resolution |
| --- | --- |
| CORS errors | Ensure backend `CORS_ALLOWED_ORIGINS` includes `http://localhost:4200` or configure a proxy. |
| Leaflet tiles missing | Provide a valid Mapbox access token or switch to open tile servers inside map services. |
| `npm run lint` complains about missing target | Initialize Angular ESLint via `ng add @angular-eslint/schematics`. |
| Dev server does not reload inside Docker | Use `ng serve --poll=2000` locally or rely on the compose profile where polling is preconfigured. |
| Production build runs out of memory | Increase Node heap: `NODE_OPTIONS=--max-old-space-size=4096 npm run build`. |

---

## 9. References

- `src/styles/design-system.scss` for typography, breakpoints, spacing, and color tokens.
- `docker-compose.yml` to see how the frontend container integrates with backend, databases, and Nginx.

Keep this README updated as ESLint, E2E tooling, or additional deployment targets are introduced.
