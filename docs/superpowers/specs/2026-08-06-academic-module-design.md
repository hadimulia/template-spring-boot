# Academic Module — Students / Teachers / Classes

## Context

The system has database-per-tenant: each school owns `sims_<code>` where its
users, roles, permissions and menus live; the registry (`sims_registry`) holds
the school list and the `school_users` login index. A school admin manages
users, roles, permissions, menus today. There is no academic data yet.

This module adds the first academic entities: **teachers**, **students**, and
**classes**, stored in the school database. Creating a teacher or student also
creates a school login account automatically (user in the school DB + a
`school_users` registry index), so they can sign in to their school with their
own role.

## Goals

- CRUD teachers in a school (routed normally, like the users module).
- CRUD students in a school.
- CRUD classes; a class has a homeroom teacher and a list of students.
- Creating a teacher/student auto-creates a login account (user + school_users
  index) with a `TEACHER`/`STUDENT` role.
- New sidebar menus (Akademik: Guru, Murid, Kelas) and permissions visible to
  school admins.

## Non-goals

- Cross-school academic management from the system realm (deferred).
- Attendance, grading, scheduling, payments — future modules.
- School/fee/billing integration.

## 1. Schema (school migration `V21`)

Tables in the school database, following the existing entity style
(`BaseEntity` gives id/created/updated/deleted/version):

```
students (
  id BIGSERIAL PK (via students_id_seq),
  user_id BIGINT NOT NULL REFERENCES users(id),
  nis VARCHAR(20) UNIQUE NOT NULL,          -- student number, used as username
  fullname VARCHAR(100) NOT NULL,
  gender VARCHAR(10),
  birth_date DATE,
  address TEXT,
  phone VARCHAR(20),
  email VARCHAR(100),
  enrollment_status VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE / INACTIVE
  created_by, created_date, updated_by, updated_date, deleted, version
)

teachers (
  id BIGSERIAL PK,
  user_id BIGINT NOT NULL REFERENCES users(id),
  nip VARCHAR(20) UNIQUE NOT NULL,          -- teacher number, used as username
  fullname VARCHAR(100) NOT NULL,
  gender VARCHAR(10),
  birth_date DATE,
  address TEXT,
  phone VARCHAR(20),
  email VARCHAR(100),
  hire_date DATE,
  created_by, created_date, updated_by, updated_date, deleted, version
)

classes (
  id BIGSERIAL PK,
  name VARCHAR(100) NOT NULL,
  grade VARCHAR(20),
  academic_year VARCHAR(20),
  homeroom_teacher_id BIGINT REFERENCES teachers(id),
  created_by, created_date, updated_by, updated_date, deleted, version
)

class_students (
  id BIGSERIAL PK,
  class_id BIGINT NOT NULL REFERENCES classes(id),
  student_id BIGINT NOT NULL REFERENCES students(id),
  created_by, created_date, updated_by, updated_date, deleted, version,
  UNIQUE(class_id, student_id)
)
```

Also seed in the same migration:
- Roles `TEACHER`, `STUDENT`.
- Permissions `TEACHER_VIEW/CREATE/EDIT/DELETE`, `STUDENT_VIEW/CREATE/EDIT/
  DELETE`, `CLASS_VIEW/CREATE/EDIT/DELETE`.
- Menus under a `Akademi` parent: `Guru` (`/teachers`), `Murid` (`/students`),
  `Kelas` (`/classes`).
- Grant TEACHER/STUDENT roles the right permissions and menus; grant the new
  school-admin access so admins see the academic menus.

## 2. Auto login account on teacher/student create

`TeacherService.create` / `StudentService.create`:
1. Derive username = the NIP/NIS.
2. Check `users.username` uniqueness within the school (routing DS); reject
   duplicates.
3. Insert a `users` row with role `TEACHER` or `STUDENT` (assign via
   `user_roles`) and an encoded password (default `password123`).
4. Insert a `school_users` index row (registry) so the account is login-able,
   mirroring the existing user-create logic (Part 1 / SystemUserService).

## 3. Controllers / services

Follow the existing `UserController`/`UserServiceImpl` pattern:
- `TeacherController` (`/teachers`) + `TeacherService` (`@Transactional`,
  routing DS — the school admin is already routed to their school).
- `StudentController` (`/students`) + `StudentService`.
- `ClassController` (`/classes`) + `ClassService`.
- Each controller method guards with the matching authority: `TEACHER_VIEW`,
  `TEACHER_CREATE`, `TEACHER_EDIT`, `TEACHER_DELETE` (and the `STUDENT_*` /
  `CLASS_*` equivalents), seeded in V21.
- Templates under `templates/teacher/*`, `templates/student/*`,
  `templates/classes/*`, modeled on `templates/user/*`.

Delete is soft (set `deleted=true` on the user, profile, and registry index).

## 4. Files touched

- Create: `db/migration/V21__academic_module.sql`
- Create: entity `Student`, `Teacher`, `ClassEntity`, `ClassStudent`
- Create: mapper `StudentMapper`/`TeacherMapper`/`ClassMapper` (+XML)
- Create: `TeacherController`/`StudentController`/`ClassController`,
  `TeacherService`/`StudentService`/`ClassService`
- Create: templates `teacher/*`, `student/*`, `class/*`
- Modify: i18n messages for new labels

## 5. Testing (Playwright E2E)

1. School admin (`coba`) sees Akademi > Guru/Murid/Kelas menus.
2. Create a teacher → verify a `users` row with TEACHER role + `school_users`
   index; log in with the NIP/password → dashboard routed to the school.
3. Create a student → verify user + index + STUDENT role; login works.
4. Create a class with a homeroom teacher; add students to it.
5. Edit/delete teacher/student → login disabled on delete, index soft-deleted.
6. Duplicate username (NIP/NIS) rejected with a form error.

## Verification

1. `mvn -o clean compile` passes.
2. School DB migrated to V21 with the new tables/menus/permissions.
3. Playwright scenarios pass.