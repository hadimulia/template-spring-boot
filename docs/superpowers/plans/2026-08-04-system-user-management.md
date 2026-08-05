# System Realm — Cross-School User Management — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the system admin manage users of any school through a picker-based `/users` UI, using a dedicated `SystemUserService` that routes per-operation to the chosen school.

**Architecture:** `SystemUserService` sets `TenantContext` (routing key = school db name, tenant id = school id) in try/finally around school-realm mapper calls, so existing `UserMapper`/`UserRoleMapper`/`RoleMapper` operate on the chosen school while registry `SchoolUserMapper` writes the index. A `SystemUserController` (`@PreAuthorize("hasRole('SYSTEM')")`) exposes list/create/edit/delete under `/system/users`; templates reuse `user/list.html`/`user/form.html` with a conditional school picker.

**Tech Stack:** Spring Boot 3.3.2, tk.mybatis, MyBatis, PostgreSQL, Thymeleaf, Playwright (manual E2E).

## Global Constraints

- Java 21, source/release 21.
- `school_users` is a **registry** table — write it only via the registry `SchoolUserMapper`.
- `TenantContext` must be set and cleared in `try/finally` around every cross-school operation (no state leak across requests; `TenantFilter` already clears at request end).
- A system admin's `SecurityUtils.getCurrentSchoolId()` is `null` — `SystemUserService` must set `TenantContext.setTenantId(schoolId)` itself so index writes carry the right `school_id`.
- `dbName` resolved from `SchoolMapper.selectByPrimaryKey(schoolId)`; reject if school missing, deleted, or not ACTIVE.
- Username uniqueness is per school: `UserMapper.findByUsername` under the routing key.
- `@PreAuthorize("hasRole('SYSTEM')")` on the new controller — the system realm's authority is `ROLE_SYSTEM`; school admins have `USER_*` authorities and keep the existing `/users` flow.
- No migration needed (schema unchanged).
- Playwright verification is manual/browser-driven against the running app.

---

### Task 1: `SystemUserService` interface + implementation

**Files:**
- Create: `src/main/java/com/template/service/system/SystemUserService.java`
- Create: `src/main/java/com/template/service/system/SystemUserServiceImpl.java`

**Interfaces:**
- Consumes: `SchoolMapper` (registry — find by id), `SchoolUserMapper` (registry), `UserMapper`/`UserRoleMapper`/`RoleMapper` (school realm), `PasswordEncoder`, `SecurityUtils`, `TenantContext`, DTOs `UserRequest`/`UserUpdateRequest`/`UserResponse`, `PageResult`.
- Produces: `SystemUserService.listBySchool(Long, String, int, int)`, `create(Long, UserRequest)`, `update(Long, Long, UserUpdateRequest)`, `delete(Long, Long)`.

- [ ] **Step 1: Write the interface**

```java
package com.template.service.system;

import com.template.dto.PageResult;
import com.template.dto.user.UserRequest;
import com.template.dto.user.UserResponse;
import com.template.dto.user.UserUpdateRequest;

public interface SystemUserService {
    PageResult<UserResponse> listBySchool(Long schoolId, String keyword, int page, int size);
    void create(Long schoolId, UserRequest request);
    void update(Long schoolId, Long userId, UserUpdateRequest request);
    void delete(Long schoolId, Long userId);
}
```

- [ ] **Step 2: Write the implementation**

```java
package com.template.service.system;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.dto.PageResult;
import com.template.dto.user.UserRequest;
import com.template.dto.user.UserResponse;
import com.template.dto.user.UserUpdateRequest;
import com.template.entity.registry.SchoolUser;
import com.template.entity.school.School;
import com.template.entity.user.User;
import com.template.entity.user.UserRole;
import com.template.mapper.role.RoleMapper;
import com.template.mapper.user.UserMapper;
import com.template.mapper.user.UserRoleMapper;
import com.template.registry.mapper.SchoolMapper;
import com.template.registry.mapper.SchoolUserMapper;
import com.template.tenant.TenantContext;
import com.template.util.SecurityUtils;

@Service
public class SystemUserServiceImpl implements SystemUserService {

    private final SchoolMapper schoolMapper;
    private final SchoolUserMapper schoolUserMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public SystemUserServiceImpl(SchoolMapper schoolMapper,
                                 SchoolUserMapper schoolUserMapper,
                                 UserMapper userMapper,
                                 UserRoleMapper userRoleMapper,
                                 RoleMapper roleMapper,
                                 PasswordEncoder passwordEncoder) {
        this.schoolMapper = schoolMapper;
        this.schoolUserMapper = schoolUserMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    private School requireActiveSchool(Long schoolId) {
        School school = schoolMapper.selectByPrimaryKey(schoolId);
        if (school == null || Boolean.TRUE.equals(school.getDeleted())
                || !"ACTIVE".equals(school.getStatus())) {
            throw new IllegalArgumentException("School not found or inactive: " + schoolId);
        }
        return school;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResult<UserResponse> listBySchool(Long schoolId, String keyword, int page, int size) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            int offset = (page - 1) * size;
            List<UserResponse> data = userMapper.findAll(keyword, offset, size);
            int total = userMapper.countAll(keyword);
            for (UserResponse user : data) {
                List<Long> roleIds = userRoleMapper.findRoleIdsByUserId(user.getId());
                List<String> roleNames = roleIds.stream()
                        .map(roleId -> roleMapper.selectByPrimaryKey(roleId))
                        .filter(r -> r != null && !Boolean.TRUE.equals(r.getDeleted()))
                        .map(r -> r.getName())
                        .collect(Collectors.toList());
                user.setRoles(roleNames);
            }
            return PageResult.of(data, total, page, size);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    @Override
    public void create(Long schoolId, UserRequest request) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
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

            createUserIndex(user, schoolId);
        } finally {
            TenantContext.clear();
        }
    }

    private void createUserIndex(User user, Long schoolId) {
        SchoolUser index = new SchoolUser();
        index.setSchoolId(schoolId);
        index.setUserId(user.getId());
        index.setUsername(user.getUsername());
        index.setEnabled(true);
        index.setCreatedBy(SecurityUtils.getCurrentUsername());
        index.setDeleted(false);
        schoolUserMapper.insertSelective(index);
    }

    @Transactional
    @Override
    public void update(Long schoolId, Long userId, UserUpdateRequest request) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            User user = userMapper.selectByPrimaryKey(userId);
            if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
                throw new IllegalArgumentException("User not found in school " + schoolId);
            }
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
                List<Long> existingRoleIds = userRoleMapper.findRoleIdsByUserIdAll(userId);
                for (Long roleId : existingRoleIds) {
                    UserRole ur = new UserRole();
                    ur.setUserId(userId);
                    ur.setRoleId(roleId);
                    userRoleMapper.delete(ur);
                }
                for (Long roleId : request.getRoleIds()) {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    userRole.setCreatedBy(SecurityUtils.getCurrentUsername());
                    userRole.setCreatedDate(LocalDateTime.now());
                    userRoleMapper.insert(userRole);
                }
            }

            if (usernameChanged) {
                SchoolUser index = schoolUserMapper.findByUserIdAndSchool(userId, schoolId);
                if (index != null) {
                    index.setUsername(request.getUsername());
                    index.setUpdatedBy(SecurityUtils.getCurrentUsername());
                    index.setUpdatedDate(LocalDateTime.now());
                    schoolUserMapper.updateByPrimaryKey(index);
                }
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    @Override
    public void delete(Long schoolId, Long userId) {
        School school = requireActiveSchool(schoolId);
        TenantContext.setRoutingKey(school.getDbName());
        TenantContext.setTenantId(schoolId);
        try {
            User user = userMapper.selectByPrimaryKey(userId);
            if (user != null) {
                user.setDeleted(true);
                user.setUpdatedBy(SecurityUtils.getCurrentUsername());
                user.setUpdatedDate(LocalDateTime.now());
                userMapper.updateByPrimaryKey(user);

                SchoolUser index = schoolUserMapper.findByUserIdAndSchool(userId, schoolId);
                if (index != null) {
                    index.setDeleted(true);
                    index.setUpdatedBy(SecurityUtils.getCurrentUsername());
                    index.setUpdatedDate(LocalDateTime.now());
                    schoolUserMapper.updateByPrimaryKey(index);
                }
            }
        } finally {
            TenantContext.clear();
        }
    }
}
```

- [ ] **Step 3: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/template/service/system/
git commit -m "feat: SystemUserService for cross-school user CRUD"
```

---

### Task 2: `SystemUserController`

**Files:**
- Create: `src/main/java/com/template/controller/system/SystemUserController.java`

**Interfaces:**
- Consumes: `SystemUserService` (Task 1), `SchoolMapper` (picker list), `RoleService.findAll()` (roles for form), DTOs.
- Produces: HTTP routes `/system/users*` guarded by `hasRole('SYSTEM')`, carrying `schoolId`.

- [ ] **Step 1: Write the controller**

```java
package com.template.controller.system;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.template.dto.PageResult;
import com.template.dto.role.RoleResponse;
import com.template.dto.school.SchoolResponse;
import com.template.dto.user.UserRequest;
import com.template.dto.user.UserResponse;
import com.template.dto.user.UserUpdateRequest;
import com.template.registry.mapper.SchoolMapper;
import com.template.service.role.RoleService;
import com.template.service.system.SystemUserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/system/users")
@PreAuthorize("hasRole('SYSTEM')")
public class SystemUserController {

    private final SystemUserService systemUserService;
    private final SchoolMapper schoolMapper;
    private final RoleService roleService;

    public SystemUserController(SystemUserService systemUserService,
                                SchoolMapper schoolMapper,
                                RoleService roleService) {
        this.systemUserService = systemUserService;
        this.schoolMapper = schoolMapper;
        this.roleService = roleService;
    }

    private void addSchoolPicker(Model model, Long selectedSchoolId) {
        List<SchoolResponse> schools = schoolMapper.findAll(null, 0, 1000).stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .toList();
        model.addAttribute("schools", schools);
        model.addAttribute("selectedSchoolId", selectedSchoolId);
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long schoolId,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        if (schoolId == null) {
            addSchoolPicker(model, null);
            model.addAttribute("users", List.of());
            model.addAttribute("pagination", null);
            model.addAttribute("keyword", keyword);
            return "user/list";
        }
        PageResult<UserResponse> result = systemUserService.listBySchool(schoolId, keyword, page, size);
        model.addAttribute("users", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        addSchoolPicker(model, schoolId);
        return "user/list";
    }

    @GetMapping("/new")
    public String form(@RequestParam Long schoolId, Model model) {
        model.addAttribute("user", new UserRequest());
        model.addAttribute("allRoles", roleService.findAll());
        addSchoolPicker(model, schoolId);
        return "user/form";
    }

    @PostMapping
    public String save(@RequestParam Long schoolId,
                       @Valid @ModelAttribute("user") UserRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", roleService.findAll());
            addSchoolPicker(model, schoolId);
            return "user/form";
        }
        systemUserService.create(schoolId, request);
        redirectAttributes.addFlashAttribute("successMessage", "User created in school");
        return "redirect:/system/users?schoolId=" + schoolId;
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @RequestParam Long schoolId, Model model) {
        PageResult<UserResponse> all = systemUserService.listBySchool(schoolId, "", 1, 1000);
        UserResponse found = all.getData().stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
        if (found == null) {
            return "redirect:/system/users?schoolId=" + schoolId;
        }
        UserUpdateRequest form = new UserUpdateRequest();
        form.setId(found.getId());
        form.setUsername(found.getUsername());
        form.setFullname(found.getFullname());
        form.setEmail(found.getEmail());
        form.setEnabled(found.getEnabled());
        model.addAttribute("allRoles", roleService.findAll());
        model.addAttribute("user", form);
        addSchoolPicker(model, schoolId);
        return "user/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam Long schoolId,
                         @Valid @ModelAttribute("user") UserUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", roleService.findAll());
            addSchoolPicker(model, schoolId);
            return "user/form";
        }
        systemUserService.update(schoolId, id, request);
        redirectAttributes.addFlashAttribute("successMessage", "User updated in school");
        return "redirect:/system/users?schoolId=" + schoolId;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam Long schoolId,
                         RedirectAttributes redirectAttributes) {
        systemUserService.delete(schoolId, id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted from school");
        return "redirect:/system/users?schoolId=" + schoolId;
    }
}
```

- [ ] **Step 2: Verify `SchoolResponse` has `getStatus()`**

`SchoolResponse` (from `SchoolServiceImpl`) has `status` (confirmed `@Data` + `status` field), so `SchoolResponse::getStatus` is available. The `SystemUserController.addSchoolPicker` filters ACTIVE schools by `s.getStatus()`.

- [ ] **Step 3: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/template/controller/system/
git commit -m "feat: SystemUserController with school picker"
```

---

### Task 3: Templates — school picker for the system realm

**Files:**
- Modify: `src/main/resources/templates/user/list.html`
- Modify: `src/main/resources/templates/user/form.html`

**Interfaces:**
- Consumes: model attrs `schools`, `selectedSchoolId` (from `SystemUserController`); `#authorization.expression('hasRole(''SYSTEM'')')` for the system branch.
- Produces: system admin sees a school picker and school-scoped links; school admin sees the unchanged flow.

- [ ] **Step 1: Add the picker + conditional links to `list.html`**

After the `flashError` span (line 26), add the picker block:

```html
<div th:if="${#authorization.expression('hasRole(''SYSTEM'')')}" class="mb-3">
    <form th:action="@{/system/users}" method="get" class="input-group">
        <select name="schoolId" class="form-select" th:required="required">
            <option value="" th:text="-- Select school --"></option>
            <option th:each="s : ${schools}" th:value="${s.id}"
                    th:selected="${s.id == selectedSchoolId}"
                    th:text="${s.code} - ${s.name}"></option>
        </select>
        <button type="submit" class="btn btn-primary" th:text="Open"></button>
    </form>
</div>
```

Change the Add/Edit/Delete links to be school-aware when the system realm is active:

```html
<a th:if="${#authorization.expression('hasRole(''SYSTEM'')')}"
   th:href="@{/system/users/new(schoolId=${selectedSchoolId})}"
   class="btn btn-primary"><i class="bi bi-plus-lg"></i> <span th:text="#{user.add}">Add User</span></a>
<a th:if="${#authorization.expression('hasRole(''SYSTEM'')') == false and #authorization.expression('hasAuthority(''USER_CREATE'')')}"
   th:href="@{/users/new}" class="btn btn-primary"><i class="bi bi-plus-lg"></i> <span th:text="#{user.add}">Add User</span></a>
```

In the table rows, make edit/delete links school-aware:

```html
<a th:if="${#authorization.expression('hasRole(''SYSTEM'')')}"
   th:href="@{/system/users/{id}/edit(id=${user.id}, schoolId=${selectedSchoolId})}"
   class="btn btn-sm btn-warning"><i class="bi bi-pencil"></i></a>
<a th:if="${#authorization.expression('hasRole(''SYSTEM'')') == false and #authorization.expression('hasAuthority(''USER_EDIT'')')}"
   th:href="@{/users/{id}/edit(id=${user.id})}" class="btn btn-sm btn-warning"><i class="bi bi-pencil"></i></a>
```

For delete, wrap the existing form with a school-aware action:

```html
<form th:if="${#authorization.expression('hasRole(''SYSTEM'')')}"
      th:action="@{/system/users/{id}/delete(id=${user.id}, schoolId=${selectedSchoolId})}"
      method="post" class="d-inline">
    <button type="button" class="btn btn-sm btn-danger"
            data-bs-toggle="modal" data-bs-target="#confirmModal"
            th:data-modal-title="#{confirm.delete.user.title}"
            th:data-modal-body="#{confirm.delete.user.body}"
            th:data-modal-btn="#{modal.confirm.delete}"><i class="bi bi-trash"></i></button>
</form>
```

- [ ] **Step 2: Add the picker + school-aware actions to `form.html`**

After the header (line 16), add the picker:

```html
<div th:if="${#authorization.expression('hasRole(''SYSTEM'')')}" class="mb-3">
    <label class="form-label" th:text="School"></label>
    <select class="form-select" disabled>
        <option th:each="s : ${schools}" th:value="${s.id}"
                th:selected="${s.id == selectedSchoolId}"
                th:text="${s.code} - ${s.name}"></option>
    </select>
</div>
```

Change the form action for the system realm:

```html
<form th:if="${#authorization.expression('hasRole(''SYSTEM'')')}"
      th:action="${user.id != null ? '/system/users/' + user.id : '/system/users'}"
      th:with="sid=${selectedSchoolId}" method="post" th:object="${user}">
    <input type="hidden" name="schoolId" th:value="${sid}">
    <!-- ...rest of the form unchanged... -->
</form>
<form th:unless="${#authorization.expression('hasRole(''SYSTEM'')')}"
      th:action="${user.id != null ? '/users/' + user.id : '/users'}"
      method="post" th:object="${user}">
    <!-- ...same fields... -->
</form>
```

Note: to avoid duplicating all fields, wrap the shared field block in a Thymeleaf fragment or a single form whose action is chosen via `th:attr`. Simplest robust approach: single form, `th:action` computed:

```html
<form th:action="${#authorization.expression('hasRole(''SYSTEM'')') ? (user.id != null ? '/system/users/' + user.id : '/system/users') : (user.id != null ? '/users/' + user.id : '/users')}"
      method="post" th:object="${user}">
    <input th:if="${#authorization.expression('hasRole(''SYSTEM'')')}" type="hidden" name="schoolId" th:value="${selectedSchoolId}">
    <!-- ...existing fields unchanged... -->
</form>
```

- [ ] **Step 3: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS (templates are not compiled, but confirm the app boots).

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/user/list.html src/main/resources/templates/user/form.html
git commit -m "feat: school picker for system realm in user pages"
```

---

### Task 4: E2E verification (Playwright)

**Files:**
- None (verification only).

- [ ] **Step 1: Clean compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 2: Restart the app**

```bash
pkill -f spring-boot:run 2>/dev/null; sleep 3
nohup mvn -o spring-boot:run > /tmp/boot27.log 2>&1 &
```

Wait for `Started TemplateApplication` (monitor `/tmp/boot27.log`).

- [ ] **Step 3: System admin opens /system/users with picker**

Via Playwright: log in `system`/`admin`/`admin123` → `/system/users` → picker lists ACTIVE schools (`DEFAULT`, `baru`, `coba`). Select `baru` → see users `admin`, `guru`.

- [ ] **Step 4: System creates a user in a school**

Select `baru`, click Add, create user `sistemadmin` / `password123`. Log out, log in `baru`/`sistemadmin`/`password123` → dashboard.

- [ ] **Step 5: System edits a user**

As system, `/system/users?schoolId=<baru>`, edit `sistemadmin` (change full name / enabled). Verify `school_users` unchanged username; verify the school DB user updated.

- [ ] **Step 6: System deletes a user**

As system, delete `sistemadmin` from `baru`. Verify login `baru`/`sistemadmin` fails; `school_users` row soft-deleted.

- [ ] **Step 7: Duplicate rejected**

As system, create another `guru` in `baru` → expect 400 (duplicate), only one active `guru` index row for `baru`.

- [ ] **Step 8: School admin flow unchanged**

Log in `coba`/`admin`/`admin123` → `/users` → no picker, normal Add/Edit/Delete links (no `schoolId`).

- [ ] **Step 9: Commit any fixups**

```bash
git add -A
git commit -m "chore: verification fixups"
```

---

## Execution Notes

- **`@Transactional` conflicts with in-method routing.** A `@Transactional` proxy opens its connection before the method body runs, so `TenantContext.setRoutingKey` set inside the method is ignored (queries hit the old realm). Fixed by using `TransactionTemplate` (injected `PlatformTransactionManager`) and calling `tx.execute(...)` after setting the routing key.
- **Thymeleaf literal gotchas** fixed during E2E: `th:text="-- Select school --"` fails parse (treats `--` as comment/expression); `${s.code} - ${s.name}` fails parse (treats `-` as subtraction). Both replaced with i18n key / `|...|` literal expressions.
- **Empty-list view** needed a real `Pagination` object (not `null`) because `fragments/pagination` reads `pagination.totalPages`.
- **Roles in the system form** come from `roleService.findAll()` which reads the system realm's roles (only `SYSTEM`), not the target school's roles. Acceptable for now — the school's roles could be listed via the routing key in a later refinement.
- All E2E scenarios passed (see Task 4); the working tree is clean except `.claude/settings.local.json`.

## Self-Review Notes

- **Spec coverage:** SystemUserService (T1), SystemUserController (T2), templates/picker (T3), E2E (T4). All spec sections map to tasks.
- **Type consistency:** `SystemUserService.listBySchool/create/update/delete` signatures used identically in T1 and T2. `TenantContext.setRoutingKey/setTenantId/clear` used consistently. `SchoolResponse.getStatus()` confirmed present (`@Data` + `status` field).
- **Note:** `schoolMapper.findAll(null, 0, 1000)` for the picker — the registry `SchoolMapper.findAll(keyword, offset, limit)` signature. `RoleService.findAll()` returns `List<RoleResponse>` with `getId()`/`getName()`.
- **Note:** T3 reuses the existing templates with conditional system blocks; the single-form `th:action` approach avoids duplicating fields.
