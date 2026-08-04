# System Realm — Cross-School User Management

## Context

The system realm (`sims_system`) can manage schools (`/schools`) but not users
inside a school. The existing `/users` UI operates in the currently-routed
school DB (via the routing DataSource); a system admin is always routed to
`sims_system`, so they cannot see or manage users of any school. Part 1 (user
login index sync) already ensures hand-created users are login-able; this
feature lets the system admin manage users across all schools from a single
picker-based UI.

## Goals

- System admin can list users of any school.
- System admin can create/edit/delete users in a chosen school.
- A school's own admin keeps the existing `/users` flow unchanged (no picker).
- Created users remain login-able (registry index sync, per Part 1).

## Non-goals

- Aggregating all schools' users in one flat list (per-school listing only).
- Changing the school admin's `/users` behavior.

## 1. `SystemUserService` (new)

A separate service taking `schoolId` as an argument, so the school realm's
mappers (bound to the routing DataSource) operate on the chosen school:

```
SystemUserService
├── PageResult<UserResponse> listBySchool(Long schoolId, String keyword, int page, int size)
├── void create(Long schoolId, UserRequest request)
├── void update(Long schoolId, Long userId, UserUpdateRequest request)
├── void delete(Long schoolId, Long userId)
```

**Per-operation routing** (new pattern): set `TenantContext` in try/finally so
`UserMapper`/`UserRoleMapper`/`RoleMapper` (school realm) hit the chosen school,
and `TenantContext.setTenantId(schoolId)` so index writes carry the right
school id:

```java
TenantContext.setRoutingKey(dbName);
TenantContext.setTenantId(schoolId);
try {
    // school-realm mapper calls + registry SchoolUserMapper calls
} finally {
    TenantContext.clear();
}
```

Because a system admin's `SecurityUtils.getCurrentSchoolId()` returns `null`,
the service sets `TenantContext.setTenantId(schoolId)` itself.

- `dbName` resolved from `SchoolMapper.selectByPrimaryKey(schoolId)` → reject if
  school missing, deleted, or not ACTIVE.
- `create` rejects duplicate username in the school (via `UserMapper.findByUsername`
  under the routing key) and inserts the `school_users` index like Part 1.

## 2. UI — `/users` with school picker (reuse)

- System admin opens `/users` → a **school picker** dropdown lists all ACTIVE
  schools (`SchoolMapper.findAll`); selecting one lists that school's users via
  `SystemUserService.listBySchool`.
- Create/edit/delete go through system endpoints carrying `schoolId`.
- Non-system admin (school admin) sees no picker; `/users` behaves as today
  (normal routing).

## 3. `SystemUserController` (new)

- `GET /system/users?schoolId=...` — list (with picker)
- `GET /system/users/new?schoolId=...` — form
- `POST /system/users` — create (body includes `schoolId`)
- `GET /system/users/{id}/edit?schoolId=...` — edit form
- `POST /system/users/{id}?schoolId=...` — update
- `POST /system/users/{id}/delete?schoolId=...` — delete
- `@PreAuthorize("hasRole('SYSTEM')")` on the controller — system realm only.

Templates reuse `user/list.html` / `user/form.html`, adding the picker for the
system realm. Decision: reuse the existing templates with a conditional school
picker block rendered only when the current principal is the system realm
(`hasRole('SYSTEM')`); the form action stays the same for school admins and
switches to the system endpoint for the system realm. No separate template set.

## 4. Error handling

- Invalid / deleted / inactive `schoolId` → clear error message.
- Duplicate username in the school → `IllegalArgumentException` (400, as Part 1).
- Any failure after routing → transaction rollback (no partial user/index).

## 5. Files touched

- Create: `SystemUserService` (+ `SystemUserServiceImpl`), `SystemUserController`
- Modify: `UserController` (system branch with picker) — or keep separate in
  `SystemUserController`; templates `user/list.html`/`user/form.html` or new
  `system/user/*` templates
- Reuse: `SchoolMapper` (picker), `UserMapper`/`UserRoleMapper`/`RoleMapper`
  (school realm), `SchoolUserMapper` (registry), `SecurityUtils`

## 6. Testing (Playwright E2E)

1. System admin opens `/system/users` → picker lists schools → select `baru` →
   sees `admin`, `guru`.
2. System admin creates user in `baru` → login `baru`/newuser succeeds.
3. System admin edits a user in `baru` → index synced (username or enabled).
4. System admin deletes a user in `baru` → login fails, index soft-deleted.
5. Duplicate username in `baru` → rejected (400).
6. School admin (`coba`) opens `/users` → no picker, normal flow.

## Verification

1. `mvn -o clean compile` passes.
2. Playwright scenarios pass against the running app.
3. Registry `school_users` reflects system-driven create/update/delete.
