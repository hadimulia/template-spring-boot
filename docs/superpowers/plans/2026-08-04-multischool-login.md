# Multi-School Login with School Code + System Realm — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a school-code field to login, authenticate against the chosen school's DB, and add a dedicated `sims_system` realm for master administration.

**Architecture:** Login carries `(schoolCode, username, password)`. The school code reaches the auth flow via a custom `WebAuthenticationDetails`. `CustomUserDetailsService` branches on `schoolCode == "system"` (system realm) vs. a real school code (registry → school DB). A new `SystemDataSourceManager` + `db/system` migration set provision `sims_system`. Routing key `system` maps to that pool.

**Tech Stack:** Spring Boot 3.3.2, Spring Security, tk.mybatis, Flyway, PostgreSQL, Thymeleaf, Java 21.

## Global Constraints

- Java 21, source/release 21 (spring-boot-starter-parent derives `maven.compiler.release` from `<java.version>`).
- `@Primary` beats parameter-name autowiring — every registry/system bean reference must be `@Qualifier`ed explicitly.
- Registry mappers bind to `registrySqlSessionFactory`; school mappers to `@Primary sqlSessionFactory`.
- `school_users.username` must become unique-per-school (drop global UNIQUE, add `UNIQUE(school_id, username)`).
- `system` is a reserved code — never a row in `schools`.
- Passwords use `BCryptPasswordEncoder`; seeded `admin`/`admin123` hash reused from existing migrations.
- New migrations must NOT modify already-applied files (Flyway checksum mismatch). Add new versioned files only.

---

### Task 1: Registry migration — unique-per-school username

**Files:**
- Create: `src/main/resources/db/registry/V5__unique_username_per_school.sql`

**Interfaces:**
- Consumes: live `sims_registry.school_users` (global UNIQUE on `username`, constraint name `school_users_username_key`).
- Produces: `school_users` allows the same `username` in different `school_id` rows; unique on `(school_id, username)`.

- [ ] **Step 1: Write the migration**

```sql
-- Allow the same username in multiple schools (e.g. 'admin' exists in every
-- school DB). Uniqueness is now per (school_id, username) instead of global.
ALTER TABLE school_users DROP CONSTRAINT school_users_username_key;

CREATE UNIQUE INDEX uk_school_users_school_username
    ON school_users(school_id, username) WHERE deleted = false;
```

- [ ] **Step 2: Verify it applies cleanly against a copy**

Run: `docker exec 17be7e22dda6 psql -U postgres -d sims_registry -c "\d school_users"`
Expected: the `school_users_username_key` UNIQUE constraint is gone; `uk_school_users_school_username` index exists. (Applies automatically on next boot via `registryFlyway`; do NOT run manually against live yet — Flyway tracks it. Boot will apply it.)

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/registry/V5__unique_username_per_school.sql
git commit -m "feat: make registry school_users username unique per school"
```

---

### Task 2: System realm — Flyway migration set for `sims_system`

**Files:**
- Create: `src/main/resources/db/system/V1__create_system_schema.sql`

**Interfaces:**
- Consumes: school realm table shapes (users, roles, permissions, menus, junctions from `db/migration/V1`–`V3`).
- Produces: `sims_system.users` seeded with `admin`/`admin123`, a `SYSTEM` role granted `SCHOOL_*` permissions, and the Schools menu.

- [ ] **Step 1: Write the system schema migration**

```sql
-- Dedicated system realm for master administrators who control all schools.
-- Mirrors the school realm's user/RBAC shape so existing mappers work unchanged.

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

-- Seed: system admin (password: admin123, same hash as school V3)
INSERT INTO users (username, password, fullname, email, enabled, created_by)
VALUES ('admin', '$2a$10$i8.1.j4j.iZp2GkL7c5y8uJndnZJ0Z0mF/b.yH/y.xY1c2W5N6OqG',
        'System Administrator', 'admin@system', true, 'system');

INSERT INTO roles (name, description, created_by) VALUES ('SYSTEM', 'System Administrator', 'system');

INSERT INTO permissions (code, description, created_by) VALUES
    ('SCHOOL_VIEW', 'View schools', 'system'),
    ('SCHOOL_CREATE', 'Create school', 'system'),
    ('SCHOOL_EDIT', 'Edit school', 'system'),
    ('SCHOOL_DELETE', 'Delete school', 'system');

INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT NULL, 'Schools', '/schools', 'bi-buildings', 1, true, 'system';

INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system' FROM roles r, permissions p
WHERE r.name = 'SYSTEM' AND p.code IN ('SCHOOL_VIEW','SCHOOL_CREATE','SCHOOL_EDIT','SCHOOL_DELETE');

INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system' FROM roles r, menus m
WHERE r.name = 'SYSTEM' AND m.url = '/schools';

INSERT INTO user_roles (user_id, role_id, created_by)
SELECT u.id, r.id, 'system' FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'SYSTEM';
```

- [ ] **Step 2: Compile the app (no code change yet) to confirm nothing breaks**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS (new migration file is not compiled).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/system/V1__create_system_schema.sql
git commit -m "feat: system realm schema for sims_system"
```

---

### Task 3: `SystemDataSourceManager` — provision + pool `sims_system`

**Files:**
- Create: `src/main/java/com/template/config/SystemDataSourceManager.java`

**Interfaces:**
- Consumes: `SchoolDataSourceProperties` (admin url, username, password, driver, max-pool-size).
- Produces: `getOrCreate()` → `javax.sql.DataSource` for `sims_system`, auto-created + Flyway-migrated (`classpath:db/system`) on first use, cached in a `ConcurrentMap`.

- [ ] **Step 1: Write the manager**

```java
package com.template.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Auto-creates and migrates the dedicated system realm database (sims_system)
 * on first use, mirroring {@link SchoolDataSourceManager}. Used by the routing
 * DataSource when the login school code is {@code system}.
 */
@Component
public class SystemDataSourceManager {

    private static final Logger log = LoggerFactory.getLogger(SystemDataSourceManager.class);
    private static final String DB_NAME = "sims_system";

    private final SchoolDataSourceProperties props;
    private volatile HikariDataSource pool;

    public SystemDataSourceManager(SchoolDataSourceProperties props) {
        this.props = props;
    }

    public DataSource getOrCreate() {
        if (pool == null) {
            synchronized (this) {
                if (pool == null) {
                    ensureDatabaseExists();
                    pool = buildPool();
                    runFlyway();
                }
            }
        }
        return pool;
    }

    private void ensureDatabaseExists() {
        try (Connection conn = DriverManager.getConnection(props.getAdminUrl(),
                props.getUsername(), props.getPassword());
             PreparedStatement check = conn.prepareStatement(
                     "SELECT 1 FROM pg_database WHERE datname = ?")) {
            check.setString(1, DB_NAME);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
            try (PreparedStatement create = conn.prepareStatement(
                    "CREATE DATABASE \"" + DB_NAME + "\"")) {
                create.executeUpdate();
                log.info("Created system database {}", DB_NAME);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create system database " + DB_NAME, e);
        }
    }

    private HikariDataSource buildPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getUrlPrefix() + DB_NAME);
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        config.setDriverClassName(props.getDriverClassName());
        config.setPoolName("system-pool");
        config.setMaximumPoolSize(props.getMaxPoolSize());
        return new HikariDataSource(config);
    }

    private void runFlyway() {
        Flyway flyway = Flyway.configure()
                .dataSource(pool)
                .locations("classpath:db/system")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Migrated system database {} (schema version {})",
                DB_NAME, flyway.info().current().getVersion());
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/template/config/SystemDataSourceManager.java
git commit -m "feat: SystemDataSourceManager provisions sims_system"
```

---

### Task 4: Routing — `system` key → `sims_system` pool

**Files:**
- Modify: `src/main/java/com/template/config/TenantDataSource.java`

**Interfaces:**
- Consumes: `SystemDataSourceManager` (from Task 3).
- Produces: routing key `system` resolves to the `sims_system` pool via `manager.getOrCreate()`.

- [ ] **Step 1: Wire the system manager into `TenantDataSource`**

Change the constructor to accept `SystemDataSourceManager systemManager` and route the `system` key:

```java
package com.template.config;

import com.template.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantDataSource.class);
    private static final String SYSTEM_KEY = "system";

    private final DataSource registryDataSource;
    private final SchoolDataSourceManager manager;
    private final SystemDataSourceManager systemManager;
    private final Map<String, DataSource> schoolDataSources = new ConcurrentHashMap<>();

    public TenantDataSource(DataSource registryDataSource,
                            SchoolDataSourceManager manager,
                            SystemDataSourceManager systemManager) {
        this.registryDataSource = registryDataSource;
        this.manager = manager;
        this.systemManager = systemManager;
        setDefaultTargetDataSource(registryDataSource);
        setTargetDataSources(new ConcurrentHashMap<>());
        afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String routingKey = TenantContext.getRoutingKey();
        if (routingKey == null || routingKey.isBlank()) {
            return null;
        }
        return routingKey;
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Object key = determineCurrentLookupKey();
        if (key == null) {
            return registryDataSource;
        }
        String dbName = (String) key;
        if (SYSTEM_KEY.equals(dbName)) {
            return systemManager.getOrCreate();
        }
        return schoolDataSources.computeIfAbsent(dbName, name -> {
            log.info("Initializing school database {}", name);
            return manager.getOrCreateByDbName(name);
        });
    }

    public DataSource registryDataSource() {
        return registryDataSource;
    }
}
```

- [ ] **Step 2: Update the `TenantDataSource` bean definition**

In `RoutingDataSourceConfig.java`, inject `SystemDataSourceManager`:

```java
@Bean
@Primary
public DataSource dataSource(DataSource registryDataSource,
                             SchoolDataSourceManager manager,
                             SystemDataSourceManager systemManager) {
    return new TenantDataSource(registryDataSource, manager, systemManager);
}
```

- [ ] **Step 3: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/template/config/TenantDataSource.java src/main/java/com/template/config/RoutingDataSourceConfig.java
git commit -m "feat: route system key to sims_system pool"
```

---

### Task 5: Capture `schoolCode` in the auth token

**Files:**
- Create: `src/main/java/com/template/security/SchoolCodeWebAuthenticationDetails.java`
- Create: `src/main/java/com/template/security/SchoolCodeAuthenticationDetailsSource.java`
- Modify: `src/main/java/com/template/config/SecurityConfig.java`

**Interfaces:**
- Consumes: login form submits `POST /login` with params `schoolCode`, `username`, `password`.
- Produces: `SecurityConfig` uses `SchoolCodeAuthenticationDetailsSource`; during authentication, `Authentication.getDetails()` is a `SchoolCodeWebAuthenticationDetails` exposing `getSchoolCode()`.

- [ ] **Step 1: Write the details class**

```java
package com.template.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/** Carries the login form's {@code schoolCode} param to the auth flow. */
public class SchoolCodeWebAuthenticationDetails extends WebAuthenticationDetails {

    private final String schoolCode;

    public SchoolCodeWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.schoolCode = request.getParameter("schoolCode");
    }

    public String getSchoolCode() {
        return schoolCode;
    }
}
```

- [ ] **Step 2: Write the details source**

```java
package com.template.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/** Builds {@link SchoolCodeWebAuthenticationDetails} for each login attempt. */
public class SchoolCodeAuthenticationDetailsSource
        implements AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails> {

    @Override
    public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        return new SchoolCodeWebAuthenticationDetails(context);
    }
}
```

- [ ] **Step 3: Wire it in `SecurityConfig`**

In the `formLogin` block, set the details source and keep `schoolCode` a valid login parameter (do NOT remove it from the request):

```java
.formLogin(form -> form
        .loginPage("/login")
        .loginProcessingUrl("/login")
        .usernameParameter("username")
        .passwordParameter("password")
        .authenticationDetailsSource(new SchoolCodeAuthenticationDetailsSource())
        .successHandler(successHandler)
        .failureHandler(failureHandler)
        .permitAll()
)
```

- [ ] **Step 4: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/template/security/SchoolCodeWebAuthenticationDetails.java src/main/java/com/template/security/SchoolCodeAuthenticationDetailsSource.java src/main/java/com/template/config/SecurityConfig.java
git commit -m "feat: capture schoolCode in login auth details"
```

---

### Task 6: Two-realm authentication in `CustomUserDetailsService`

**Files:**
- Modify: `src/main/java/com/template/security/CustomUserDetailsService.java`

**Interfaces:**
- Consumes: `SchoolMapper`, `SchoolUserMapper`, `UserMapper`, `RoleMapper`, `PermissionMapper`, `SchoolDataSourceManager`, `SystemDataSourceManager`; `SchoolCodeWebAuthenticationDetails` (Task 5).
- Produces: for school code `system`, a `CustomUserDetails` with `schoolId=null`, `schoolCode="system"`, `schoolDbName="sims_system"`; for a real school, the existing behavior but looked up by `(schoolId, username)`.

- [ ] **Step 1: Add `findBySchoolAndUsername` to the mapper + XML**

Modify `src/main/java/com/template/registry/mapper/SchoolUserMapper.java`:

```java
SchoolUser findBySchoolAndUsername(@Param("schoolId") Long schoolId, @Param("username") String username);
```

Modify `src/main/resources/registry-mapper/SchoolUserMapper.xml`, add:

```xml
<select id="findBySchoolAndUsername" resultMap="schoolUserMap">
    SELECT * FROM school_users
    WHERE school_id = #{schoolId} AND username = #{username} AND deleted = false
</select>
```

- [ ] **Step 2: Rewrite `CustomUserDetailsService`**

```java
package com.template.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;

import com.template.config.SchoolDataSourceManager;
import com.template.config.SystemDataSourceManager;
import com.template.entity.permission.Permission;
import com.template.entity.registry.SchoolUser;
import com.template.entity.role.Role;
import com.template.entity.school.School;
import com.template.entity.user.User;
import com.template.mapper.permission.PermissionMapper;
import com.template.mapper.role.RoleMapper;
import com.template.mapper.user.UserMapper;
import com.template.registry.mapper.SchoolMapper;
import com.template.registry.mapper.SchoolUserMapper;
import com.template.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final String SYSTEM_CODE = "system";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final SchoolUserMapper schoolUserMapper;
    private final SchoolMapper schoolMapper;
    private final SchoolDataSourceManager schoolDataSourceManager;
    private final SystemDataSourceManager systemDataSourceManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String schoolCode = currentSchoolCode();
        if (SYSTEM_CODE.equalsIgnoreCase(schoolCode)) {
            return loadSystemUser(username);
        }
        return loadSchoolUser(username, schoolCode);
    }

    private String currentSchoolCode() {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken token
                && token.getDetails() instanceof WebAuthenticationDetails details) {
            if (details instanceof SchoolCodeWebAuthenticationDetails sc) {
                String code = sc.getSchoolCode();
                if (code != null && !code.isBlank()) {
                    return code.trim();
                }
            }
        }
        throw new UsernameNotFoundException("School code is required");
    }

    private UserDetails loadSchoolUser(String username, String schoolCode) {
        School school = schoolMapper.findByCode(schoolCode);
        if (school == null || Boolean.TRUE.equals(school.getDeleted())
                || !"ACTIVE".equals(school.getStatus())) {
            throw new UsernameNotFoundException("School not found or inactive: " + schoolCode);
        }

        SchoolUser index = schoolUserMapper.findBySchoolAndUsername(school.getId(), username);
        if (index == null) {
            throw new UsernameNotFoundException("User not found in this school");
        }
        if (!Boolean.TRUE.equals(index.getEnabled())) {
            throw new DisabledException("Account is disabled");
        }

        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(school.getId());
        try {
            schoolDataSourceManager.getOrCreate(school.getCode());

            User user = userMapper.selectByPrimaryKey(index.getUserId());
            if (user == null) {
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
            if (!user.getEnabled()) {
                throw new DisabledException("Account is disabled");
            }
            if (user.getAccountLocked()) {
                throw new LockedException("Account is locked");
            }

            Set<GrantedAuthority> authorities = new HashSet<>();
            List<Role> roles = roleMapper.findByUserId(user.getId());
            for (Role role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                List<Permission> permissions = permissionMapper.findByRoleId(role.getId());
                for (Permission permission : permissions) {
                    authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                }
            }

            return new CustomUserDetails(user.getId(), school.getId(), school.getCode(),
                    school.getDbName(), user.getUsername(), user.getPassword(), authorities);
        } finally {
            TenantContext.clear();
        }
    }

    private UserDetails loadSystemUser(String username) {
        TenantContext.setRoutingKey(SYSTEM_CODE);
        try {
            systemDataSourceManager.getOrCreate();
            User user = userMapper.findByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
            if (!user.getEnabled()) {
                throw new DisabledException("Account is disabled");
            }
            if (user.getAccountLocked()) {
                throw new LockedException("Account is locked");
            }

            Set<GrantedAuthority> authorities = new HashSet<>();
            List<Role> roles = roleMapper.findByUserId(user.getId());
            for (Role role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                List<Permission> permissions = permissionMapper.findByRoleId(role.getId());
                for (Permission permission : permissions) {
                    authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                }
            }

            return new CustomUserDetails(user.getId(), null, SYSTEM_CODE, "sims_system",
                    user.getUsername(), user.getPassword(), authorities);
        } finally {
            TenantContext.clear();
        }
    }
}
```

- [ ] **Step 3: Verify mapper reuse for the system realm**

`loadSystemUser` reuses `UserMapper.findByUsername`, `RoleMapper.findByUserId`, and `PermissionMapper.findByRoleId`. These school-realm mappers bind to the routing DataSource; while the routing key is `system`, they read `sims_system`. No new mapper is needed. Confirm `RoleMapper.findByUserId` and `PermissionMapper.findByRoleId` exist with the signatures used above (they are already used in `loadSchoolUser`, so they do).

- [ ] **Step 4: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/template/security/CustomUserDetailsService.java src/main/java/com/template/registry/mapper/SchoolUserMapper.java src/main/resources/registry-mapper/SchoolUserMapper.xml
git commit -m "feat: two-realm auth by school code + username"
```

---

### Task 7: Login form — add school code field

**Files:**
- Modify: `src/main/resources/templates/auth/login.html`
- Modify: `src/main/resources/i18n/messages.properties`
- Modify: `src/main/resources/i18n/messages_id.properties`

**Interfaces:**
- Consumes: the new auth flow (Task 6) reads `schoolCode` from the request.
- Produces: login form posts `schoolCode`, `username`, `password`.

- [ ] **Step 1: Add the school code field**

Insert between the brand header and the username field:

```html
<div class="mb-3">
    <label for="schoolCode" class="form-label" th:text="#{login.schoolCode}">School Code</label>
    <div class="input-group">
        <span class="input-group-text"><i class="bi bi-building"></i></span>
        <input type="text" id="schoolCode" name="schoolCode"
               class="form-control" th:placeholder="#{login.schoolCode.placeholder}" required autofocus>
    </div>
</div>
```

Move `autofocus` from the username input to this field.

- [ ] **Step 2: Add i18n keys (English)**

In `messages.properties`:

```properties
login.schoolCode=School Code
login.schoolCode.placeholder=School code (e.g. baru) or system
```

- [ ] **Step 3: Add i18n keys (Indonesian)**

In `messages_id.properties`:

```properties
login.schoolCode=Kode Sekolah
login.schoolCode.placeholder=Kode sekolah (mis. baru) atau system
```

- [ ] **Step 4: Compile (templates are not compiled, but confirm app boots)**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/auth/login.html src/main/resources/i18n/messages.properties src/main/resources/i18n/messages_id.properties
git commit -m "feat: add school code field to login form"
```

---

### Task 8: Onboarding — auto-index seeded admin

**Files:**
- Modify: `src/main/java/com/template/service/school/SchoolServiceImpl.java`

**Interfaces:**
- Consumes: `School` entity with populated `id` (after `save`), `SchoolUserMapper` (registry).
- Produces: after creating a school, a `school_users` row links the school's seeded `admin` (user_id 1) so the school is immediately login-able.

- [ ] **Step 1: Inject `SchoolUserMapper` and create the index row**

In `SchoolServiceImpl`, add the field and import:

```java
import com.template.entity.registry.SchoolUser;
import com.template.registry.mapper.SchoolUserMapper;
```

Add to the constructor:

```java
private final SchoolUserMapper schoolUserMapper;
```

In `create()`, after `save(school);`, index the seeded admin:

```java
save(school);

// Every school DB is seeded with an 'admin' user (user_id 1) by the school
// migration set. Index it globally so the school is immediately login-able.
SchoolUser index = new SchoolUser();
index.setSchoolId(school.getId());
index.setUserId(1L);
index.setUsername("admin");
index.setEnabled(true);
index.setCreatedBy(SecurityUtils.getCurrentUsername());
index.setDeleted(false);
schoolUserMapper.insertSelective(index);
```

- [ ] **Step 2: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/template/service/school/SchoolServiceImpl.java
git commit -m "feat: auto-index seeded admin for new schools"
```

---

### Task 9: Failure handler — distinct messages

**Files:**
- Modify: `src/main/java/com/template/security/CustomAuthenticationFailureHandler.java`

**Interfaces:**
- Consumes: request params `schoolCode`, `username`; registry mappers.
- Produces: distinct `?error=` messages for school-not-found vs user-not-found vs bad credentials.

- [ ] **Step 1: Rewrite the failure handler**

```java
package com.template.security;

import java.io.IOException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.template.entity.registry.SchoolUser;
import com.template.entity.school.School;
import com.template.registry.mapper.SchoolMapper;
import com.template.registry.mapper.SchoolUserMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final SchoolUserMapper schoolUserMapper;
    private final SchoolMapper schoolMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String schoolCode = request.getParameter("schoolCode");
        String username = request.getParameter("username");
        String errorMessage = "Invalid username or password";

        if (exception instanceof UsernameNotFoundException) {
            if (schoolCode != null && !schoolCode.isBlank()
                    && !"system".equalsIgnoreCase(schoolCode.trim())) {
                School school = schoolMapper.findByCode(schoolCode.trim());
                if (school == null || Boolean.TRUE.equals(school.getDeleted())
                        || !"ACTIVE".equals(school.getStatus())) {
                    errorMessage = "School not found or inactive: " + schoolCode;
                } else if (username != null && !username.isEmpty()) {
                    SchoolUser index = schoolUserMapper.findBySchoolAndUsername(
                            school.getId(), username);
                    errorMessage = (index == null)
                            ? "User not found in this school"
                            : "Invalid username or password";
                }
            } else {
                errorMessage = "User not found in system";
            }
        }

        if (exception instanceof LockedException) {
            errorMessage = "Account has been locked. Please contact administrator";
        } else if (exception instanceof DisabledException) {
            errorMessage = "Account is disabled";
        } else if (exception instanceof BadCredentialsException) {
            // keep "Invalid username or password"
        }

        getRedirectStrategy().sendRedirect(request, response, "/login?error=" + errorMessage);
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/template/security/CustomAuthenticationFailureHandler.java
git commit -m "feat: distinct login failure messages"
```

---

### Task 10: End-to-end verification

**Files:**
- None (verification only).

- [ ] **Step 1: Clean compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 2: Restart the app**

```bash
pkill -f spring-boot:run 2>/dev/null; sleep 2
nohup mvn -o spring-boot:run > /tmp/boot19.log 2>&1 &
```

Wait for `Started TemplateApplication` (monitor `/tmp/boot19.log`).

- [ ] **Step 3: Confirm registry V5 + system V1 migrations applied**

Run: `docker exec 17be7e22dda6 psql -U postgres -d sims_registry -c "\d school_users"`
Expected: no global username UNIQUE; `uk_school_users_school_username` index present.

Run: `docker exec 17be7e22dda6 psql -U postgres -d sims_system -c "SELECT id, username, enabled FROM users;"`
Expected: one `admin` user, enabled.

- [ ] **Step 4: Create a new school via UI**

Login as `system`/`admin`/`admin123`, create school `coba` → DB `sims_coba` created + migrated; `school_users` has a `(coba, admin)` row.

- [ ] **Step 5: Login to the new school**

Login with `schoolCode=coba`, `username=admin`, `password=admin123` → redirect to `/dashboard`, routed to `sims_coba` (school name/db visible).

- [ ] **Step 6: Login to the system realm**

Login with `schoolCode=system`, `username=admin`, `password=admin123` → redirect to `/dashboard`, then `/schools` shows the school list (all schools).

- [ ] **Step 7: Negative cases**

- `schoolCode=nowhere` → "School not found or inactive: nowhere"
- `schoolCode=coba` + wrong username → "User not found in this school"
- `schoolCode=coba` + `admin` + wrong password → "Invalid username or password"

- [ ] **Step 8: Final commit of any fixups**

```bash
git add -A
git commit -m "chore: verification fixups"
```

---

## Self-Review Notes

- **Spec coverage:** login form (T7), auth flow (T6), system realm DB + manager (T2, T3), routing (T4), registry uniqueness (T1), onboarding index (T8), distinct errors (T9). All spec sections map to tasks.
- **Type consistency:** `SystemDataSourceManager.getOrCreate()` used in T3/T4/T6; `SchoolCodeWebAuthenticationDetails.getSchoolCode()` used in T6; `findBySchoolAndUsername(Long, String)` defined in T6 and used in T6/T9. `CustomUserDetails` constructor signature unchanged from existing code.
- **Note:** `loadSystemUser` reuses the school-realm `UserMapper`/`RoleMapper`/`PermissionMapper` which bind to the routing DataSource; routing key `system` → `sims_system`, so these read the system DB. This reuses existing mappers with no new mapper needed.
