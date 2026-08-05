# System Realm — Role, Permission, Menu Management, and System User CRUD

## Context

The system realm (`sims_system`) currently has only two menus: Schools
(`/schools`) and Users (`/system/users`, which manages *school* users). The
existing controllers `/roles`, `/permissions`, `/menus`, `/users` are guarded by
`ROLE_*`, `PERMISSION_*`, `MENU_*`, `USER_*` authorities that do not exist in
the system DB. The system admin needs full CRUD for:

- **User System** — the `sims_system.users` admin accounts themselves (distinct
  from school-user management at `/system/users`).
- **Role** — roles used by system admins (`sims_system.roles`).
- **Permission** — permissions used by system admins.
- **Menu Management** — the sidebar menus shown to system admins.

## Goals

- Four distinct system menus: User Sekolah (existing), User System, Role,
  Permission, Menu Management.
- Full CRUD (list/create/edit/delete) for each of the four new areas, reusing
  the existing templates and services where possible.
- Permissions and menu entries seeded via a Flyway migration.

## Non-goals

- Changing school-realm `/users`, `/roles`, etc. behavior.
- Cross-school user management (that's `/system/users`, already built).

## 1. Migration `db/system/V4__add_rbac_menu_permissions.sql`

Seed the missing authorities and menu rows in `sims_system`:

```
INSERT permissions (on conflict do nothing):
  USER_VIEW, USER_CREATE, USER_EDIT, USER_DELETE,
  ROLE_VIEW, ROLE_CREATE, ROLE_EDIT, ROLE_DELETE,
  PERMISSION_VIEW, PERMISSION_CREATE, PERMISSION_EDIT, PERMISSION_DELETE,
  MENU_VIEW, MENU_CREATE, MENU_EDIT, MENU_DELETE

INSERT menus:
  id=2 rename 'Users' → 'User Sekolah' (url stays /system/users)
  'User System'  → /system/users/system
  'Role'         → /system/roles
  'Permission'   → /system/permissions
  'Menu Management' → /system/menus

Grant all to SYSTEM role (role_permissions + role_menus).
```

## 2. Controllers (new) — `/system/*` routes

All `@PreAuthorize("hasRole('SYSTEM')")` at the class level (system realm only).

- `SystemUserController` — add route `GET/POST /system/users/system` to manage
  `sims_system.users` (system admin accounts, no `school_users` index).
- `SystemRoleController` — `/system/roles` CRUD (list/create/edit/delete),
  reusing `RoleService` + `templates/role/list.html` / `form.html`.
- `SystemPermissionController` — `/system/permissions` CRUD, reusing
  `PermissionService` + templates.
- `SystemMenuController` — `/system/menus` CRUD, reusing `MenuService` +
  templates.

Because a system admin routes to `sims_system`, the existing services
(`RoleService`, `PermissionService`, `MenuService`) already operate on the
system DB — no new service needed for those three.

## 3. System user management (no school index)

`SystemUserService` gains methods for the system realm:
- `listSystemUsers(keyword, page, size)` — reads `sims_system.users`.
- `createSystemUser(UserRequest)` — inserts into `sims_system.users` + assigns
  roles; does NOT create a `school_users` index (system is not a school).
- `updateSystemUser(id, UserUpdateRequest)` / `deleteSystemUser(id)`.

The existing `UserServiceImpl` cannot be reused as-is: its `createUserIndex`
throws when `getCurrentSchoolId()` is null. A separate system-user path avoids
the school-index logic.

## 4. Templates

Reuse existing templates with the system realm already handled:
- `templates/role/list.html`, `form.html`
- `templates/permission/list.html`, `form.html`
- `templates/menu/list.html`, `form.html`
- `templates/user/list.html`, `form.html` (for system users, with the school
  picker hidden and system-user actions)

New `templates/system/user/list.html` + `form.html` for system users — the
existing `user/*` templates hard-code the `schoolId` hidden input and school
picker for the system realm, which is wrong for system-user management (no
school context). The system-user templates drop the school picker and post to
`/system/users/system` without `schoolId`.

## 5. Error handling

- Duplicate username in system realm → `BusinessException` with redirect to the
  form (flash error, same as Part 1/2).
- Invalid/deleted target → `BusinessException` with redirect.

## 6. Files touched

- Create: `db/system/V4__add_rbac_menu_permissions.sql`
- Create: `SystemRoleController`, `SystemPermissionController`,
  `SystemMenuController`; extend `SystemUserController` (+ new service methods)
- Modify: `SystemUserService` / `SystemUserServiceImpl` (system-user methods)
- Reuse: `RoleService`, `PermissionService`, `MenuService`, templates

## 7. Testing (Playwright E2E)

1. System admin logs in → sidebar shows: User Sekolah, User System, Role,
   Permission, Menu Management.
2. `/system/roles` → create a role → appears in list.
3. `/system/permissions` → create a permission → appears.
4. `/system/menus` → create a menu → appears.
5. `/system/users/system` → create a system admin → login with that account
   works (system realm).
6. School admin's `/users` flow unchanged.

## Verification

1. `mvn -o clean compile` passes.
2. Playwright scenarios pass against the running app.
3. `sims_system` flyway at V4 with menus/permissions present.
