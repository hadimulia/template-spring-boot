# Spring Boot RBAC Template (Phase 1+2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a reusable Spring Boot web application template with a complete authentication and Role-Based Access Control (RBAC) system.

**Architecture:** A traditional layered architecture (Controller → Service → Mapper) with clear separation of concerns. MyBatis is used for data access, with tk.mybatis for basic CRUD and XML for complex queries. Security is handled by Spring Security, and views are rendered with Thymeleaf and Bootstrap 5.

**Tech Stack:**
- Java 21, Spring Boot 3.x
- Maven
- Thymeleaf, Bootstrap 5
- PostgreSQL
- MyBatis, tk.mybatis
- Spring Security, BCrypt
- Flyway

## Global Constraints

- **Java Version:** 21
- **Framework:** Spring Boot 3.x
- **Build Tool:** Maven
- **Database:** PostgreSQL
- **ORM:** MyBatis + tk.mybatis
- **Password Hashing:** BCrypt

---

### Task 1: Project Initialization and Dependencies

**Files:**
- Create: `pom.xml`

**Interfaces:**
- Produces: A configured Maven project with all necessary dependencies.

- [ ] **Step 1: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.2</version>
        <relativePath/>
    </parent>
    <groupId>com.template</groupId>
    <artifactId>template-spring-boot</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>template-spring-boot</name>
    <description>Reusable Spring Boot Web Application Template</description>
    <properties>
        <java.version>21</java.version>
        <mybatis.version>3.0.3</mybatis.version>
        <tk.mybatis.version>4.2.3</tk.mybatis.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.thymeleaf.extras</groupId>
            <artifactId>thymeleaf-extras-springsecurity6</artifactId>
        </dependency>
        <dependency>
            <groupId>nz.net.ultraq.thymeleaf</groupId>
            <artifactId>thymeleaf-layout-dialect</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>${mybatis.version}</version>
        </dependency>
        <dependency>
            <groupId>tk.mybatis</groupId>
            <artifactId>mapper-spring-boot-starter</artifactId>
            <version>${tk.mybatis.version}</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter-test</artifactId>
            <version>${mybatis.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Run `mvn clean install` to download dependencies**

Run: `mvn clean install`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "feat: initialize Maven project and add dependencies"
```
---

### Task 2: Application Configuration

**Files:**
- Create: `src/main/java/com/template/TemplateApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-dev.yml`

- [ ] **Step 1: Create main application class**

```java
// src/main/java/com/template/TemplateApplication.java
package com.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.template.mapper")
public class TemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }

}
```

- [ ] **Step 2: Create main configuration file (`application.yml`)**

```yaml
# src/main/resources/application.yml
spring:
  application:
    name: template-spring-boot
  profiles:
    active: dev
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/template_db}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  thymeleaf:
    cache: false
    encoding: UTF-8
    mode: HTML
    prefix: classpath:/templates/
    suffix: .html
  security:
    user:
      name: admin
      password: admin123
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.template.entity
  configuration:
    map-underscore-to-camel-case: true
mapper:
  mappers: tk.mybatis.mapper.common.Mapper
  not-empty: false
  identity: POSTGRES
logging:
  level:
    root: INFO
    com.template: DEBUG
```

- [ ] **Step 3: Create development profile configuration (`application-dev.yml`)**

```yaml
# src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/template_db
    username: postgres
    password: postgres
logging:
  level:
    org.springframework.web: DEBUG
    com.template.mapper: TRACE
    org.springframework.security: DEBUG
```

- [ ] **Step 4: Run the application**

Run: `mvn spring-boot:run`
Expected: Application starts but fails to connect to the database (which is expected).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/template/TemplateApplication.java src/main/resources/application.yml src/main/resources/application-dev.yml
git commit -m "feat: add main application class and configuration files"
```
---

### Task 3: Database Schema with Flyway

**Files:**
- Create: `src/main/resources/db/migration/V1__create_base_tables.sql`
- Create: `src/main/resources/db/migration/V2__create_rbac_tables.sql`
- Create: `src/main/resources/db/migration/V3__insert_initial_data.sql`

- [ ] **Step 1: Create base tables migration**

```sql
-- src/main/resources/db/migration/V1__create_base_tables.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fullname VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    enabled BOOLEAN DEFAULT true,
    account_locked BOOLEAN DEFAULT false,
    login_attempts INT DEFAULT 0,
    last_login TIMESTAMP,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);
CREATE TABLE menus (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES menus(id),
    name VARCHAR(50) NOT NULL,
    url VARCHAR(255),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    visible BOOLEAN DEFAULT true,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);
```

- [ ] **Step 2: Create RBAC junction tables migration**

```sql
-- src/main/resources/db/migration/V2__create_rbac_tables.sql
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);
CREATE TABLE role_menus (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    menu_id BIGINT NOT NULL REFERENCES menus(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, menu_id)
);
```

- [ ] **Step 3: Create initial data migration**

```sql
-- src/main/resources/db/migration/V3__insert_initial_data.sql
INSERT INTO users (username, password, fullname, email, enabled, created_by)
VALUES ('admin', '$2a$10$i8.1.j4j.iZp2GkL7c5y8uJndnZJ0Z0mF/b.yH/y.xY1c2W5N6OqG', 'System Administrator', 'admin@example.com', true, 'system');
INSERT INTO roles (name, description, created_by) VALUES ('ADMIN', 'System Administrator', 'system'), ('USER', 'Regular User', 'system');
INSERT INTO permissions (code, description, created_by) VALUES ('USER_VIEW', 'View users', 'system'), ('USER_CREATE', 'Create user', 'system'), ('USER_EDIT', 'Edit user', 'system'), ('USER_DELETE', 'Delete user', 'system'), ('ROLE_VIEW', 'View roles', 'system'), ('ROLE_CREATE', 'Create role', 'system'), ('ROLE_EDIT', 'Edit role', 'system'), ('ROLE_DELETE', 'Delete role', 'system'), ('MENU_VIEW', 'View menus', 'system'), ('MENU_CREATE', 'Create menu', 'system'), ('MENU_EDIT', 'Edit menu', 'system'), ('MENU_DELETE', 'Delete menu', 'system');
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by) VALUES (NULL, 'Dashboard', '/dashboard', 'bi-speedometer2', 1, true, 'system'), (NULL, 'Master', NULL, 'bi-folder', 2, true, 'system'), (2, 'User', '/users', 'bi-person', 1, true, 'system'), (2, 'Role', '/roles', 'bi-shield', 2, true, 'system'), (2, 'Menu', '/menus', 'bi-list', 3, true, 'system');
INSERT INTO role_permissions (role_id, permission_id, created_by) SELECT r.id, p.id, 'system' FROM roles r, permissions p WHERE r.name = 'ADMIN';
INSERT INTO role_menus (role_id, menu_id, created_by) SELECT r.id, m.id, 'system' FROM roles r, menus m WHERE r.name = 'ADMIN';
INSERT INTO user_roles (user_id, role_id, created_by) SELECT u.id, r.id, 'system' FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ADMIN';
```

- [ ] **Step 4: Create database and run migrations**

1. Create a PostgreSQL database named `template_db`.
2. Run: `mvn spring-boot:run`
Expected: Application starts successfully, and Flyway creates all tables and inserts data.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/
git commit -m "feat: add Flyway database migrations for schema and initial data"
```
---

### Task 4: Entities and DTOs

**Files:**
- Create: `src/main/java/com/template/entity/*`
- Create: `src/main/java/com/template/dto/*`

- [ ] **Step 1: Create `BaseEntity`**

```java
// src/main/java/com/template/entity/BaseEntity.java
package com.template.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
    private Boolean deleted = false;
    @Version
    private Integer version = 0;
}
```

- [ ] **Step 2: Create other entities (`User`, `Role`, `Permission`, `Menu`, etc.)**

```java
// src/main/java/com/template/entity/User.java
package com.template.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    private String username;
    private String password;
    private String fullname;
    private String email;
    private Boolean enabled;
    private Boolean accountLocked;
    private Integer loginAttempts;
    private LocalDateTime lastLogin;
}

// Create Role.java, Permission.java, Menu.java, UserRole.java, etc. similarly
```

- [ ] **Step 3: Create DTOs (`UserRequest`, `UserResponse`, etc.)**

```java
// src/main/java/com/template/dto/UserRequest.java
package com.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UserRequest {
    @NotBlank
    private String username;
    @NotBlank
    @Size(min = 6)
    private String password;
    @NotBlank
    private String fullname;
    private String email;
    private List<Long> roleIds;
}

// Create UserResponse.java, PageResult.java, etc. similarly
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/template/entity/ src/main/java/com/template/dto/
git commit -m "feat: add entities and DTOs for RBAC"
```
---

### Task 5: Base Layout and Thymeleaf Configuration

**Files:**
- Create: `src/main/java/com/template/config/ThymeleafConfig.java`
- Create: `src/main/resources/templates/layout/*.html`
- Create: `src/main/resources/static/css/custom.css`
- Create: `src/main/resources/static/js/app.js`

- [ ] **Step 1: Create Layout files** (`main.html`, `header.html`, `sidebar.html`, `footer.html`)
- [ ] **Step 2: Create empty CSS and JS files.**
- [ ] **Step 3: Create Thymeleaf Config**

```java
// src/main/java/com/template/config/ThymeleafConfig.java
package com.template.config;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThymeleafConfig {
    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }
}
```

- [ ] **Step 4: Commit**
```bash
git add src/main/java/com/template/config/ThymeleafConfig.java src/main/resources/templates/layout/ src/main/resources/static/
git commit -m "feat: set up base Thymeleaf layout and static resources"
```
---

### Task 6: Security Configuration

**Files:**
- Create: `src/main/java/com/template/config/SecurityConfig.java`
- Create: `src/main/java/com/template/security/*`
- Create: `src/main/java/com/template/mapper/*` (for User, Role, Permission)

- [ ] **Step 1: Create Mappers for `User`, `Role`, `Permission`**
- [ ] **Step 2: Create `PasswordEncoderConfig`**
```java
// src/main/java/com/template/security/PasswordEncoderConfig.java
package com.template.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```
- [ ] **Step 3: Create `CustomUserDetailsService`** (as per design doc)
- [ ] **Step 4: Create `CustomAuthenticationSuccessHandler` and `FailureHandler`**
- [ ] **Step 5: Create `SecurityConfig`** (as per design doc)
- [ ] **Step 6: Write unit test for `CustomUserDetailsService`**
- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/template/config/SecurityConfig.java src/main/java/com/template/security/ src/main/java/com/template/mapper/ src/test/
git commit -m "feat: implement Spring Security configuration and user details service"
```
---

### Task 7: Authentication Controller and Login Page

**Files:**
- Create: `src/main/java/com/template/controller/AuthController.java`
- Create: `src/main/java/com/template/controller/DashboardController.java`
- Create: `src/main/resources/templates/auth/login.html`
- Create: `src/main/resources/templates/dashboard.html`

- [ ] **Step 1: Create `login.html` view**
- [ ] **Step 2: Create `dashboard.html` view**
- [ ] **Step 3: Create `AuthController` to serve the login page**
```java
// src/main/java/com/template/controller/AuthController.java
package com.template.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}
```
- [ ] **Step 4: Create `DashboardController`**
```java
// src/main/java/com/template/controller/DashboardController.java
package com.template.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
```
- [ ] **Step 5: Test login flow**
Run application, navigate to `/login`, and attempt to log in with `admin`/`admin123`. Should redirect to `/dashboard`.
- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/template/controller/ src/main/resources/templates/auth/ src/main/resources/templates/dashboard.html
git commit -m "feat: implement login page and dashboard"
```
---

### Task 8: Dynamic Menu Implementation

**Files:**
- Create: `src/main/java/com/template/service/MenuService.java`
- Create: `src/main/java/com/template/util/MenuTreeBuilder.java`
- Create: `src/main/java/com/template/controller/BaseController.java`
- Modify: `src/main/resources/templates/layout/sidebar.html`
- Create: `src/main/resources/mapper/MenuMapper.xml`

- [ ] **Step 1: Create `MenuService` and `MenuTreeBuilder`** (as per design doc)
- [ ] **Step 2: Create `MenuMapper.xml` with `findByRoleNames` query**
- [ ] **Step 3: Create `BaseController` with `@ModelAttribute("menuTree")`**
- [ ] **Step 4: Update `sidebar.html` to render the menu tree** (as per design doc)
- [ ] **Step 5: Test dynamic menu**
Login as admin, verify that the sidebar shows the Dashboard and Master menus.
- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/template/service/MenuService.java src/main/java/com/template/util/MenuTreeBuilder.java src/main/java/com/template/controller/BaseController.java src/main/resources/templates/layout/sidebar.html src/main/resources/mapper/MenuMapper.xml
git commit -m "feat: implement dynamic menu system"
```
---

### Task 9: User Management Module

**Files:**
- Create: `src/main/java/com/template/controller/UserController.java`
- Create: `src/main/java/com/template/service/UserService.java`
- Create: `src/main/java/com/template/mapper/UserMapper.xml`
- Create: `src/main/resources/templates/user/*`

- [ ] **Step 1: Create `UserMapper.xml` for `findAll` and `countAll` queries**
- [ ] **Step 2: Create `UserService` for user CRUD operations** (as per design doc)
- [ ] **Step 3: Create `UserController` for handling user management routes**
- [ ] **Step 4: Create Thymeleaf templates: `list.html`, `form.html`, `detail.html`**
- [ ] **Step 5: Test User CRUD functionality**
- [ ] **Step 6: Commit**
```bash
git add src/main/java/com/template/controller/UserController.java src/main/java/com/template/service/UserService.java src/main/java/com/template/mapper/UserMapper.xml src/main/resources/templates/user/
git commit -m "feat: implement user management module"
```
---
*(Tasks for Role, Permission, Menu management, and Exception Handling would follow a similar pattern)*
