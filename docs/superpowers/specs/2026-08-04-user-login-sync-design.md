# User Management — Sync Login Index for New Users

## Context

The app is database-per-tenant: each school owns a PostgreSQL database
(`sims_<code>`); a registry DB (`sims_registry`) holds the school list (`schools`)
and a global login index (`school_users`) mapping `(school_id, username)` to a
`user_id` inside that school's database.

Today, creating a user via the `/users` UI (`UserServiceImpl.create`) writes the
user to the school database and assigns roles, but **does not** create a
`school_users` entry. Because login resolves the user through the registry index
(`CustomUserDetailsService.findBySchoolAndUsername`), a newly created user
cannot log in. (The seed `admin` already works because onboarding auto-indexes
it; hand-created users are missing the index.)

## Goals

- A user created by a school admin is immediately login-able.
- Updating a user's username keeps the registry index in sync.
- Soft-deleting a user soft-deletes the registry index.
- Username uniqueness is enforced **per school** (consistent with registry V5).

## Non-goals

- Cross-school user management from the `system` realm (deferred to a later task).
- Creating users in a different school than the one currently being accessed.

## 1. Current-school id helper

`SecurityUtils` gains `getCurrentSchoolId()`: reads `CustomUserDetails.getSchoolId()`
from the security context. Returns `null` for the system realm (schoolId is null).
Used by `UserServiceImpl` to know which school's index to write.

## 2. Create user — sync the login index

In `UserServiceImpl.create`, after inserting the user and assigning roles:

1. Resolve `schoolId = SecurityUtils.getCurrentSchoolId()`. If null (system
   admin creating a user), throw `IllegalStateException("No school context for
   user creation")` — a user without an index cannot log in, which is exactly
   the bug being fixed, so never silently skip indexing.
2. Insert a `SchoolUser` index row:
   - `schoolId` = current school id
   - `userId` = new user id
   - `username` = new username
   - `enabled` = true
   - `createdBy` / `deleted` set like `SchoolServiceImpl.create`
   via `SchoolUserMapper.insertSelective` (registry factory, like onboarding).

## 3. Username uniqueness per school

Before inserting, check the school DB for an existing `users.username`
(`userMapper.findByUsername`). Since the school DB's `users.username` is
globally unique within that DB, this enforces per-school uniqueness. Reject
with a clear message if it exists.

## 4. Update user — sync username

In `UserServiceImpl.update`, if `request.getUsername()` differs from the stored
value, after updating the user also update the matching `SchoolUser` row
(`findByUserIdAndSchool(userId, schoolId)` then `updateByPrimaryKey`), so the
registry index follows the rename.

## 5. Delete user — soft-delete index

In `UserServiceImpl.delete`, after soft-deleting the user, soft-delete the
matching `SchoolUser` row (`setDeleted(true)` + update).

## 6. Error handling

- Duplicate username in the school → `IllegalArgumentException` with a clear
  message (surfaced by the existing controller redirect, as in `SchoolController`).
- Registry index insert/update failure → the `@Transactional` on the service
  rolls back the whole operation, so no user exists without an index.

## 7. Files touched

- `SecurityUtils.java` — add `getCurrentSchoolId()`
- `UserServiceImpl.java` — create/update/delete sync logic + duplicate check
- `SchoolUserMapper.java` — no new methods needed (tk common + `findByUserIdAndSchool` exist)

## 8. Testing (Playwright E2E)

Automated browser tests against the running app:

1. **Create user can log in** — as school `xyz` admin, create user `guru` /
   password; log out; log in with school `xyz` + `guru`; expect dashboard
   routed to `sims_xyz`.
2. **Duplicate username rejected** — create a second user with the same
   username in the same school; expect an error message, no second index row.
3. **Update username syncs index** — rename user; verify `school_users` updated
   and login still works with the new username.
4. **Delete disables login** — soft-delete user; verify login fails with
   "User not found in this school".
5. **Same username allowed in different schools** — create `admin` in a second
   school; both schools' `admin` log in independently.

Playwright tests are manual/browser-driven in this repo (no CI runner), matching
the existing verification approach.

## Verification

1. `mvn -o clean compile` passes.
2. Playwright scenarios above pass against the running app.
3. Registry `school_users` rows reflect create/update/delete correctly.
