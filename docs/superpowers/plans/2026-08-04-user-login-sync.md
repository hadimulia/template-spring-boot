# User Login Index Sync — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make hand-created users login-able by syncing the registry `school_users` login index on user create/update/delete, with per-school username uniqueness.

**Architecture:** `SecurityUtils` gains `getCurrentSchoolId()`. `UserServiceImpl` injects the registry `SchoolUserMapper` and, alongside its existing school-DB user writes, creates/updates/soft-deletes the matching `school_users` row. All operations share the existing `@Transactional` on the service, so a failed index write rolls back the user write.

**Tech Stack:** Spring Boot 3.3.2, tk.mybatis, MyBatis, PostgreSQL, Playwright (manual E2E).

## Global Constraints

- Java 21, source/release 21.
- `school_users` is a **registry** table — write it only via the registry `SchoolUserMapper` (binds to `registrySqlSessionFactory`), never the school mapper.
- `SchoolUser` entity uses `@KeySql(genId = RegistrySequenceGenId.class)` for id generation — `insertSelective` generates the id from `school_users_id_seq`.
- Username uniqueness is **per school**: the school DB `users.username` is unique within that DB.
- `getCurrentSchoolId()` returns `null` for the system realm; user creation must not silently skip indexing — throw instead.
- New migrations must NOT modify already-applied files (Flyway checksum mismatch). No migration is needed for this feature (the `school_users` table already exists).
- Playwright verification is manual/browser-driven (no CI runner) against the running app.

---

### Task 1: `SecurityUtils.getCurrentSchoolId()`

**Files:**
- Modify: `src/main/java/com/template/util/SecurityUtils.java`

**Interfaces:**
- Consumes: `CustomUserDetails.getSchoolId()` (existing).
- Produces: `static Long SecurityUtils.getCurrentSchoolId()` — the school id of the authenticated principal, or `null` for the system realm.

- [ ] **Step 1: Add the method**

Add after `getCurrentUserId()`:

```java
public static Long getCurrentSchoolId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()
            && !(auth instanceof AnonymousAuthenticationToken)
            && auth.getPrincipal() instanceof CustomUserDetails details) {
        return details.getSchoolId();
    }
    return null;
}
```

- [ ] **Step 2: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/template/util/SecurityUtils.java
git commit -m "feat: expose current school id from security context"
```

---

### Task 2: Sync login index on user create

**Files:**
- Modify: `src/main/java/com/template/service/user/UserServiceImpl.java`

**Interfaces:**
- Consumes: `SecurityUtils.getCurrentSchoolId()` (Task 1); `SchoolUser` entity; `SchoolUserMapper` (registry); `UserMapper.findByUsername` (school DB).
- Produces: `create()` rejects duplicate per-school usernames and creates a `school_users` index row for each new user.

- [ ] **Step 1: Inject the registry mapper and imports**

Add imports:

```java
import com.template.entity.registry.SchoolUser;
import com.template.registry.mapper.SchoolUserMapper;
```

Add a field + constructor param:

```java
private final SchoolUserMapper schoolUserMapper;
```

Update the constructor:

```java
public UserServiceImpl(UserMapper userMapper,
                       UserRoleMapper userRoleMapper,
                       RoleMapper roleMapper,
                       PasswordEncoder passwordEncoder,
                       SchoolUserMapper schoolUserMapper) {
    super(userMapper);
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.roleMapper = roleMapper;
    this.passwordEncoder = passwordEncoder;
    this.schoolUserMapper = schoolUserMapper;
}
```

- [ ] **Step 2: Add duplicate-username check + index sync in `create()`**

Add a `createUserIndex(User user)` helper and wire it into `create()`:

```java
@Auditable(action = "CREATE", entityType = "USER", description = "#request.username")
public void create(UserRequest request) {
    if (userMapper.findByUsername(request.getUsername()) != null) {
        throw new IllegalArgumentException("Username already exists in this school: " + request.getUsername());
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setFullname(request.getFullname());
    user.setEmail(request.getEmail());
    user.setEnabled(true);
    user.setAccountLocked(false);
    user.setLoginAttempts(0);
    user.setCreatedBy(SecurityUtils.getCurrentUsername());
    user.setCreatedDate(LocalDateTime.now());
    user.setDeleted(false);
    user.setVersion(0);
    userMapper.insert(user);

    if (request.getRoleIds() != null) {
        for (Long roleId : request.getRoleIds()) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(roleId);
            userRole.setCreatedBy(SecurityUtils.getCurrentUsername());
            userRole.setCreatedDate(LocalDateTime.now());
            userRoleMapper.insert(userRole);
        }
    }

    createUserIndex(user);
}
```

Add the private helper:

```java
private void createUserIndex(User user) {
    Long schoolId = SecurityUtils.getCurrentSchoolId();
    if (schoolId == null) {
        throw new IllegalStateException("No school context for user creation");
    }
    SchoolUser index = new SchoolUser();
    index.setSchoolId(schoolId);
    index.setUserId(user.getId());
    index.setUsername(user.getUsername());
    index.setEnabled(true);
    index.setCreatedBy(SecurityUtils.getCurrentUsername());
    index.setDeleted(false);
    schoolUserMapper.insertSelective(index);
}
```

- [ ] **Step 3: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/template/service/user/UserServiceImpl.java
git commit -m "feat: index new school users in registry login table"
```

---

### Task 3: Sync login index on user update

**Files:**
- Modify: `src/main/java/com/template/service/user/UserServiceImpl.java`

**Interfaces:**
- Consumes: `SchoolUserMapper.findByUserIdAndSchool(userId, schoolId)` + `updateByPrimaryKey` (Task 2).
- Produces: `update()` keeps `school_users.username` in sync when the username changes.

- [ ] **Step 1: Update the `update()` method to sync the username**

Replace the existing `update()` body's username update block. Full method:

```java
@Auditable(action = "UPDATE", entityType = "USER", description = "#request.username")
public void update(Long id, UserUpdateRequest request) {
    User user = get(id);
    if (user != null) {
        boolean usernameChanged = request.getUsername() != null
                && !request.getUsername().equals(user.getUsername());
        user.setUsername(request.getUsername());
        user.setFullname(request.getFullname());
        user.setEmail(request.getEmail());
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setUpdatedBy(SecurityUtils.getCurrentUsername());
        user.setUpdatedDate(LocalDateTime.now());
        userMapper.updateByPrimaryKey(user);

        if (request.getRoleIds() != null) {
            List<Long> existingRoleIds = userRoleMapper.findRoleIdsByUserIdAll(id);
            for (Long roleId : existingRoleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                userRoleMapper.delete(ur);
            }

            for (Long roleId : request.getRoleIds()) {
                UserRole userRole = new UserRole();
                userRole.setUserId(id);
                userRole.setRoleId(roleId);
                userRole.setCreatedBy(SecurityUtils.getCurrentUsername());
                userRole.setCreatedDate(LocalDateTime.now());
                userRoleMapper.insert(userRole);
            }
        }

        if (usernameChanged) {
            syncUserIndex(id, request.getUsername());
        }
    }
}
```

Add the private helper:

```java
private void syncUserIndex(Long userId, String newUsername) {
    Long schoolId = SecurityUtils.getCurrentSchoolId();
    if (schoolId == null) {
        return; // system realm has no per-school index to sync
    }
    SchoolUser index = schoolUserMapper.findByUserIdAndSchool(userId, schoolId);
    if (index != null) {
        index.setUsername(newUsername);
        index.setUpdatedBy(SecurityUtils.getCurrentUsername());
        index.setUpdatedDate(LocalDateTime.now());
        schoolUserMapper.updateByPrimaryKey(index);
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/template/service/user/UserServiceImpl.java
git commit -m "feat: sync registry username when user is renamed"
```

---

### Task 4: Sync login index on user delete

**Files:**
- Modify: `src/main/java/com/template/service/user/UserServiceImpl.java`

**Interfaces:**
- Consumes: `SchoolUserMapper.findByUserIdAndSchool` + `updateByPrimaryKey` (Task 3).
- Produces: `delete()` soft-deletes the matching `school_users` row, disabling login.

- [ ] **Step 1: Update `delete()`**

Replace the existing `delete()` body:

```java
@Auditable(action = "DELETE", entityType = "USER", description = "")
public void delete(Long id) {
    User user = get(id);
    if (user != null) {
        user.setDeleted(true);
        user.setUpdatedBy(SecurityUtils.getCurrentUsername());
        user.setUpdatedDate(LocalDateTime.now());
        userMapper.updateByPrimaryKey(user);

        Long schoolId = SecurityUtils.getCurrentSchoolId();
        if (schoolId != null) {
            SchoolUser index = schoolUserMapper.findByUserIdAndSchool(id, schoolId);
            if (index != null) {
                index.setDeleted(true);
                index.setUpdatedBy(SecurityUtils.getCurrentUsername());
                index.setUpdatedDate(LocalDateTime.now());
                schoolUserMapper.updateByPrimaryKey(index);
            }
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/template/service/user/UserServiceImpl.java
git commit -m "feat: soft-delete registry index when user is deleted"
```

---

### Task 5: E2E verification (Playwright + registry checks)

**Files:**
- None (verification only).

- [ ] **Step 1: Clean compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 2: Restart the app**

```bash
pkill -f spring-boot:run 2>/dev/null; sleep 3
nohup mvn -o spring-boot:run > /tmp/boot26.log 2>&1 &
```

Wait for `Started TemplateApplication` (monitor `/tmp/boot26.log`).

- [ ] **Step 3: Create a user via the UI and verify it can log in**

Via Playwright browser:
1. Log in as school `xyz` (`xyz` / `admin` / `admin123`).
2. Go to `/users/new`, create user `guru` / `password123` / fullname `Guru Satu`.
3. Log out.
4. Log in as `xyz` / `guru` / `password123`.
5. Expect redirect to `/dashboard` (routed to `sims_xyz`).

Verify the registry row:

```bash
docker exec 17be7e22dda6 psql -U postgres -d sims_registry -c \
  "SELECT id, school_id, user_id, username, enabled, deleted FROM school_users WHERE username='guru';"
```
Expected: one row, `school_id` = xyz's id, `enabled` = true, `deleted` = false.

- [ ] **Step 4: Duplicate username rejected**

Via Playwright: while logged in as `xyz` admin, go to `/users/new`, create another user with username `guru`. Expect an error message ("Username already exists in this school: guru"), and only one `guru` row in `school_users`.

- [ ] **Step 5: Update username syncs index**

Via Playwright: as `xyz` admin, edit user `guru`, change username to `gurubaru`. Verify:
```bash
docker exec 17be7e22dda6 psql -U postgres -d sims_registry -c \
  "SELECT username FROM school_users WHERE user_id=(SELECT id FROM sims_xyz.users WHERE username='gurubaru');"
```
Expected: `gurubaru`. Log out, log in as `xyz` / `gurubaru` / `password123` → dashboard.

- [ ] **Step 6: Delete disables login**

Via Playwright: as `xyz` admin, delete user `gurubaru`. Verify login `xyz` / `gurubaru` fails with "User not found in this school", and the `school_users` row has `deleted` = true.

- [ ] **Step 7: Same username allowed across schools**

Via Playwright: log in as school `coba` admin (`coba` / `admin` / `admin123`), create user `guru` / `password123`. Verify both `xyz.guru` and `coba.guru` exist and each logs into its own school. Confirm two `guru` rows in `school_users` with different `school_id`s.

- [ ] **Step 8: Commit any fixups**

```bash
git add -A
git commit -m "chore: verification fixups"
```

---

## Self-Review Notes

- **Spec coverage:** getCurrentSchoolId (T1), create index (T2), duplicate check (T2), update sync (T3), delete sync (T4), Playwright E2E (T5). All spec sections map to tasks.
- **Type consistency:** `SecurityUtils.getCurrentSchoolId()` used in T2/T3/T4; `schoolUserMapper.findByUserIdAndSchool(userId, schoolId)` used in T3/T4 and exists in the mapper; `insertSelective`/`updateByPrimaryKey` are tk.mybatis `Mapper<SchoolUser>` common methods. `UserRequest.getUsername()/getPassword()/getFullname()/getEmail()/getRoleIds()` exist on the DTO.
- **Note:** `UserUpdateRequest` uses the same field getters (`getUsername`, `getFullname`, `getEmail`, `getEnabled`, `getPassword`, `getRoleIds`) — confirmed against the existing `update()` body.
- **Note:** The system-realm user-creation case throws per the spec; update/delete gracefully skip (system realm has no per-school index).
