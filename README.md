# Template Spring Boot

A reusable Spring Boot web application template with RBAC (Role-Based Access Control), Thymeleaf theming, and PostgreSQL persistence.

## Tech Stack

- **Framework**: Spring Boot 3.3.2
- **Java**: 21
- **Database**: PostgreSQL
- **ORM**: MyBatis 3.0.3 + tk.mybatis 4.2.3
- **Migration**: Flyway
- **Template**: Thymeleaf 3.1 with Layout Dialect
- **Security**: Spring Security with form login

## Prerequisites

- Java 21+
- PostgreSQL (running on port 5432, or configure via `DATABASE_URL`)

## Setup

1. **Database** — create the database:

   ```sql
   CREATE DATABASE template_db;
   ```

2. **Configure** — default credentials are `postgres`/`postgres`. Override with environment variables:

   ```bash
   export DATABASE_URL=jdbc:postgresql://localhost:5432/template_db
   export DATABASE_USERNAME=your_user
   export DATABASE_PASSWORD=your_password
   ```

3. **Run migrations** — Flyway runs automatically on startup.

4. **Default admin account** — created by migration:

   | Username | Password |
   |----------|----------|
   | admin    | admin123 |

## Running

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

## Features

- **RBAC** — users, roles, permissions, and menus managed through the UI
- **Menu tree** — hierarchical sidebar navigation with role-based visibility
- **Soft delete** — all entities use soft delete with audit columns
- **Flyway migrations** — versioned database schema changes
- **Thymeleaf layout** — reusable fragments for header, sidebar, footer

## Project Structure

```
src/main/java/com/template/
  controller/    — Spring MVC controllers
  service/       — Business logic
  mapper/        — MyBatis mapper interfaces
  entity/        — JPA entities (tk.mybatis)
  dto/           — Request/response DTOs
  config/        — Spring configuration
  exception/     — Global exception handling
  util/          — Utility classes

src/main/resources/
  templates/     — Thymeleaf templates
  db/migration/  — Flyway SQL migrations
  static/        — CSS, JS
  mapper/        — MyBatis XML mappers
```

## Default Users

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | ADMIN |
| user     | user123   | USER  |

## License

MIT
