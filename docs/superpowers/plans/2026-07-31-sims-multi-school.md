# SIMS Multi-School Implementation Plan (Phase 0–6)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Status:** Phase 0 in progress. Tasks 0.1–0.5 and 0.7 (routing DataSource, two-realm login, tenant_id removal) are done and verified — app boots, login as `admin` auto-provisions `sims_default` and routes all queries to it. Remaining: Task 0.6 (school onboarding UI + Tenant→School rename), Task 0.8 (compose config, smoke test, README). Phases 1–6 cannot start until 0.6/0.8 complete.

**Goal:** Turn the existing single-tenant Spring Boot RBAC template into a **multi-school SaaS** (Sistem Informasi Manajemen Sekolah — SIMS). Each school is a separate tenant backed by its **own PostgreSQL database** (database-per-tenant isolation). Phase 0 migrates the current row-level `tenant_id` multi-tenancy to database-per-tenant routing; Phases 1–6 build the six SIMS modules.

**Source-of-truth domain document:** `SIMS.md` (in repo root).

**Architecture:** Modular monolith (monorepo, single deployable) with layered `Controller → Service → Mapper` and MyBatis/tk.mybatis, as in the existing template. A **Registry DB** holds school metadata; a routing `DataSource` switches to the school's database per request based on the authenticated user.

---

## Global Constraints

- **Java:** 21, **Spring Boot:** 3.3.2 (existing)
- **Build:** Maven, **View:** Thymeleaf + Bootstrap 5 + jQuery (existing)
- **ORM:** MyBatis + tk.mybatis (existing)
- **Database:** PostgreSQL 18.4 (per-school DB + one registry DB)
- **Migrations:** Flyway per database (registry DB has its own migrations; each school DB runs the same application migration set on creation)
- **Auth:** Spring Security, BCrypt; roles/permissions/menus stay **per-school** (each school has its own copy of reference data)
- **Password Hashing:** BCrypt
- **Language of UI:** Indonesian labels (school system); i18n via existing `SessionLocaleResolver` (en/id)

---

## Architecture Overview (Target)

```
[ Browser ]
     │  (HTTPS / Nginx optional)
[ Spring Boot app ]  ← single instance, modular monolith
     │
     ├─ Registry DataSource (rw)   →  db `sims_registry`   (schools, users→school map, global system users)
     └─ Routing DataSource (per-request)
             ├─ school A  →  db `sims_<schoolcode>`   (all school business data)
             ├─ school B  →  db `sims_<schoolcode>`   (same schema, separate database)
             └─ ...
```

**Two logical data realms:**
1. **Registry realm** (shared, one database): schools, system admin users, and the mapping `user → school`.
2. **School realm** (per school, one database each): all business data — users, roles, permissions, menus, classes, students, teachers, grades, raports, SPMB applications, finance, HR. Reference data (roles/permissions/menus) is duplicated per school so each school can customize its menus and permissions.

**Tenant resolution flow (unchanged in spirit, changed in mechanism):**
1. Login hits the **registry** DB (global user lookup by username).
2. `CustomUserDetails` carries `schoolId` + `schoolCode`.
3. `TenantFilter` reads it and calls `TenantContext.setTenantId(schoolId)` and sets the **routing key** on the routing DataSource.
4. Every mapper in the school realm now hits the school's database — **no `tenant_id` WHERE filters needed anymore** (isolation is physical).

---

## Phase 0 — Database-per-Tenant Migration

**Goal:** Replace row-level `tenant_id` filtering with per-school databases. After this phase the app boots, logs in, and every existing page works against a school database, with `tenants` becoming the school-onboarding UI that **auto-creates** a database.

> **Decision (confirmed):** Registry DB + auto-create. When a school is created in the UI, the system creates a new PostgreSQL database `sims_<code>` and runs the Flyway migration set on it. No static config mapping. Data existing in the current shared DB is dev/seed only — **abandoned**, no production migration strategy.

### Task 0.1: Registry DB schema and migrations

**Files:**
- Create: `src/main/resources/db/registry/V1__create_registry_schema.sql` (new Flyway location `classpath:db/registry`)
- Create: `src/main/resources/db/registry/V2__seed_registry.sql`

**Interfaces:**
- Produces: the registry database (`sims_registry`) with `schools`, `school_users` (or reuse a registry `users`), and the `user→school` mapping.

- [x] **Step 1:** Create `V1__create_registry_schema.sql` with:
  - `schools` → `tenants` table: `id BIGSERIAL PK`, `code VARCHAR(50) UNIQUE NOT NULL`, `name VARCHAR(100) NOT NULL`, `db_name VARCHAR(63) UNIQUE NOT NULL`, `status VARCHAR(20) DEFAULT 'ACTIVE'`, `description`, `created_by`, `created_date`, `updated_by`, `updated_date`, `deleted BOOLEAN DEFAULT false`, `version INT DEFAULT 0`.
  - `school_users` mapping: `id BIGSERIAL PK`, `tenant_id FK`, `user_id`, `username` (login name scoped to school), `enabled`, audit fields. (Credential/password rows live in the school DB's `users` table; the registry only indexes login.)
- [x] **Step 2:** Create `V2__seed_registry.sql`: insert a `DEFAULT` school (code `DEFAULT`, db_name `sims_default`), and a mapping for `admin → DEFAULT`.

**Verification:** `sims_registry` exists with the tables and seed rows.

### Task 0.2: Per-school application migrations (multi-tenant Flyway)

**Files:**
- Modify: `src/main/resources/db/migration/` (make it the **school** migration set, shared by every school DB)
- Create: `src/main/resources/db/migration/V18__rebuild_for_school_realm.sql`

**Interfaces:**
- Produces: a single Flyway migration set that each school database runs on creation. The old `tenant_id` columns and the shared-DB `tenants` table are removed from the school schema.

- [x] **Step 1:** Reframe `db/migration` as the per-school migration set. `V18__drop_tenant_columns.sql`:
  - Drops `tenant_id` from `users`, `user_roles`, `audit_logs`, `approval_requests`, `file_uploads`, `notifications`.
  - Drops the `tenants` table and the FK constraints/indexes that referenced it.
- [x] **Step 2:** The `TENANT_*` permissions and `/tenants` menu seed from V17 are KEPT in the school set (each school gets its own copies). The school-management page reads from the registry database via registry mappers instead of a local `tenants` table.

**Verification:** `flyway migrate` against a scratch school DB produces the full school schema with **no** `tenant_id` columns.

### Task 0.3: Routing DataSource and connection management

**Files:**
- Create: `src/main/java/com/template/config/TenantDataSource.java` (an `AbstractRoutingDataSource`)
- Create: `src/main/java/com/template/config/RegistryDataSourceConfig.java`
- Create: `src/main/java/com/template/config/SchoolDataSourceManager.java` (create/drop school DBs, per-DB Hikari pool cache)
- Modify: `src/main/resources/application.yml` (two datasource sections + Flyway config for both locations)
- Modify: `src/main/java/com/template/TemplateApplication.java` (disable default single DataSource autoconfig; enable the routing one)

**Interfaces:**
- Produces: `DataSource` bean that routes by `TenantContext` routing key. A `SchoolDataSourceManager` that, given a school code, returns (and lazily creates) a Hikari `DataSource` for `sims_<code>` and runs Flyway against it the first time.

- [x] **Step 1:** Configure `spring.datasource.registry.*` and `spring.datasource.school.*` template connection info in `application.yml`. Set the school DB naming rule: `sims_<schoolcode>` (lowercase, sanitized). Flyway is disabled globally; the registry DB migrates at startup and each school DB is migrated by `SchoolDataSourceManager` on first creation.
- [x] **Step 2:** Implement `TenantDataSource extends AbstractRoutingDataSource`. `determineCurrentLookupKey()` returns `TenantContext.getRoutingKey()` (school db name). Default fallback datasource = registry DB for pre-login / registry pages.
- [x] **Step 3:** Implement `SchoolDataSourceManager`: `getOrCreate(schoolCode)` → `sims_<code>`, checks `pg_database`, creates with `CREATE DATABASE` if missing, builds a Hikari pool, runs school Flyway migrations once. Added `getOrCreateByDbName(dbName)` for the routing DataSource whose key is already a db name (avoids `sims_sims_<code>` double-prefix).
- [x] **Step 4:** Wire the routing DataSource as the primary `DataSource`; register the registry DataSource under a qualifier. Two explicit `SqlSessionFactory` beans — `schoolSqlSessionFactory` (routing DS, `classpath:mapper/**/*.xml`, `@Primary`) and `registrySqlSessionFactory` (registry DS, `classpath:registry-mapper/**/*.xml`) — each `@Qualifier`-pinned because `@Primary` silently wins over parameter names.
- [x] **Step 5:** In `TenantContext`, add `routingKey` (school db name) alongside the numeric id; `TenantFilter` populates both from `CustomUserDetails`.

**Verification:** App boots with two DataSources; on login as `admin@DEFAULT`, the routing key is set and queries hit `sims_default`.

### Task 0.4: Update login and user lookup for two realms

**Files:**
- Modify: `src/main/java/com/template/security/CustomUserDetailsService.java`
- Modify: `src/main/java/com/template/security/CustomUserDetails.java`
- Modify: `src/main/java/com/template/controller/AuthController.java` (accept school code / scope on login)
- Modify: `src/main/java/com/template/tenant/TenantFilter.java`

**Interfaces:**
- Produces: authentication that resolves both the user **and** the school database before the request proceeds.

- [x] **Step 1:** On login, resolve credentials against the **registry** realm (`school_users` by `username`). Look up the mapped school via `tenants` (registry) and set the routing key.
- [x] **Step 2:** Add `schoolId`, `schoolCode`, `schoolDbName` to `CustomUserDetails`.
- [x] **Step 3:** `TenantFilter` sets `TenantContext.setTenantId(schoolId)` and `setRoutingKey(schoolDbName)`; clears in `finally` as today. `CustomUserDetailsService` sets the routing key before loading school credentials and clears it in `finally`.
- [x] **Step 4:** Login UX: single username global lookup (matches current form). Global `school_users` index per school; credentials/RBAC loaded from the school DB.
- [x] **Step 5:** Update `CustomAuthenticationFailureHandler` / `SessionService` to query the registry realm via `SchoolUserMapper`.

**Verification:** Login as registry admin lands in the DEFAULT school DB; school staff login lands in their own school DB.

### Task 0.5: Remove tenant_id scoping from mappers/services

**Files:**
- Modify: every school-realm mapper XML that currently has `<if test="tenantId != null">AND tenant_id = #{tenantId}</if>` guards
- Modify: `UserMapper`, `FileUploadMapper`, `NotificationMapper`, `AuditLogMapper`, `ApprovalRequestMapper` and their service impls
- Modify: `src/main/java/com/template/service/generic/GenericServiceImpl.java`
- Modify: `src/main/java/com/template/config/AuditInterceptor.java`
- Modify: `src/main/java/com/template/controller/DashboardController.java`

**Interfaces:**
- Produces: school-realm queries with **no** tenant_id condition — isolation is now physical (per-DB). `TenantContext.getTenantId()` is no longer needed inside school SQL.

- [x] **Step 1:** Stripped `tenant_id` guards from school-realm mapper XMLs; dropped `tenantId` params from mapper interfaces/services where used only for scoping (`findByUsername`, `findByEntity`, `findByUserId`, `countByUserId`, `countUnreadByUserId`, `markAsRead`, `markAllAsRead`, `findAll`).
- [x] **Step 2:** Removed `TenantContext.getTenantId()` usage from `SecurityUtils`, `UserServiceImpl`, `NotificationServiceImpl`, `FileUploadServiceImpl`, `ExcelImport/Export/Pdf`, `DashboardController`. Dashboard `totalUsers` → `countAllUsers()`.
- [x] **Step 3:** Removed `tenant_id` stamping; dropped `tenant_id` from all school tables via `V18`.
- [x] **Step 4:** Updated services reading `TenantContext.getTenantId()` to drop the param.

**Verification:** `mvn compile` passes; every existing page (users/roles/menus/audit/files/notifications/approvals/sessions) works against the school DB with no tenant filters.

### Task 0.6: School onboarding (auto-create database)

**Files:**
- Modify: `src/main/java/com/template/service/tenant/TenantService.java` / `TenantServiceImpl.java`
- Modify: `src/main/java/com/template/controller/tenant/TenantController.java`
- Modify: `src/main/resources/templates/tenant/*.html`
- Modify: `src/main/resources/static/js/tenants/data-render.js`
- Modify: `src/main/java/com/template/entity/tenant/Tenant.java` (now `School`)
- Modify: `src/main/resources/db/registry/` (insert into registry `schools`)

**Interfaces:**
- Produces: a `School` CRUD where "create school" runs `SchoolDataSourceManager.getOrCreate(code)` → creates `sims_<code>`, runs migrations, and records the school in the registry `schools` table.

- [ ] **Step 1:** Rename domain concept `Tenant` → `School` in UI/labels (keep code package `tenant` or rename to `school` — choose one; plan recommends renaming to `school` for clarity).
- [ ] **Step 2:** On `save`, call `SchoolDataSourceManager.getOrCreate(code)` before persisting registry row; roll back the registry insert if DB creation fails.
- [ ] **Step 3:** On delete, mark school `INACTIVE` (do not drop the database immediately — safer); add a separate "purge" action guarded by confirmation for dropping the DB.
- [ ] **Step 4:** Seed each new school DB with default roles/permissions/menus via the school Flyway set (extend `V3__insert_initial_data.sql` pattern).
- [ ] **Step 5:** Update i18n keys `sidebar.tenant` → `sidebar.school`, `tenant.*` → `school.*` in `messages.properties` + `messages_id.properties`.

**Verification:** Creating a school from the UI results in a new `sims_<code>` database with a full migrated schema; the school appears in the registry `schools` table.

### Task 0.7: Global (registry) data access

**Files:**
- Create: `src/main/java/com/template/mapper/school/SchoolMapper.java` + `SchoolMapper.xml` (or keep `tenant` naming)
- Create: `src/main/java/com/template/mapper/registry/RegistryUserMapper.java` + XML
- Modify: any mapper/service that must read from the registry realm regardless of the routing key

**Interfaces:**
- Produces: explicit access to registry tables for flows that must not be school-scoped (school list page, system-admin login).

- [x] **Step 1:** Registry mappers (`SchoolUserMapper`, `TenantMapper`) in `com.template.registry.mapper` bound to the registry `SqlSessionTemplate` via `@MapperScan(basePackages = "com.template.registry.mapper", sqlSessionTemplateRef = "registrySqlSessionTemplate")`. XMLs under `classpath:registry-mapper`.
- [ ] **Step 2:** `TenantController`/`SchoolController` list + CRUD reads from the registry mapper, not the routing datasource. (Uses school-realm `TenantMapper` currently — pending Task 0.6 rework.)
- [x] **Step 3:** Pre-login / registry pages route via the registry fallback datasource.

**Verification:** The schools list page reads from `sims_registry` while other pages read from the routed school DB.

### Task 0.8: Configuration, tests, and rollback

**Files:**
- Create: `src/main/resources/application-dev.yml` (registry + school connection info), already exists — extend it
- Modify: `pom.xml` (add `org.postgresql:postgresql` if not present; Hikari already present)
- Create: `src/test/java/...` (config + smoke tests)
- Modify: `README.md`

**Interfaces:**
- Produces: reproducible dev setup and a documented rollback path.

- [ ] **Step 1:** Provide a `docker-compose.yml` service that creates `sims_registry` and a template school DB (or rely on auto-create). Update compose env vars.
- [ ] **Step 2:** Add a smoke test: app context loads with routing DataSource; a login as `admin@default` reaches the school DB.
- [ ] **Step 3:** Document in `README.md`: how to create a school, how the DB auto-creation works, required DB permissions (the app's registry user must be able to run `CREATE DATABASE` and connect to new DBs).
- [ ] **Step 4:** Rollback note: keep a git tag/branch of the row-level-tenancy state. Because dev data is seed-only, downgrade = checkout + fresh migrations; no data-migration tooling needed.

**Phase 0 exit criteria:** app boots, registry + routing data sources work, login resolves school DB, all existing modules render against a school DB with no `tenant_id` filters, and creating a school auto-provisions its database.

---

## Phase 1 — Master Data (Manajemen Pengguna & Data Sekolah)

**Goal:** Master data for the school realm — teachers, students, classes, and the user accounts that map to them. Extends the existing User module.

### Task 1.1: Teacher and Student entities

**Files:**
- Create: `src/main/java/com/template/entity/academic/Teacher.java`
- Create: `src/main/java/com/template/entity/academic/Student.java`
- Create: `src/main/java/com/template/entity/academic/StudentClass.java` (kelas)
- Create: `src/main/resources/db/migration/V19__master_data.sql`
- Create: mappers + services + controllers for each (mirror existing User stack)

**Interfaces:**
- Produces: CRUD for teachers, students, and classes, linked to `users` where the person has a login.

- [ ] **Step 1:** `V19` DDL: `teachers(id, user_id FK, nip, name, gender, phone, address, subject_specialization, ...)`, `students(id, user_id FK, nis, name, gender, birth_date, birth_place, class_id FK, guardian_name, guardian_phone, ...)`, `classes(id, name, grade, homeroom_teacher_id FK, academic_year, ...)`.
- [ ] **Step 2:** Teacher/Student forms with validation (NIP/NIS uniqueness per school). Link `user_id` optional (not every teacher/student has a login yet).
- [ ] **Step 3:** Class management with homeroom teacher (wali kelas) assignment and academic year.

**Verification:** CRUD for guru/siswa/kelas works; a teacher/student can be optionally linked to an existing `users` row.

### Task 1.2: User account management for school staff

**Files:**
- Modify: `src/main/java/com/template/service/user/UserServiceImpl.java`
- Modify: `src/main/resources/templates/user/form.html`
- Modify: `src/main/java/com/template/dto/user/*`

**Interfaces:**
- Produces: user CRUD within a school realm (no tenant_id), with role assignment from the school's role set.

- [ ] **Step 1:** Remove `tenantId` from `UserRequest`/`UserResponse` and the create/update flows (school realm now).
- [ ] **Step 2:** Add a "profile type" link so a `users` row can be connected to a Teacher or Student.
- [ ] **Step 3:** Keep username uniqueness per school DB (natural — unique per database).

**Verification:** Admin creates a user inside the school; login works; roles assign from the school's own role set.

### Task 1.3: School profile & academic settings

**Files:**
- Create: `src/main/java/com/template/entity/academic/SchoolProfile.java`
- Create: `src/main/resources/db/migration/V20__school_profile.sql`
- Create: service + controller + templates

**Interfaces:**
- Produces: a per-school settings record (school name, address, logo, academic years, grading scale).

- [ ] **Step 1:** DDL `school_profile` (single-row per DB): `id, school_name, address, phone, email, logo_path, current_academic_year, grading_scale JSONB, ...`.
- [ ] **Step 2:** Settings page under Master; seed defaults in V20.

**Verification:** Each school DB has its own profile record, editable from the UI.

---

## Phase 2 — Akademik (Jadwal, Absensi, Ujian)

**Goal:** Scheduling, attendance, and online-exam (CBT) support.

### Task 2.1: Subject and teaching assignment

**Files:**
- Create: `Subject`, `TeachingAssignment` entities + migrations `V21`
- Create: mappers/services/controllers/templates

**Interfaces:**
- Produces: subjects per school and which teacher teaches which subject in which class.

- [ ] **Step 1:** DDL `subjects(id, code, name, ...)`, `teaching_assignments(id, teacher_id, subject_id, class_id, academic_year, ...)`.
- [ ] **Step 2:** CRUD + assignment UI (assign guru → mapel → kelas).

### Task 2.2: Schedule (Jadwal Pelajaran)

**Files:**
- Create: `LessonSchedule` entity + migration `V22`
- Create: service/controller/templates + a timetable renderer (grid view)

**Interfaces:**
- Produces: weekly timetable per class, with conflict detection.

- [ ] **Step 1:** DDL `lesson_schedules(id, class_id, subject_id, teacher_id, day_of_week, period, room, ...)`.
- [ ] **Step 2:** Timetable grid by class and by teacher; block double-booking of the same teacher/class/period (validation).

### Task 2.3: Attendance (Absensi)

**Files:**
- Create: `Attendance` entity + migration `V23`
- Create: service/controller/templates + QR check-in (optional)

**Interfaces:**
- Produces: daily attendance capture for students (and optionally teachers), with monthly summaries.

- [ ] **Step 1:** DDL `attendances(id, student_id, class_id, teacher_id, date, status(PRESENT/ABSENT/SICK/PERMIT), note, ...)`.
- [ ] **Step 2:** Bulk entry per class per date; per-student history; monthly report table.
- [ ] **Step 3:** (Optional) QR code per student for check-in; mark AMBIGUOUS for scope.

### Task 2.4: Online exam (CBT) — optional, Phase 2 stretch

**Files:**
- Create: `Exam`, `ExamQuestion`, `ExamAttempt` entities + migration `V24`
- Create: service/controller/templates

**Interfaces:**
- Produces: teacher-created question banks and student attempts with auto-scoring for objective questions.

- [ ] **Step 1:** DDL `exams(id, class_id, subject_id, title, duration, start_time, end_time, ...)`, `exam_questions(id, exam_id, type(OBJECTIVE/ESSAY), prompt, options JSONB, answer, points)`, `exam_attempts(id, exam_id, student_id, started_at, submitted_at, score)`.
- [ ] **Step 2:** Teacher builds an exam from a question bank; student answers; objective auto-score, essay manual review.

**Verification:** End-to-end CBT: create exam → student takes it → score recorded.

---

## Phase 3 — E-Raport

**Goal:** Formative & summative grades, automated computation, and PDF raport export.

### Task 3.1: Grade book (nilai formatif & sumatif)

**Files:**
- Create: `Grade` entity + migration `V25`
- Create: service/controller/templates

**Interfaces:**
- Produces: per-student, per-subject, per-competency grades with the two assessment types.

- [ ] **Step 1:** DDL `grades(id, student_id, subject_id, teacher_id, class_id, academic_year, semester, type(FORMATIF/SUMATIF), competency_code, score, ...)`.
- [ ] **Step 2:** Teacher entry UI; validation that scores are in the configured scale (from school profile).

### Task 3.2: Rapor computation (Perhitungan Otomatis)

**Files:**
- Create: `RaporService.java`
- Create: `Rapor` entity + migration `V26`

**Interfaces:**
- Produces: computed final raport per student/semester: formula scores, average, rank, and final decision (naik/tinggal kelas).

- [ ] **Step 1:** Define formula: final = weighted(formatif, sumatif) per subject; aggregate into a per-semester transcript.
- [ ] **Step 2:** `Rapor` table storing the computed snapshot (so a published raport is immutable even if grades change).
- [ ] **Step 3:** Automatic class ranking; pass/fail rules based on thresholds in school profile.

### Task 3.3: PDF export (Raport PDF)

**Files:**
- Create: `RaporPdfService.java`
- Create: templates for the raport layout

**Interfaces:**
- Produces: a printable, per-student raport PDF.

- [ ] **Step 1:** Extend the existing export stack (`OpenPDF` — check dependency; existing PDF export uses the same lib) with a raport layout: header (school profile + logo), transcript table, decision.
- [ ] **Step 2:** Generate per-student PDF; batch print per class.

**Verification:** Enter grades → compute raport → export PDF shows correct weighted values and decision.

### Task 3.4: Approval workflow for raport publication

**Files:**
- Modify: `src/main/java/com/template/service/approval/*` (reuse approval flow)
- Create: `RaporPublishController`

**Interfaces:**
- Produces: a "draft → submitted → approved → published" state for raports, using the existing `approval_requests` machinery.

- [ ] **Step 1:** Teacher submits raport for review; principal approves; only approved raports are printable.
- [ ] **Step 2:** Reuse `ApprovalRequestMapper` + notification on approval.

**Verification:** A raport cannot be printed until approved.

---

## Phase 4 — SPMB (Pendaftaran / Admission)

**Goal:** Online registration, document upload, verification & status tracking.

### Task 4.1: Registration application

**Files:**
- Create: `SpmbApplication` entity + migration `V27`
- Create: service/controller/templates (public + admin)

**Interfaces:**
- Produces: an online admission form with status lifecycle.

- [ ] **Step 1:** DDL `spmb_applications(id, applicant_name, nisn, birth_date, parent_name, phone, email, address, desired_class_id, status(NEW/VERIFIED/ACCEPTED/REJECTED), submitted_at, ...)`.
- [ ] **Step 2:** Public registration page (no login); admin review list.

### Task 4.2: Document upload & verification

**Files:**
- Modify: `src/main/java/com/template/service/file/FileUploadService.java` (reuse)
- Create: verification UI

**Interfaces:**
- Produces: applicants upload documents (raport/KK/akte) tied to their application; admin verifies.

- [ ] **Step 1:** Reuse `file_uploads` (entity = SPMB_APPLICATION, entity_id = application id). Add per-document type labels.
- [ ] **Step 2:** Admin verification page: view docs, set status, use the existing approval flow for acceptance decisions.

### Task 4.3: Selection & announcement

**Files:**
- Create: `SpmbSelectionService.java`

**Interfaces:**
- Produces: score/rank-based selection (from CBT exam scores if used) and acceptance list.

- [ ] **Step 1:** Optional selection scoring (combine CBT score + document completeness).
- [ ] **Step 2:** Publish accepted list per school; email/notification to applicant if available.

**Verification:** Register → upload docs → verify → accept; status visible to admin and applicant.

---

## Phase 5 — Keuangan (SPP & Arus Kas)

**Goal:** Tuition (SPP) payments and cash-flow tracking.

### Task 5.1: Payment configuration

**Files:**
- Create: `PaymentConfig`, `Fee` entities + migration `V28`
- Create: service/controller/templates

**Interfaces:**
- Produces: fee structure per class/grade/period.

- [ ] **Step 1:** DDL `fees(id, class_id, fee_type(SPP/DSP/OTHER), amount, academic_year, semester, ...)`, `payment_configs(...)`.

### Task 5.2: SPP billing & payment records

**Files:**
- Create: `Payment`, `PaymentRecord` entities + migration `V29`
- Create: service/controller/templates

**Interfaces:**
- Produces: per-student monthly SPP invoices and payment records with remaining balance.

- [ ] **Step 1:** DDL `payments(id, student_id, fee_id, period, amount, paid_at, method, status(PAID/OVERDUE), reference_no, ...)`. Auto-generate monthly invoices for active students.
- [ ] **Step 2:** Payment entry (cash/transfer); receipt number; per-student billing history and outstanding balance.

### Task 5.3: Cash-flow reports

**Files:**
- Create: `FinanceReportService.java`
- Create: report templates (list + PDF)

**Interfaces:**
- Produces: income summaries per period, per fee type; trial balance view.

- [ ] **Step 1:** Aggregations: total SPP collected per month, by class, by fee type; outstanding arrears.
- [ ] **Step 2:** Export monthly cash-flow to PDF/Excel (reuse export stack).

**Verification:** Invoice students monthly, record payments, see arrears + cash flow report.

---

## Phase 6 — HR & Payroll

**Goal:** Employee data and payroll generation (gaji pokok, tunjangan, potongan).

### Task 6.1: Employee master

**Files:**
- Create: `Employee` entity + migration `V30`
- Create: service/controller/templates

**Interfaces:**
- Produces: employee records (teachers + staff) with employment terms.

- [ ] **Step 1:** DDL `employees(id, user_id FK, teacher_id FK nullable, employee_no, name, role_type(EMPLOYEE_TEACHER/EMPLOYEE_STAFF), join_date, status(ACTIVE/RESIGNED), ...)`.

### Task 6.2: Salary components & payroll runs

**Files:**
- Create: `SalaryComponent`, `PayrollRun`, `PayrollItem` entities + migration `V31`
- Create: `PayrollService.java`

**Interfaces:**
- Produces: configurable salary components and a payroll run that computes each employee's slip.

- [ ] **Step 1:** DDL `salary_components(id, code, name, type(ALLOWANCE/DEDUCTION), amount_or_formula, ...)`, `payroll_runs(id, period, status(DRAFT/PAID), generated_at, ...)`, `payroll_items(id, payroll_run_id, employee_id, base_salary, allowances JSONB, deductions JSONB, net, ...)`.
- [ ] **Step 2:** Run payroll: for each active employee, sum base + allowances − deductions; produce net; store snapshot.
- [ ] **Step 3:** Review a draft run; approve/mark paid (tie to approval flow).

### Task 6.3: Payslip (slip gaji)

**Files:**
- Create: `PayslipPdfService.java`

**Interfaces:**
- Produces: PDF payslip per employee per period.

- [ ] **Step 1:** Extend PDF export: payslip layout (employee info, components table, net pay, signature lines).
- [ ] **Step 2:** Employee self-service view (only their own payslips) if login exists.

**Verification:** Configure components → run payroll → review → pay → print slips.

---

## Cross-cutting notes

- **Menu & permission seeding:** every school DB gets the same default roles/permissions/menus via the school Flyway set (extend `V3__insert_initial_data.sql`). School admins can then customize.
- **Roles from SIMS.md:** Admin, Guru, Siswa, Tata Usaha, Keuangan → seed these five roles per school, with `@PreAuthorize` on the module endpoints.
- **i18n:** Indonesian default labels for all new modules (`messages.properties` + `messages_id.properties`); keep English keys too.
- **Storage:** file uploads (SPMB docs, school logo) — local storage for dev; MinIO is a documented option behind the existing file service interface. Not part of this plan's critical path.
- **Async/messaging:** not needed for these phases; notifications remain in-DB via the existing module.

---

## Dependencies

```
Phase 0  (database-per-tenant foundation)
   │
   ├──► Phase 1  Master Data
   │         │
   │         ├──► Phase 2  Akademik ──► Phase 3  E-Raport
   │         │                              │
   │         └──► Phase 4  SPMB  ───────────┘ (optional CBT reuse)
   │
   ├──► Phase 5  Keuangan
   └──► Phase 6  HR & Payroll   (depends on Phase 1 employees/teachers)
```

Phase 0 must land first. Phases 5 and 6 depend on Phase 1 master data. Phases 2–3 are the educational core; Phase 4 can proceed in parallel after Phase 1. Phases are delivered in order; each phase's exit criteria gate the next.
