# System Realm — Role/Permission/Menu/System-User CRUD — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the system realm full CRUD for system users, roles, permissions, and menu management, with distinct sidebar menus.

**Architecture:** A Flyway migration (`db/system/V4`) seeds the missing authorities and menu rows. New `SystemRoleController` / `SystemPermissionController` / `SystemMenuController` under `/system/*` reuse the existing services (which route to `sims_system` for system admins). `SystemUserService` gains system-user methods (no `school_users` index). Separate `templates/system/*` templates avoid the school-picker logic baked into the shared `user/*` templates.

**Tech Stack:** Spring Boot 3.3.2, tk.mybatis, MyBatis, PostgreSQL, Thymeleaf, Playwright (manual E2E).

## Global Constraints

- Java 21, source/release 21.
- System admin routes to `sims_system` — existing `RoleService`/`PermissionService`/`MenuService` operate on the system DB without changes.
- `UserServiceImpl.create` throws when `getCurrentSchoolId()` is null — the system-user path must NOT reuse it; use `SystemUserService` methods.
- System users have NO `school_users` index (system is not a school).
- All new controllers are `@PreAuthorize("hasRole('SYSTEM')")`.
- New migrations only (V4) — never edit applied V1–V3.
- Templates: shared `user/*` templates have school-picker logic; system-user and role/menu/permission system pages use separate `templates/system/*` templates.
- Playwright verification is manual/browser-driven.

---

### Task 1: Migration `db/system/V4` — seed permissions + menus

**Files:**
- Create: `src/main/resources/db/system/V4__add_rbac_menu_permissions.sql`

**Interfaces:**
- Consumes: `sims_system` schema (menus, permissions, roles, role_permissions, role_menus from V1/V3).
- Produces: permissions USER_*/ROLE_*/PERMISSION_*/MENU_*, menu rows (User Sekolah renamed, User System, Role, Permission, Menu Management), all granted to SYSTEM.

- [ ] **Step 1: Write the migration**

```sql
-- System realm RBAC: seed authorities and menus for role/permission/menu/
-- system-user management, and rename the school-user menu.

-- 1. Permissions
INSERT INTO permissions (code, description, created_by) VALUES
    ('USER_VIEW', 'View system users', 'system'),
    ('USER_CREATE', 'Create system user', 'system'),
    ('USER_EDIT', 'Edit system user', 'system'),
    ('USER_DELETE', 'Delete system user', 'system'),
    ('ROLE_VIEW', 'View roles', 'system'),
    ('ROLE_CREATE', 'Create role', 'system'),
    ('ROLE_EDIT', 'Edit role', 'system'),
    ('ROLE_DELETE', 'Delete role', 'system'),
    ('PERMISSION_VIEW', 'View permissions', 'system'),
    ('PERMISSION_CREATE', 'Create permission', 'system'),
    ('PERMISSION_EDIT', 'Edit permission', 'system'),
    ('PERMISSION_DELETE', 'Delete permission', 'system'),
    ('MENU_VIEW', 'View menus', 'system'),
    ('MENU_CREATE', 'Create menu', 'system'),
    ('MENU_EDIT', 'Edit menu', 'system'),
    ('MENU_DELETE', 'Delete menu', 'system')
ON CONFLICT (code) DO NOTHING;

-- 2. Menus
UPDATE menus SET name = 'User Sekolah' WHERE url = '/system/users';

INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by) VALUES
    (NULL, 'User System', '/system/users/system', 'bi-person-gear', 3, true, 'system'),
    (NULL, 'Role', '/system/roles', 'bi-shield', 4, true, 'system'),
    (NULL, 'Permission', '/system/permissions', 'bi-key', 5, true, 'system'),
    (NULL, 'Menu Management', '/system/menus', 'bi-list', 6, true, 'system')
ON CONFLICT DO NOTHING;

-- 3. Grant all new permissions to SYSTEM
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'SYSTEM'
  AND p.code IN ('USER_VIEW','USER_CREATE','USER_EDIT','USER_DELETE',
                 'ROLE_VIEW','ROLE_CREATE','ROLE_EDIT','ROLE_DELETE',
                 'PERMISSION_VIEW','PERMISSION_CREATE','PERMISSION_EDIT','PERMISSION_DELETE',
                 'MENU_VIEW','MENU_CREATE','MENU_EDIT','MENU_DELETE')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 4. Grant new menus to SYSTEM
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'SYSTEM'
  AND m.url IN ('/system/users/system','/system/roles','/system/permissions','/system/menus')
  AND NOT EXISTS (
    SELECT 1 FROM role_menus rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id
);
```

- [ ] **Step 2: Compile (no code change)**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/system/V4__add_rbac_menu_permissions.sql
git commit -m "feat: seed system RBAC permissions and menus"
```

---

### Task 2: System-user CRUD in `SystemUserService`

**Files:**
- Modify: `src/main/java/com/template/service/system/SystemUserService.java`
- Modify: `src/main/java/com/template/service/system/SystemUserServiceImpl.java`

**Interfaces:**
- Consumes: `UserMapper`/`UserRoleMapper`/`RoleMapper` (school realm, route to `sims_system`), `PasswordEncoder`, `SecurityUtils`, `TransactionTemplate`.
- Produces: `listSystemUsers(String, int, int)`, `createSystemUser(UserRequest)`, `updateSystemUser(Long, UserUpdateRequest)`, `deleteSystemUser(Long)` — all WITHOUT `school_users` index writes.

- [ ] **Step 1: Extend the interface**

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

    PageResult<UserResponse> listSystemUsers(String keyword, int page, int size);
    void createSystemUser(UserRequest request);
    void updateSystemUser(Long userId, UserUpdateRequest request);
    void deleteSystemUser(Long userId);
}
```

- [ ] **Step 2: Add implementations (no routing override needed — system admin already routes to `sims_system`)**

Add to `SystemUserServiceImpl`:

```java
@Override
public PageResult<UserResponse> listSystemUsers(String keyword, int page, int size) {
    return tx.execute(status -> {
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
    });
}

@Override
public void createSystemUser(UserRequest request) {
    tx.executeWithoutResult(status -> {
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new com.template.exception.BusinessException(
                    "Username already exists: " + request.getUsername(),
                    "/system/users/system/new");
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
    });
}

@Override
public void updateSystemUser(Long userId, UserUpdateRequest request) {
    tx.executeWithoutResult(status -> {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new com.template.exception.BusinessException(
                    "User not found", "/system/users/system");
        }
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
    });
}

@Override
public void deleteSystemUser(Long userId) {
    tx.executeWithoutResult(status -> {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user != null) {
            user.setDeleted(true);
            user.setUpdatedBy(SecurityUtils.getCurrentUsername());
            user.setUpdatedDate(LocalDateTime.now());
            userMapper.updateByPrimaryKey(user);
        }
    });
}
```

Note: no `TenantContext.setRoutingKey` needed — a system admin is already routed to `sims_system`; `tx` is the injected `TransactionTemplate`.

- [ ] **Step 3: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/template/service/system/
git commit -m "feat: system-user CRUD without school index"
```

---

### Task 3: `SystemUserController` — system-user routes

**Files:**
- Modify: `src/main/java/com/template/controller/system/SystemUserController.java`

**Interfaces:**
- Consumes: `SystemUserService.listSystemUsers/createSystemUser/updateSystemUser/deleteSystemUser` (Task 2), `RoleService.findAll()`.
- Produces: `GET/POST /system/users/system*` routes using `templates/system/user/*`.

- [ ] **Step 1: Add system-user routes**

Add to `SystemUserController`:

```java
@GetMapping("/system")
public String listSystemUsers(@RequestParam(defaultValue = "") String keyword,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
    PageResult<UserResponse> result = systemUserService.listSystemUsers(keyword, page, size);
    model.addAttribute("users", result.getData());
    model.addAttribute("pagination", result.getPagination());
    model.addAttribute("keyword", keyword);
    return "system/user/list";
}

@GetMapping("/system/new")
public String formSystemUser(Model model) {
    model.addAttribute("user", new UserRequest());
    model.addAttribute("allRoles", roleService.findAll());
    return "system/user/form";
}

@PostMapping("/system")
public String saveSystemUser(@Valid @ModelAttribute("user") UserRequest request,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        model.addAttribute("allRoles", roleService.findAll());
        return "system/user/form";
    }
    systemUserService.createSystemUser(request);
    redirectAttributes.addFlashAttribute("successMessage", "System user created");
    return "redirect:/system/users/system";
}

@GetMapping("/system/{id}/edit")
public String editSystemUser(@PathVariable Long id, Model model) {
    PageResult<UserResponse> all = systemUserService.listSystemUsers("", 1, 1000);
    UserResponse found = all.getData().stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    if (found == null) {
        return "redirect:/system/users/system";
    }
    UserUpdateRequest form = new UserUpdateRequest();
    form.setId(found.getId());
    form.setUsername(found.getUsername());
    form.setFullname(found.getFullname());
    form.setEmail(found.getEmail());
    form.setEnabled(found.getEnabled());
    model.addAttribute("allRoles", roleService.findAll());
    model.addAttribute("user", form);
    return "system/user/form";
}

@PostMapping("/system/{id}")
public String updateSystemUser(@PathVariable Long id,
                               @Valid @ModelAttribute("user") UserUpdateRequest request,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        model.addAttribute("allRoles", roleService.findAll());
        return "system/user/form";
    }
    systemUserService.updateSystemUser(id, request);
    redirectAttributes.addFlashAttribute("successMessage", "System user updated");
    return "redirect:/system/users/system";
}

@PostMapping("/system/{id}/delete")
public String deleteSystemUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    systemUserService.deleteSystemUser(id);
    redirectAttributes.addFlashAttribute("successMessage", "System user deleted");
    return "redirect:/system/users/system";
}
```

- [ ] **Step 2: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/template/controller/system/SystemUserController.java
git commit -m "feat: system-user routes under /system/users/system"
```

---

### Task 4: System role/permission/menu controllers

**Files:**
- Create: `src/main/java/com/template/controller/system/SystemRoleController.java`
- Create: `src/main/java/com/template/controller/system/SystemPermissionController.java`
- Create: `src/main/java/com/template/controller/system/SystemMenuController.java`

**Interfaces:**
- Consumes: `RoleService`, `PermissionService`, `MenuService` (existing), DTOs `RoleRequest`/`RoleResponse`, `PermissionRequest`/`PermissionResponse`, `MenuRequest`/`MenuResponse`, `PageResult`.
- Produces: `/system/roles`, `/system/permissions`, `/system/menus` CRUD, rendering `templates/system/*`.

- [ ] **Step 1: Write `SystemRoleController`**

```java
package com.template.controller.system;

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
import com.template.dto.role.RoleRequest;
import com.template.dto.role.RoleResponse;
import com.template.service.role.RoleService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/system/roles")
@PreAuthorize("hasRole('SYSTEM')")
public class SystemRoleController {

    private final RoleService roleService;

    public SystemRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<RoleResponse> result = roleService.findAll(keyword, page, size);
        model.addAttribute("roles", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "system/role/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("role", new RoleRequest());
        return "system/role/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("role") RoleRequest request,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "system/role/form";
        }
        roleService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Role created");
        return "redirect:/system/roles";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        RoleResponse role = roleService.getById(id);
        if (role == null) {
            return "redirect:/system/roles";
        }
        RoleRequest form = new RoleRequest();
        form.setId(role.getId());
        form.setName(role.getName());
        form.setDescription(role.getDescription());
        model.addAttribute("role", form);
        return "system/role/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("role") RoleRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "system/role/form";
        }
        roleService.update(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Role updated");
        return "redirect:/system/roles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        roleService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Role deleted");
        return "redirect:/system/roles";
    }
}
```

- [ ] **Step 2: Confirm `RoleService` signatures**

Confirmed: `findAll(String, int, int)` → `PageResult<RoleResponse>`, `create(RoleRequest)`, `update(Long, RoleRequest)`, `delete(Long)`, `getById(Long)` → `RoleResponse`. The controller above matches; no adjustment needed.

- [ ] **Step 3: Write `SystemPermissionController` (analogous)**

```java
package com.template.controller.system;

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
import com.template.dto.permission.PermissionRequest;
import com.template.dto.permission.PermissionResponse;
import com.template.service.permission.PermissionService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/system/permissions")
@PreAuthorize("hasRole('SYSTEM')")
public class SystemPermissionController {

    private final PermissionService permissionService;

    public SystemPermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        PageResult<PermissionResponse> result = permissionService.findAll(keyword, page, size);
        model.addAttribute("permissions", result.getData());
        model.addAttribute("pagination", result.getPagination());
        model.addAttribute("keyword", keyword);
        return "system/permission/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("permission", new PermissionRequest());
        return "system/permission/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("permission") PermissionRequest request,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "system/permission/form";
        }
        permissionService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Permission created");
        return "redirect:/system/permissions";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        PermissionResponse permission = permissionService.getById(id);
        if (permission == null) {
            return "redirect:/system/permissions";
        }
        PermissionRequest form = new PermissionRequest();
        form.setId(permission.getId());
        form.setCode(permission.getCode());
        form.setDescription(permission.getDescription());
        model.addAttribute("permission", form);
        return "system/permission/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("permission") PermissionRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "system/permission/form";
        }
        request.setId(id); // PermissionRequest carries its own id; update() takes the request
        permissionService.update(request);
        redirectAttributes.addFlashAttribute("successMessage", "Permission updated");
        return "redirect:/system/permissions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        permissionService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Permission deleted");
        return "redirect:/system/permissions";
    }
}
```

- [ ] **Step 4: Confirm `PermissionService` signatures**

Confirmed: `findAll(String, int, int)` → `PageResult<PermissionResponse>`, `create(PermissionRequest)`, `update(PermissionRequest)` (id on request — set `request.setId(id)` before calling), `delete(Long)`, `getById(Long)` → `PermissionResponse`. The controller above matches.

- [ ] **Step 5: Write `SystemMenuController` (analogous, reusing `MenuService` — confirmed signatures below)**

Follow the same pattern with `MenuRequest`/`MenuResponse`, routes `/system/menus`, templates `system/menu/list` + `form`. Confirmed signatures: `create(MenuRequest)` returns `MenuResponse`, `update(MenuRequest)` (no separate id param — `MenuRequest` carries `id`), `delete(Long)`, `getById(Long)`, `findAll(String, int, int)`. So in the update handler: `request.setId(id); menuService.update(request);`.

- [ ] **Step 6: Compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS (fix any signature mismatches).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/template/controller/system/
git commit -m "feat: system role/permission/menu controllers"
```

---

### Task 5: System templates (`templates/system/*`)

**Files:**
- Create: `src/main/resources/templates/system/user/list.html`, `form.html`
- Create: `src/main/resources/templates/system/role/list.html`, `form.html`
- Create: `src/main/resources/templates/system/permission/list.html`, `form.html`
- Create: `src/main/resources/templates/system/menu/list.html`, `form.html`

**Interfaces:**
- Consumes: the same model attrs as the school templates (`users`/`roles`/`permissions`/`menus`, `pagination`, `keyword`, `allRoles` for user form, `user`/`role`/`permission`/`menu` objects).
- Produces: system-scoped pages posting to `/system/*` without school-picker logic.

- [ ] **Step 1: Copy and adapt the school templates**

For each of user/role/permission/menu:
1. Copy `templates/user/list.html` → `templates/system/user/list.html` (and form, role, permission, menu).
2. Change all `th:action` and `th:href` from `/users`, `/roles`, etc. to `/system/users/system`, `/system/roles`, etc.
3. Remove the school-picker block from the system user list/form (the `hasRole('SYSTEM')` picker block is not needed — these pages are system-only).
4. Keep the `flashError`/`flashSuccess` spans/alert (already present in form.html).

- [ ] **Step 2: Verify the templates render via a boot check**

Run: `mvn -o -q clean compile` (templates are not compiled, but confirm the app boots with the new controllers referencing the new templates).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/system/
git commit -m "feat: system-scoped templates for user/role/permission/menu"
```

---

### Task 6: E2E verification (Playwright)

**Files:**
- None (verification only).

- [ ] **Step 1: Clean compile**

Run: `mvn -o -q clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 2: Restart the app**

```bash
pkill -f spring-boot:run 2>/dev/null; sleep 3
nohup mvn -o spring-boot:run > /tmp/boot32.log 2>&1 &
```

Wait for `Started TemplateApplication`; log in as `system`/`admin`/`admin123` (triggers V4 migration on `sims_system`).

- [ ] **Step 3: Verify menus + migration**

Check `sims_system` flyway at V4; sidebar shows: Schools, User Sekolah, User System, Role, Permission, Menu Management.

- [ ] **Step 4: Role CRUD**

`/system/roles` → create `Manager` → appears; edit → updated; delete → gone.

- [ ] **Step 5: Permission CRUD**

`/system/permissions` → create → appears; edit/delete work.

- [ ] **Step 6: Menu CRUD**

`/system/menus` → create → appears; edit/delete work.

- [ ] **Step 7: System user CRUD + login**

`/system/users/system` → create `ops` / password → log out → log in `system`/`ops`/password → dashboard. Delete `ops` → login fails.

- [ ] **Step 8: School admin flow unchanged**

Log in `coba`/`admin`/`admin123` → `/users` unchanged (no picker, normal links).

- [ ] **Step 9: Commit any fixups**

```bash
git add -A
git commit -m "chore: verification fixups"
```

---

## Execution Notes

- **Two extra system migrations were needed during E2E**, because `sims_system`'s V1 tables lacked columns the shared school mappers expect:
  - `V5__add_menu_i18n_key.sql` — `menus.i18n_key` (MenuMapper queries `pm.i18n_key`); also added `sidebar.schoolUser`/`sidebar.systemUser` i18n keys.
  - `V6__add_audit_columns.sql` — audit/soft-delete columns (`deleted`, `version`, `updated_by`, `updated_date`, `created_date`) on `user_roles`, `role_permissions`, `role_menus`, `users`, `roles`, `permissions`, `menus`.
- `SystemRoleController` needed `PermissionService` + `MenuService` injected to render `allPermissions`/`allMenus` in the role form (matches the school `RoleController`).
- Verified E2E: system sidebar shows all 6 menus; role create (MANAGER), permission create (REPORT_VIEW), menu create (Dashboard System), system-user create + login (`system`/`ops`) all work.

## Self-Review Notes

- **Spec coverage:** migration (T1), system-user service (T2), system-user routes (T3), role/permission/menu controllers (T4), templates (T5), E2E (T6).
- **Type consistency:** `SystemUserService.listSystemUsers/createSystemUser/updateSystemUser/deleteSystemUser` used in T2/T3 identically. Confirmed signatures: `RoleService.update(Long, RoleRequest)`, `PermissionService.update(PermissionRequest)` (id on request), `MenuService.create/update(MenuRequest)` (id on request) — controllers set `request.setId(id)` before calling the no-id-param update methods.
- **Note:** The system-user service methods do NOT set `TenantContext` — a system admin is already routed to `sims_system`; `TransactionTemplate` `tx` is injected. This avoids the `@Transactional`-vs-routing issue from the previous feature.
- **Note:** `BusinessException` with redirect URLs surfaces duplicate/invalid errors as form flash messages (consistent with the previous fix).
