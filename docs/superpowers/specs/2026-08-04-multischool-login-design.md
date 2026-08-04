# Multi-School Login with School Code + System Realm

## Context

The app is a database-per-tenant school management system. Each school owns its
own PostgreSQL database (`sims_<code>`); a registry DB (`sims_registry`) holds the
school list (`schools`) and a global login index (`school_users`).

Today login is **single-field**: username + password. The registry `school_users`
index maps a *globally unique* username to one school. This blocks the target UX:
the same `admin` username must be able to log into *any* school, plus a special
`system` realm for controlling all schools.

## Goals

- Login form takes **school code + username + password**.
- School code `baru` + valid credentials → dashboard routed to `sims_baru`.
- School code `system` + valid credentials → master dashboard to control all schools.
- A newly created school is immediately login-able with its seeded `admin` user.
- Unknown school code, unknown user, or bad password each give a distinct message.

## Non-goals

- No new "super admin" dashboard UI; the existing School Management pages are reused.
- No user-provisioning UI; only the seeded `admin` is auto-indexed per school.

## 1. Login form

`templates/auth/login.html` gains a **School code** text field between the brand
header and username. Submits to the existing `POST /login`. Placeholder text: the
school code (e.g. `baru`) or `system`.

## 2. Authentication flow

`CustomUserDetailsService.loadUserByUsername` is rewritten to take school code and
username together (a custom `UsernamePasswordAuthenticationToken` carries the code).

```
code == "system"
  └─ no → look up school by code in registry
  │        └─ missing/inactive/deleted → "School not found"
  │        └─ found → resolve school_users index (school_id + username)
  │                    └─ missing/disabled → "User not found in this school"
  │                    └─ found → route to school DB, verify password + RBAC
  │                                (auto-provision DB on first login)
  └─ yes → authenticate against sims_system DB (system realm)
           └─ missing user / bad password → standard auth failure
```

The lookup key changes from globally-unique `username` to `(school_code, username)`.

## 3. System realm — dedicated `sims_system` database

- New Flyway migration set `db/system`:
  - V1: `users` table + `SYSTEM` role + school-management permissions + admin
    menu entries; seed `admin` / `admin123`.
- New `SystemDataSourceManager` mirrors `SchoolDataSourceManager`: auto-creates
  and migrates `sims_system` on first use, caches a Hikari pool.
- `system` is a reserved code; it is never a row in `schools`.

## 4. Routing

- `TenantDataSource` recognizes the routing key `system` → returns the
  `sims_system` pool.
- `CustomUserDetails` carries `schoolDbName` = `sims_system` for system admins.
- `TenantFilter` already routes by `CustomUserDetails.schoolDbName`, so system
  admins automatically hit `sims_system`.

## 5. Registry schema migration

Live `school_users.username` has a global `UNIQUE` constraint. Add `V5`:

```sql
ALTER TABLE school_users DROP CONSTRAINT school_users_username_key;
CREATE UNIQUE INDEX uk_school_users_school_username
    ON school_users(school_id, username) WHERE deleted = false;
```

Usernames become unique per school; the same `admin` can exist in every school.

## 6. Onboarding — auto-create login index

In `SchoolServiceImpl.create()`: after the school DB is provisioned (which seeds
an `admin` user via the school migration set), insert a `school_users` index row:

- `school_id` = the new school's registry id
- `user_id` = 1 (the seeded admin)
- `username` = `admin`

This makes every new school immediately login-able.

## 7. Error handling

Distinct flash messages via the failure handler:
- School code not found / inactive
- User not found in this school
- Invalid credentials
- System realm not configured

## Files touched

- `templates/auth/login.html` — add school code field + i18n keys
- `CustomUserDetailsService` — two-realm auth with school code
- `CustomUserDetails` — already carries school code/db
- `SecurityConfig` — username parameter stays `username`; add `schoolCode` parameter
- `TenantDataSource` — route `system` key
- `RegistrySequenceGenId` — unchanged
- `db/registry/V5__*` — unique-per-school migration
- `db/system/V1__*` — system realm schema + seed
- `SystemDataSourceManager` — new, mirrors school manager
- `SchoolServiceImpl.create()` — auto-index seeded admin

## Verification

1. Compile clean.
2. Create school `baru` → DB `sims_baru` auto-created + migrated + indexed.
3. Login with code `baru` + `admin`/`admin123` → dashboard routed to `sims_baru`.
4. Login with code `system` + `admin`/`admin123` → master dashboard (school pages).
5. Wrong school code / wrong user / wrong password → distinct error messages.
