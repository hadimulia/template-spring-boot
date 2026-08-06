# Academic Module — Students / Teachers / Classes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add CRUD for teachers, students, and classes inside each school, auto-creating login accounts for teachers/students (user row + `school_users` index).

**Architecture:** A school migration (`db/migration/V21`) creates the tables (`students`, `teachers`, `classes`, `class_students`) and seeds roles/permissions/menus. `TeacherService`/`StudentService`/`ClassService` follow the `UserServiceImpl` pattern (routing DS, `@Transactional`), and reuse the school role/menu mappers. Creating a teacher/student inserts a `users` row with role `TEACHER`/`STUDENT` plus a `school_users` registry index so the account is login-able. Controllers/templates mirror `UserController`/`user/*`.

**Tech Stack:** Spring Boot 3.3.2, tk.mybatis, MyBatis, PostgreSQL, Thymeleaf, Playwright (manual E2E).

## Global Constraints

- Java 21, source/release 21.
- School module runs in the school database (routing DS). Controllers are school-admin guarded with `TEACHER_*`/`STUDENT_*`/`CLASS_*` authorities (seeded in V21).
- `users.username` is unique within a school; teacher/student username = NIP/NIS.
- `school_users` index is required for login; insert via the registry mapper (`SchoolUserMapper`).
- `@Transactional` is safe here (routing key is already set by `TenantFilter` to the school — no in-method routing change needed).
- New migration only (`V21`) — never edit applied V1–V20.
- No system realm code for this module (deferred).
- Playwright verification is manual/browser-driven.

---

### Task 1: Migration `V21` — academic schema + seed

**Files:**
- Create: `src/main/resources/db/migration/V21__academic_module.sql`

**Interfaces:**
- Consumes: existing `users`, `roles`, `permissions`, `menus`, `role_permissions`, `role_menus`, `user_roles`.
- Produces: `students`, `teachers`, `classes`, `class_students` tables; roles `TEACHER`/`STUDENT`; permissions `STUDENT_*`/`TEACHER_*`/`CLASS_*`; Akademi menus.

- [ ] **Step 1: Write the migration**

```sql
-- Academic module: teachers, students, classes. Runs in each school DB.

CREATE TABLE IF NOT EXISTS teachers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    nip VARCHAR(20) UNIQUE NOT NULL,
    fullname VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    birth_date DATE,
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(100),
    hire_date DATE,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS classes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    grade VARCHAR(20),
    academic_year VARCHAR(20),
    homeroom_teacher_id BIGINT REFERENCES teachers(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    nis VARCHAR(20) UNIQUE NOT NULL,
    fullname VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    birth_date DATE,
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(100),
    enrollment_status VARCHAR(20) DEFAULT 'ACTIVE',
    class_id BIGINT REFERENCES classes(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS class_students (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES classes(id),
    student_id BIGINT NOT NULL REFERENCES students(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0,
    UNIQUE(class_id, student_id)
);

-- Roles
INSERT INTO roles (name, description, created_by)
SELECT 'TEACHER', 'Teacher', 'system'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'TEACHER');
INSERT INTO roles (name, description, created_by)
SELECT 'STUDENT', 'Student', 'system'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'STUDENT');

-- Permissions
INSERT INTO permissions (code, description, created_by) VALUES
    ('TEACHER_VIEW', 'View teachers', 'system'),
    ('TEACHER_CREATE', 'Create teacher', 'system'),
    ('TEACHER_EDIT', 'Edit teacher', 'system'),
    ('TEACHER_DELETE', 'Delete teacher', 'system'),
    ('STUDENT_VIEW', 'View students', 'system'),
    ('STUDENT_CREATE', 'Create student', 'system'),
    ('STUDENT_EDIT', 'Edit student', 'system'),
    ('STUDENT_DELETE', 'Delete student', 'system'),
    ('CLASS_VIEW', 'View classes', 'system'),
    ('CLASS_CREATE', 'Create class', 'system'),
    ('CLASS_EDIT', 'Edit class', 'system'),
    ('CLASS_DELETE', 'Delete class', 'system')
ON CONFLICT (code) DO NOTHING;

-- Menus under a top-level 'Akademi' (parent), then child links.
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT NULL, 'Akademi', NULL, 'bi-mortarboard', 3, true, 'system'
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE name = 'Akademi');

INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT m.id, 'Guru', '/teachers', 'bi-person-workspace', 1, true, 'system'
FROM menus m WHERE m.name = 'Akademi' AND NOT EXISTS (SELECT 1 FROM menus WHERE url = '/teachers');
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT m.id, 'Murid', '/students', 'bi-people', 2, true, 'system'
FROM menus m WHERE m.name = 'Akademi' AND NOT EXISTS (SELECT 1 FROM menus WHERE url = '/students');
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT m.id, 'Kelas', '/classes', 'bi-window-stack', 3, true, 'system'
FROM menus m WHERE m.name = 'Akademi' AND NOT EXISTS (SELECT 1 FROM menus WHERE url = '/classes');

-- Grant teacher/student roles the read permission + menus so they can log in.
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'TEACHER' AND p.code IN ('TEACHER_VIEW')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'STUDENT' AND p.code IN ('STUDENT_VIEW')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Grant ADMIN all new management permissions.
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('TEACHER_VIEW','TEACHER_CREATE','TEACHER_EDIT','TEACHER_DELETE',
                 'STUDENT_VIEW','STUDENT_CREATE','STUDENT_EDIT','STUDENT_DELETE',
                 'CLASS_VIEW','CLASS_CREATE','CLASS_EDIT','CLASS_DELETE')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ADMIN sees the Akademi menus.
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN'
  AND m.url IN ('/teachers','/students','/classes')
  AND NOT EXISTS (SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);
```

- [ ] **Step 2: Compile (no code change)**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V21__academic_module.sql
git commit -m "feat: academic module schema and seed"
```

---

### Task 2 — Entities + mappers

**Files:**
- Create: `src/main/java/com/template/entity/academic/Teacher.java`
- Create: `src/main/java/com/template/entity/academic/Student.java`
- Create: `src/main/java/com/template/entity/academic/ClassEntity.java`
- Create: `src/main/java/com/template/entity/academic/ClassStudent.java`
- Create: `src/main/java/com/template/mapper/academic/TeacherMapper.java`
- Create: `src/main/java/com/template/mapper/academic/StudentMapper.java`
- Create: `src/main/java/com/template/mapper/academic/ClassMapper.java`

**Interfaces:**
- Consumes: `com.template.entity.BaseEntity` (gives id via `PostgreSqlSequenceGenId`, created/updated/deleted/version).
- Produces: entity classes + tk.mybatis `Mapper<T>` interfaces used by the services.

- [ ] **Step 1: Write `Teacher.java`**

```java
package com.template.entity.academic;

import java.time.LocalDate;

import javax.persistence.Table;

import com.template.entity.BaseEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper = true) @EqualsAndHashCode(callSuper = true)
@Table(name = "teachers")
public class Teacher extends BaseEntity {
    private Long userId;
    private String nip;
    private String fullname;
    private String gender;
    private LocalDate birthDate;
    private String address;
    private String phone;
    private String email;
    private LocalDate hireDate;
}
```

- [ ] **Step 2: Write `Student.java`** (mirror, table `students`, fields userId/nis/fullname/gender/birthDate/address/phone/email/enrollmentStatus/classId).

- [ ] **Step 3: Write `ClassEntity.java`**

```java
package com.template.entity.academic;

import javax.persistence.Table;

import com.template.entity.BaseEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
@Table(name = "classes")
public class ClassEntity extends BaseEntity {
    private String name;
    private String grade;
    private String academicYear;
    private Long homeroomTeacherId;
}
```

- [ ] **Step 4: Write `ClassStudent.java`** (table `class_students`, fields `classId`, `studentId`).

- [ ] **Step 5: Write the mappers**

```java
package com.template.mapper.academic;

import com.template.entity.academic.Teacher;
import tk.mybatis.mapper.common.Mapper;

public interface TeacherMapper extends Mapper<Teacher> {
}
```
(and `StudentMapper extends Mapper<Student>`, `ClassMapper extends Mapper<ClassEntity>`.)

- [ ] **Step 6: Register mappers with MyBatis**

Confirm the `@MapperScan` / mapper-locations picks up `com.template.mapper.academic`. Check `application.yml` `type-aliases-package` includes `com.template.entity` (it does), and `mapper-locations` is `classpath:mapper/**/*.xml` (fine — annotation mappers need no XML). Verify by compiling.

- [ ] **Step 7: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/template/entity/academic/ src/main/java/com/template/mapper/academic/
git commit -m "feat: academic entities and mappers"
```

---

### Task 3: `TeacherService` (with auto login)

**Files:**
- Create: `src/main/java/com/template/service/academic/TeacherService.java`
- Create: `src/main/java/com/template/service/academic/TeacherServiceImpl.java`

**Interfaces:**
- Consumes: `TeacherMapper`, `UserMapper`, `UserRoleMapper`, `RoleMapper`, `SchoolUserMapper`, `PasswordEncoder`, `SecurityUtils`, `UserRequest`/`UserResponse`.
- Produces: `createTeacher(TeacherRequest)`, `updateTeacher(Long, TeacherRequest)`, `deleteTeacher(Long)`, `PageResult<...> findAll(String, int, int)`.

The auto-login flow (used by create AND update) is extracted into a shared helper `createLoginAccount(...)` so both Teacher and Student services reuse it (DRY).

- [ ] **Step 1: Add a shared login-account helper**

Create `src/main/java/com/template/service/system/LoginAccountHelper.java` (a `@Component`), because both `TeacherServiceImpl` and `StudentServiceImpl` need it:

```java
package com.template.service.system;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.template.entity.registry.SchoolUser;
import com.template.entity.user.User;
import com.template.registry.mapper.SchoolUserMapper;
import com.template.util.SecurityUtils;

/** Creates the school login account + registry index for a teacher/student. */
@Component
public class LoginAccountHelper {

    private final PasswordEncoder passwordEncoder;
    private final SchoolUserMapper schoolUserMapper;

    public LoginAccountHelper(PasswordEncoder passwordEncoder, SchoolUserMapper schoolUserMapper) {
        this.passwordEncoder = passwordEncoder;
        this.schoolUserMapper = schoolUserMapper;
    }

    /** Inserts the user + index; returns the new user id. Requires routing to the school. */
    public Long createUserAndIndex(String username, String password, String fullname,
                                   String email, UserMapper userMapper) {
        if (userMapper.findByUsername(username) != null) {
            throw new com.template.exception.BusinessException(
                    "Username already exists in this school: " + username,
                    "/teachers/new");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullname(fullname);
        user.setEmail(email);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setLoginAttempts(0);
        user.setCreatedBy(SecurityUtils.getCurrentUsername());
        user.setCreatedDate(LocalDateTime.now());
        user.setDeleted(false);
        user.setVersion(0);
        userMapper.insert(user);

        SchoolUser index = new SchoolUser();
        index.setSchoolId(SecurityUtils.getCurrentSchoolId());
        index.setUserId(user.getId());
        index.setUsername(username);
        index.setEnabled(true);
        index.setCreatedBy(SecurityUtils.getCurrentUsername());
        index.setDeleted(false);
        schoolUserMapper.insertSelective(index);

        return user.getId();
    }
}
```

Note: `UserMapper` is passed as a parameter to avoid a circular dependency (it's a school-realm mapper). If `SecurityUtils.getCurrentSchoolId()` returns null (system realm), this flow is only used by school admins, so it will have a school context.

- [ ] **Step 2: Write `TeacherService` interface**

```java
package com.template.service.academic;

import com.template.dto.PageResult;
import com.template.dto.academic.TeacherRequest;
import com.template.dto.academic.TeacherResponse;

public interface TeacherService {
    PageResult<TeacherResponse> findAll(String keyword, int page, int size);
    void create(TeacherRequest request);
    void update(Long id, TeacherRequest request);
    void delete(Long id);
    TeacherResponse getById(Long id);
}
```

- [ ] **Step 3: Write the DTOs**

`TeacherRequest`: `id, nip (@NotBlank), fullname (@NotBlank), gender, birthDate, address, phone, email, hireDate, password (when creating account)`.

`TeacherResponse`: `id, userId, nip, fullname, gender, birthDate, address, phone, email, hireDate, username, roles`.

- [ ] **Step 4: Write `TeacherServiceImpl`**

```java
package com.template.service.academic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.dto.PageResult;
import com.template.dto.academic.TeacherRequest;
import com.template.dto.academic.TeacherResponse;
import com.template.entity.academic.Teacher;
import com.template.entity.user.User;
import com.template.entity.user.UserRole;
import com.template.mapper.academic.TeacherMapper;
import com.template.mapper.role.RoleMapper;
import com.template.mapper.user.UserMapper;
import com.template.mapper.user.UserRoleMapper;
import com.template.service.generic.GenericServiceImpl;
import com.template.service.system.LoginAccountHelper;
import com.template.util.SecurityUtils;

@Service
@Transactional
public class TeacherServiceImpl extends GenericServiceImpl<Teacher, Long> implements TeacherService {

    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final LoginAccountHelper loginAccountHelper;

    public TeacherServiceImpl(TeacherMapper teacherMapper, UserMapper userMapper,
                              UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                              LoginAccountHelper loginAccountHelper) {
        super(teacherMapper);
        this.teacherMapper = teacherMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.loginAccountHelper = loginAccountHelper;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResult<TeacherResponse> findAll(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<Teacher> teachers = teacherMapper.selectAll(); // refine with keyword/paging via Condition
        List<TeacherResponse> data = teachers.stream()
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .map(this::toResponse)
                .collect(Collectors.toList());
        // NOTE: implement real paging/count via a Condition or a custom mapper query.
        return PageResult.of(data, data.size(), page, size);
    }

    @Override
    public void create(TeacherRequest request) {
        // Create login account (routing DS is the school).
        Long userId = loginAccountHelper.createUserAndIndex(
                request.getNip(), request.getPassword(), request.getFullname(),
                request.getEmail(), userMapper);

        // Assign TEACHER role. RoleMapper has no findByName; use tk Condition.
        Role teacherRole = roleMapper.selectOneByExample(new Condition(Role.class, "name=TEACHER"));
        if (teacherRole != null) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(teacherRole.getId());
            ur.setCreatedBy(SecurityUtils.getCurrentUsername());
            ur.setCreatedDate(LocalDateTime.now());
            userRoleMapper.insert(ur);
        }

        Teacher teacher = new Teacher();
        teacher.setUserId(userId);
        teacher.setNip(request.getNip());
        teacher.setFullname(request.getFullname());
        teacher.setGender(request.getGender());
        teacher.setBirthDate(request.getBirthDate());
        teacher.setAddress(request.getAddress());
        teacher.setPhone(request.getPhone());
        teacher.setEmail(request.getEmail());
        teacher.setHireDate(request.getHireDate());
        teacherMapper.insert(teacher);
    }

    @Override
    public void update(Long id, TeacherRequest request) { /* similar: update profile fields; optionally update user fullname/email */ }
    @Override
    public void delete(Long id) { /* soft-delete profile + its user + registry index */ }
    @Override
    public TeacherResponse getById(Long id) { return toResponse(teacherMapper.selectByPrimaryKey(id)); }

    private TeacherResponse toResponse(Teacher t) {
        User u = userMapper.selectByPrimaryKey(t.getUserId());
        return TeacherResponse.builder()
                .id(t.getId()).userId(t.getUserId()).nip(t.getNip()).fullname(t.getFullname())
        .gender(t.getGender()).birthDate(t.getBirthDate()).address(t.getAddress())
        .phone(t.getPhone()).email(t.getEmail()).hireDate(t.getHireDate())
        .username(u != null ? u.getUsername() : null).build();
    }
}
```

**Important refinements for the implementer** (already verified):
- `RoleMapper` has no `findByName` — use tk `selectOneByExample(new Condition(Role.class, "name=TEACHER"))` to fetch the role by name.
- `SecurityUtils.getCurrentUsername()` (not `getCreatedBy`) — confirmed present.
- Implement real paging in `findAll` with a tk `Condition` filtering `deleted = false`, or a custom mapper query; the snippet above is illustrative.

- [ ] **Step 5: Compile**

Run: `mvn -o -q clean compile`. Fix any signature mismatches against `RoleMapper`/`UserMapper` (record the actual method names used).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/template/service/academic/ src/main/java/com/template/service/system/LoginAccountHelper.java src/main/java/com/template/dto/academic/
git commit -m "feat: TeacherService with auto login account"
```

---

### Task 4: `StudentService` + `ClassService`

**Files:**
- Create: `src/main/java/com/template/service/academic/StudentService.java` + `StudentServiceImpl.java`
- Create: `src/main/java/com/template/service/academic/ClassService.java` + `ClassServiceImpl.java`
- Create: `src/main/java/com/template/dto/academic/StudentRequest`/`StudentResponse`, `ClassRequest`/`ClassResponse`

**Interfaces:**
- Consumes: `LoginAccountHelper`, `StudentMapper`, `ClassMapper`, `TeacherMapper`, security utils.
- Produces: same CRUD for students (username = NIS, role `STUDENT`) and classes (homeroom teacher + optional class-student links).

- [ ] **Step 1: Write `StudentServiceImpl`** mirroring `TeacherServiceImpl`, with username = `request.getNis()` and role `STUDENT`.

- [ ] **Step 2: Write `ClassServiceImpl`**: CRUD for `ClassEntity`; `createClass` sets `homeroomTeacherId`. Class-student assignment is via a `ClassStudentMapper` (`extends Mapper<ClassStudent>`): `addStudent(classId, studentId)` inserts a link; `removeStudent(classId, studentId)` soft-deletes it. The class form lists/checks students.

- [ ] **Step 3: Assemble `ClassStudentMapper` (`extends Mapper<ClassStudent>`) and wire class-student links into `ClassServiceImpl`: `addStudent(Long classId, Long studentId)`, `removeStudent(Long classId, Long studentId)`, `listStudents(Long classId)`.

- [ ] **Step 4: Compile**

Run: `mvn -o -q clean compile`

- [ ] **Step 5: Commit** (`git add ...`).

---

### Task 5: Controllers + templates

**Files:**
- Create: `TeacherController`, `StudentController`, `ClassController` (`/teachers`, `/students`, `/classes`).
- Create: templates `teacher/list.html`, `form.html`; `student/list.html`, `form.html`; `class/list.html`, `form.html` (modeled on `user/*`).
- Modify: i18n `messages.properties` + `messages_id.properties` with new labels (`teacher.*`, `student.*`, `class.*`, `academic.*`).

**Interfaces:**
- Consumes: `TeacherService`/`StudentService`/`ClassService`, `RoleService` etc.
- Produces: school-admin routes guarded by the seeded authorities.

- [ ] **Step 1: Write `TeacherController`** (mirror `UserController`):

```java
package com.template.controller.academic;

@Controller
@RequestMapping("/teachers")
@RequiredArgsConstructor
public class TeacherController {
    private final TeacherService teacherService;

    @GetMapping @PreAuthorize("hasAuthority('TEACHER_VIEW')")
    public String list(@RequestParam(defaultValue="") String keyword,
                       @RequestParam(defaultValue="1") int page,
                       @RequestParam(defaultValue="10") int size, Model model) { ... }

    @GetMapping("/new") @PreAuthorize("hasAuthority('TEACHER_CREATE')") ...
    @PostMapping @PreAuthorize("hasAuthority('TEACHER_CREATE')") ...
    @GetMapping("/{id}/edit") @PreAuthorize("hasAuthority('TEACHER_EDIT')") ...
    @PostMapping("/{id}") @PreAuthorize("hasAuthority('TEACHER_EDIT')") ...
    @PostMapping("/{id}/delete") @PreAuthorize("hasAuthority('TEACHER_DELETE')") ...
}
```

- [ ] **Step 2: Write `StudentController` and `ClassController`** analogously.

- [ ] **Step 3: Write templates** (teacher list/form, student list/form, class list/form) copying `user/list.html`/`form.html` structure, replacing labels with the new i18n keys.

- [ ] **Step 4: Add i18n keys** (`teacher.management`, `teacher.add`, `student.management`, `student.add`, `class.management`, `class.add`, plus field labels) in both `messages.properties` and `messages_id.properties`.

- [ ] **Step 5: Compile + commit**.

---

### Task 6: E2E verification (Playwright)

**Files:**
- None (verification only).

- [ ] **Step 1: Clean compile** — `mvn -o -q clean compile`.
- [ ] **Step 2: Restart** — `nohup mvn -o spring-boot:run > /tmp/boot35.log 2>&1 &`; wait for startup; log in `coba`/`admin`/`admin123` (triggers V21 on `sims_coba`).
- [ ] **Step 3: Menus** — sidebar shows `Akademi > Guru / Murid / Kelas`.
- [ ] **Step 4: Teacher create + login** — create teacher `Budi` with NIP `12345`; verify a `users` row (TEACHER) + `school_users` index; log out, log in `coba`/`12345`/default password → dashboard.
- [ ] **Step 5: Student create** — create student `Ani` NIS `54321`; login works (STUDENT role).
- [ ] **Step 6: Class create + membership** — create class with homeroom teacher; add a student.
- [ ] **Step 7: Edit/delete** — delete teacher; login fails; index soft-deleted.
- [ ] **Step 8: Duplicate** — create another with the same NIP/NIS → form error (not JSON).
- [ ] **Step 9: Commit fixups** — `git add -A && git commit -m "chore: verification fixups"`.

---

## Self-Review Notes

- **Spec coverage:** schema V21 (T1), entities/mappers (T2), teacher+login (T3), student/class (T4), controllers/templates (T5), E2E (T6).
- **Type consistency:** `LoginAccountHelper.createUserAndIndex(username, password, fullname, email, userMapper)` used in both T3 and T4. `TeacherMapper`/`StudentMapper`/`ClassMapper` extend tk `Mapper<T>`.
- **Verified during planning:** `RoleMapper` has no `findByName` — use tk `Condition(name=...)`; `SecurityUtils.getCurrentUsername()`/`getCurrentSchoolId()` exist; `UserRole extends BaseEntity`; class-student linking is in scope (T4).
- **Note:** `users.username` per-school uniqueness; full `UserMapper` method names validated at compile time.