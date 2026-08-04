# Template Spring Boot

A reusable Spring Boot web application template with RBAC (Role-Based Access Control), **database-per-tenant** school management, Thymeleaf theming, and PostgreSQL persistence.

## Tech Stack

- **Framework**: Spring Boot 3.3.2
- **Java**: 21
- **Database**: PostgreSQL
- **ORM**: MyBatis 3.0.3 + tk.mybatis 4.2.3
- **Migration**: Flyway
- **Template**: Thymeleaf 3.1 with Layout Dialect
- **Security**: Spring Security with form login (school-code + username + password)

## Architecture — Database-Per-Tenant

Each school owns its own PostgreSQL database. A shared registry database holds
the school list and a global login index.

```
sims_registry   — school list (schools) + login index (school_users)
sims_<code>     — one database per school, auto-created on first use
sims_system     — dedicated master-admin realm controlling all schools
```

- **Login** takes a **school code + username + password**. A real school code
  (e.g. `baru`) routes to that school's database; the reserved code `system`
  logs into the master realm.
- **Onboarding**: creating a school through the `/schools` UI auto-creates and
  migrates its database and seeds an `admin` login, so it is immediately
  login-able.
- **Permissions**: school-management (`/schools`) and `SCHOOL_*` permissions
  exist only in the `system` realm, not in school databases.

## Prerequisites

- Java 21+
- PostgreSQL running on port 5432

## Setup

1. **Registry database** — create the registry database (Flyway creates the
   tables on startup):

   ```sql
   CREATE DATABASE sims_registry;
   ```

2. **Configure** — default credentials are `postgres`/`postgres`. Override with
   environment variables:

   ```bash
   # Registry DB (school list + login index)
   export REGISTRY_DATABASE_URL=jdbc:postgresql://localhost:5432/sims_registry
   export REGISTRY_DATABASE_USERNAME=postgres
   export REGISTRY_DATABASE_PASSWORD=postgres

   # School DBs — the app appends sims_<code> and creates them automatically
   export SCHOOL_DATABASE_URL_PREFIX=jdbc:postgresql://localhost:5432/
   export SCHOOL_DATABASE_ADMIN_URL=jdbc:postgresql://localhost:5432/postgres
   export SCHOOL_DATABASE_USERNAME=postgres
   export SCHOOL_DATABASE_PASSWORD=postgres
   ```

3. **Run migrations** — Flyway runs automatically on startup for the registry
   DB; each school DB is migrated when first created or accessed.

## Running

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

Or with Docker:

```bash
docker compose up --build
```

## Login

| School code | Username | Password | Scope |
|-------------|----------|----------|-------|
| `system`    | admin    | admin123 | Master realm — manage all schools |
| any school code (e.g. `baru`) | admin | admin123 | That school's own database |

## Features

- **RBAC** — users, roles, permissions, and menus managed through the UI
- **Database-per-tenant** — each school isolated in its own database
- **Auto-provisioning** — new school databases created + migrated on demand
- **Menu tree** — hierarchical sidebar navigation with role-based visibility
- **Soft delete** — all entities use soft delete with audit columns
- **Flyway migrations** — versioned database schema changes (registry, system,
  and per-school sets)
- **Thymeleaf layout** — reusable fragments for header, sidebar, footer

## Project Structure

```
src/main/java/com/template/
  controller/    — Spring MVC controllers
  service/       — Business logic
  mapper/        — MyBatis mapper interfaces (school realm)
  registry/      — Registry-realm mappers (schools, school_users)
  entity/        — JPA entities (tk.mybatis)
  dto/           — Request/response DTOs
  config/        — Spring configuration, routing DataSource, DB managers
  security/      — Authentication provider, user details, failure handler
  tenant/        — TenantContext + request filter
  exception/     — Global exception handling
  util/          — Utility classes

src/main/resources/
  templates/     — Thymeleaf templates
  db/migration/  — Flyway migrations for each school DB
  db/registry/   — Flyway migrations for the registry DB
  db/system/     — Flyway migrations for the master realm DB
  static/        — CSS, JS
  mapper/        — MyBatis XML mappers (school realm)
  registry-mapper/ — MyBatis XML mappers (registry realm)
```

## Tests

```bash
mvn test
```

The smoke test (`ApplicationSmokeTest`) boots the Spring context and verifies
the routing datasource resolves to the registry realm. It requires a running
PostgreSQL.

## License

MIT
