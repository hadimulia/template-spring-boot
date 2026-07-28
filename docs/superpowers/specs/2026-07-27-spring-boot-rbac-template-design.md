# Spring Boot RBAC Template - Design Specification

**Date:** 2026-07-27  
**Version:** 1.0  
**Scope:** Phase 1 (Foundation) + Phase 2 (RBAC)

## Overview

This document specifies the design for a reusable Spring Boot web application template with complete authentication and Role-Based Access Control (RBAC). The template serves as a foundation for enterprise internal applications such as ERP, CRM, HRIS, and administration systems.

## Goals

1. Provide production-ready authentication and authorization system
2. Implement complete RBAC with User, Role, Permission, and Menu management
3. Support dynamic menu generation based on user roles
4. Follow clean architecture with clear layer separation
5. Use MyBatis for flexible database access
6. Create reusable patterns for future module development

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Java | Java | 21 |
| Framework | Spring Boot | 3.x |
| Build Tool | Maven | - |
| View Engine | Thymeleaf | - |
| CSS Framework | Bootstrap | 5 |
| JavaScript | Vanilla JS | - |
| Database | PostgreSQL | - |
| ORM | MyBatis + tk.mybatis | - |
| Security | Spring Security | - |
| Validation | Jakarta Validation | - |
| Migration | Flyway | - |
| Logging | Logback | (default) |
| Password Hashing | BCrypt | - |

## Architecture

### Layer Structure

```
Browser
    ↓
Controller (Presentation Layer)
    ↓
Service (Business Logic Layer)
    ↓
Mapper (Data Access Layer)
    ↓
Database
```

**Responsibilities:**

- **Controller**: Handle HTTP requests, validation, return views
- **Service**: Business logic, transactions, orchestration
- **Mapper**: Database queries, CRUD operations
- **Entity**: Domain models
- **DTO**: Data transfer objects for API

**Key Rules:**
- Controllers MUST NOT access Mappers directly
- Services own transaction boundaries
- Each layer communicates only with adjacent layers

### Project Structure

```
src/main/java/com/template/
├── TemplateApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── MyBatisConfig.java
│   ├── WebMvcConfig.java
│   └── ThymeleafConfig.java
├── security/
│   ├── CustomUserDetailsService.java
│   ├── CustomAuthenticationSuccessHandler.java
│   ├── CustomAuthenticationFailureHandler.java
│   └── PasswordEncoderConfig.java
├── controller/
│   ├── AuthController.java
│   ├── DashboardController.java
│   ├── UserController.java
│   ├── RoleController.java
│   ├── PermissionController.java
│   └── MenuController.java
├── service/
│   ├── UserService.java
│   ├── RoleService.java
│   ├── PermissionService.java
│   └── MenuService.java
├── mapper/
│   ├── UserMapper.java
│   ├── RoleMapper.java
│   ├── PermissionMapper.java
│   ├── MenuMapper.java
│   ├── UserRoleMapper.java
│   ├── RolePermissionMapper.java
│   └── RoleMenuMapper.java
├── entity/
│   ├── BaseEntity.java
│   ├── User.java
│   ├── Role.java
│   ├── Permission.java
│   ├── Menu.java
│   ├── UserRole.java
│   ├── RolePermission.java
│   └── RoleMenu.java
├── dto/
│   ├── UserRequest.java
│   ├── UserResponse.java
│   ├── RoleRequest.java
│   ├── RoleResponse.java
│   ├── MenuRequest.java
│   ├── MenuResponse.java
│   ├── MenuTreeNode.java
│   └── PageResult.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── BusinessException.java
└── util/
    ├── SecurityUtils.java
    └── MenuTreeBuilder.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── mapper/
│   ├── UserMapper.xml
│   ├── RoleMapper.xml
│   ├── PermissionMapper.xml
│   ├── MenuMapper.xml
│   ├── UserRoleMapper.xml
│   ├── RolePermissionMapper.xml
│   └── RoleMenuMapper.xml
├── db/migration/
│   ├── V1__create_base_tables.sql
│   ├── V2__create_rbac_tables.sql
│   └── V3__insert_initial_data.sql
├── templates/
│   ├── layout/
│   │   ├── main.html
│   │   ├── header.html
│   │   ├── sidebar.html
│   │   └── footer.html
│   ├── auth/
│   │   ├── login.html
│   │   └── change-password.html
│   ├── dashboard.html
│   ├── user/
│   │   ├── list.html
│   │   ├── form.html
│   │   └── detail.html
│   ├── role/
│   │   ├── list.html
│   │   └── form.html
│   ├── permission/
│   │   └── list.html
│   └── menu/
│       ├── list.html
│       └── form.html
├── static/
│   ├── css/
│   │   └── custom.css
│   └── js/
│       └── app.js
└── messages/
    └── messages.properties
```

## Database Schema

### Core Tables

**users**
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fullname VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    enabled BOOLEAN DEFAULT true,
    account_locked BOOLEAN DEFAULT false,
    login_attempts INT DEFAULT 0,
    last_login TIMESTAMP,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);
```

**roles**
```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);
```

**permissions**
```sql
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);
```

**menus**
```sql
CREATE TABLE menus (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES menus(id),
    name VARCHAR(50) NOT NULL,
    url VARCHAR(255),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    visible BOOLEAN DEFAULT true,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);
```

### Junction Tables

**user_roles**
```sql
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);
```

**role_permissions**
```sql
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);
```

**role_menus**
```sql
CREATE TABLE role_menus (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    menu_id BIGINT NOT NULL REFERENCES menus(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, menu_id)
);
```

### Relationships

```
User ─┬─> UserRole ──> Role ─┬─> RolePermission ──> Permission
      │                      │
      └──────────────────────┴─> RoleMenu ──> Menu (hierarchical)
```

### Initial Data

The system will include default data:
- Admin user (username: `admin`, password: `admin123`)
- Two roles: `ADMIN`, `USER`
- 12 permissions covering User, Role, and Menu CRUD operations
- 5 default menu items: Dashboard, Master (parent), User, Role, Menu
- Admin role assigned all permissions and menus
- Admin user assigned ADMIN role

## Security & Authentication

### Spring Security Configuration

**Authentication:**
- Form-based login
- BCrypt password hashing with default strength (10 rounds)
- Remember Me functionality (24 hour token validity)
- Session management (1 concurrent session per user)
- CSRF protection enabled

**Authorization:**
- Method-level security with `@PreAuthorize`
- Permission-based access control using permission codes
- Role-based menu visibility

**Account Security:**
- Account locking after 5 failed login attempts
- Login attempt tracking
- Last login timestamp recording

### Authentication Flow

```
1. User submits login form
   ↓
2. Spring Security Filter intercepts request
   ↓
3. CustomUserDetailsService.loadUserByUsername()
   - Query user by username
   - Check enabled and not locked
   - Load user's roles (via user_roles)
   - Load permissions for all roles (via role_permissions)
   - Build GrantedAuthority list:
     * "ROLE_" + role.name for roles
     * permission.code for permissions
   ↓
4. BCryptPasswordEncoder verifies password
   ↓
5. On Success: CustomAuthenticationSuccessHandler
   - Update last_login timestamp
   - Reset login_attempts to 0
   - Load and cache user's menu tree
   - Redirect to /dashboard
   ↓
6. On Failure: CustomAuthenticationFailureHandler
   - Increment login_attempts
   - Lock account if attempts >= 5
   - Redirect to /login?error
```

### Authorization Implementation

**Method-level security:**
```java
@PreAuthorize("hasAuthority('USER_CREATE')")
public void createUser(UserRequest request) { ... }

@PreAuthorize("hasAuthority('USER_EDIT')")
public void updateUser(Long id, UserRequest request) { ... }
```

**Template-level security:**
```html
<button th:if="${#authorization.expression('hasAuthority(''USER_CREATE'')')}" 
        class="btn btn-primary">Add User</button>

<a th:if="${#authorization.expression('hasAuthority(''USER_EDIT'')')}"
   th:href="@{/users/{id}/edit(id=${user.id})}" 
   class="btn btn-sm btn-warning">Edit</a>
```

### Permission Naming Convention

Format: `{RESOURCE}_{ACTION}`

Examples:
- `USER_VIEW`, `USER_CREATE`, `USER_EDIT`, `USER_DELETE`
- `ROLE_VIEW`, `ROLE_CREATE`, `ROLE_EDIT`, `ROLE_DELETE`
- `MENU_VIEW`, `MENU_CREATE`, `MENU_EDIT`, `MENU_DELETE`

## Dynamic Menu System

### Menu Structure

Menus support unlimited hierarchical nesting via `parent_id` self-reference. Each menu has:
- `name`: Display text
- `url`: Navigation target (nullable for parent menus)
- `icon`: Bootstrap Icon class
- `sort_order`: Display order within same parent
- `visible`: Show/hide toggle
- `parent_id`: Reference to parent menu (null for root)

### Menu Loading Flow

```
1. User successfully authenticates
   ↓
2. Load user's roles
   ↓
3. Query menus assigned to those roles
   SELECT DISTINCT m.* 
   FROM menus m
   JOIN role_menus rm ON m.id = rm.menu_id
   JOIN roles r ON rm.role_id = r.id
   WHERE r.name IN (user's role names)
   AND m.deleted = false
   AND m.visible = true
   ↓
4. MenuTreeBuilder.buildTree()
   - Create MenuTreeNode for each menu
   - Build parent-child relationships via parent_id
   - Sort recursively by sort_order
   - Return list of root nodes (parent_id = null)
   ↓
5. Store in session or request attribute
   ↓
6. Render in sidebar template with recursive Thymeleaf
```

### MenuTreeBuilder Algorithm

```java
public static List<MenuTreeNode> buildTree(List<Menu> flatMenus) {
    Map<Long, MenuTreeNode> nodeMap = new HashMap<>();
    List<MenuTreeNode> roots = new ArrayList<>();
    
    // Convert to nodes
    for (Menu menu : flatMenus) {
        if (!menu.getVisible() || menu.getDeleted()) continue;
        MenuTreeNode node = MenuTreeNode.from(menu);
        nodeMap.put(menu.getId(), node);
    }
    
    // Build relationships
    for (MenuTreeNode node : nodeMap.values()) {
        if (node.getParentId() == null) {
            roots.add(node);
        } else {
            MenuTreeNode parent = nodeMap.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            }
        }
    }
    
    // Sort recursively
    sortRecursive(roots);
    return roots;
}
```

### Sidebar Rendering

Thymeleaf template uses recursive structure to support unlimited nesting:

```html
<ul class="nav flex-column">
    <li th:each="menu : ${menuTree}" class="nav-item">
        <!-- Link for menus with URL -->
        <a th:if="${menu.url != null}" 
           th:href="@{${menu.url}}" 
           class="nav-link">
            <i th:class="${menu.icon}"></i>
            <span th:text="${menu.name}">Menu</span>
        </a>
        
        <!-- Collapsible parent for menus without URL -->
        <a th:if="${menu.url == null && !menu.children.isEmpty()}" 
           class="nav-link" 
           data-bs-toggle="collapse" 
           th:href="'#submenu-' + ${menu.id}">
            <i th:class="${menu.icon}"></i>
            <span th:text="${menu.name}">Parent</span>
            <i class="bi bi-chevron-down ms-auto"></i>
        </a>
        
        <!-- Recursive submenu -->
        <div th:if="${!menu.children.isEmpty()}" 
             th:id="'submenu-' + ${menu.id}" 
             class="collapse">
            <ul class="nav flex-column ms-3">
                <li th:each="child : ${menu.children}" class="nav-item">
                    <!-- Recursive rendering here -->
                </li>
            </ul>
        </div>
    </li>
</ul>
```

## RBAC Master Modules

### Standard CRUD Pattern

All master modules (User, Role, Permission, Menu) follow consistent pattern:

**Routes:**
- `GET /{resource}` → list page with search and pagination
- `GET /{resource}/new` → form page for create
- `POST /{resource}` → save new record
- `GET /{resource}/{id}` → detail page
- `GET /{resource}/{id}/edit` → form page for edit
- `POST /{resource}/{id}` → update existing record
- `POST /{resource}/{id}/delete` → soft delete record

**Controller Layer:**
```java
@Controller
@RequestMapping("/{resource}")
@RequiredArgsConstructor
public class ResourceController extends BaseController {
    
    private final ResourceService service;
    
    @GetMapping
    @PreAuthorize("hasAuthority('RESOURCE_VIEW')")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) { ... }
    
    @GetMapping("/new")
    @PreAuthorize("hasAuthority('RESOURCE_CREATE')")
    public String form(Model model) { ... }
    
    @PostMapping
    @PreAuthorize("hasAuthority('RESOURCE_CREATE')")
    public String save(@Valid @ModelAttribute ResourceRequest request,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) { ... }
    
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('RESOURCE_EDIT')")
    public String edit(@PathVariable Long id, Model model) { ... }
    
    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('RESOURCE_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute ResourceRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) { ... }
    
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('RESOURCE_DELETE')")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) { ... }
}
```

**Service Layer:**
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ResourceService {
    
    private final ResourceMapper mapper;
    
    @Transactional(readOnly = true)
    public PageResult<ResourceResponse> findAll(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<ResourceResponse> data = mapper.findAll(keyword, offset, size);
        int total = mapper.countAll(keyword);
        return PageResult.of(data, total, page, size);
    }
    
    public void create(ResourceRequest request) {
        Resource entity = new Resource();
        // Map request to entity
        entity.setCreatedBy(SecurityUtils.getCurrentUsername());
        entity.setCreatedDate(LocalDateTime.now());
        mapper.insert(entity);
    }
    
    public void update(Long id, ResourceRequest request) {
        Resource entity = mapper.selectByPrimaryKey(id);
        // Map request to entity
        entity.setUpdatedBy(SecurityUtils.getCurrentUsername());
        entity.setUpdatedDate(LocalDateTime.now());
        mapper.updateByPrimaryKey(entity);
    }
    
    public void delete(Long id) {
        Resource entity = mapper.selectByPrimaryKey(id);
        entity.setDeleted(true);
        entity.setUpdatedBy(SecurityUtils.getCurrentUsername());
        entity.setUpdatedDate(LocalDateTime.now());
        mapper.updateByPrimaryKey(entity);
    }
}
```

**Mapper Layer:**

Interface using tk.mybatis for basic CRUD:
```java
@Mapper
public interface ResourceMapper extends tk.mybatis.mapper.common.Mapper<Resource> {
    List<ResourceResponse> findAll(@Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);
    int countAll(@Param("keyword") String keyword);
}
```

XML for complex queries:
```xml
<select id="findAll" resultType="ResourceResponse">
    SELECT id, name, description, created_date
    FROM resources
    WHERE deleted = false
    <if test="keyword != null and keyword != ''">
        AND (name ILIKE '%' || #{keyword} || '%'
             OR description ILIKE '%' || #{keyword} || '%')
    </if>
    ORDER BY created_date DESC
    LIMIT #{limit} OFFSET #{offset}
</select>
```

### User Module Specifics

**Additional features:**
- Password encoding on create/update
- Role assignment via `user_roles` junction table
- Account locking mechanism
- Login attempt tracking

**UserService.create():**
```java
public void create(UserRequest request) {
    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setFullname(request.getFullname());
    user.setEmail(request.getEmail());
    user.setEnabled(true);
    user.setCreatedBy(SecurityUtils.getCurrentUsername());
    user.setCreatedDate(LocalDateTime.now());
    userMapper.insert(user);
    
    // Assign roles
    if (request.getRoleIds() != null) {
        for (Long roleId : request.getRoleIds()) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(roleId);
            userRole.setCreatedBy(SecurityUtils.getCurrentUsername());
            userRoleMapper.insert(userRole);
        }
    }
}
```

### Role Module Specifics

**Additional features:**
- Permission assignment via `role_permissions` junction table
- Menu assignment via `role_menus` junction table

**RoleService.create():**
```java
public void create(RoleRequest request) {
    Role role = new Role();
    role.setName(request.getName());
    role.setDescription(request.getDescription());
    role.setCreatedBy(SecurityUtils.getCurrentUsername());
    role.setCreatedDate(LocalDateTime.now());
    roleMapper.insert(role);
    
    // Assign permissions
    if (request.getPermissionIds() != null) {
        for (Long permissionId : request.getPermissionIds()) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(role.getId());
            rp.setPermissionId(permissionId);
            rp.setCreatedBy(SecurityUtils.getCurrentUsername());
            rolePermissionMapper.insert(rp);
        }
    }
    
    // Assign menus
    if (request.getMenuIds() != null) {
        for (Long menuId : request.getMenuIds()) {
            RoleMenu rm = new RoleMenu();
            rm.setRoleId(role.getId());
            rm.setMenuId(menuId);
            rm.setCreatedBy(SecurityUtils.getCurrentUsername());
            roleMenuMapper.insert(rm);
        }
    }
}
```

### Menu Module Specifics

**Additional features:**
- Parent menu selection (dropdown showing hierarchical structure)
- Validation: URL required for leaf menus, optional for parents
- Cascade visibility: hiding parent hides all children

**MenuMapper.xml:**
```xml
<select id="findAll" resultMap="MenuResultMap">
    SELECT m.*, 
           pm.name as parent_name
    FROM menus m
    LEFT JOIN menus pm ON m.parent_id = pm.id
    WHERE m.deleted = false
    <if test="keyword != null and keyword != ''">
        AND m.name ILIKE '%' || #{keyword} || '%'
    </if>
    ORDER BY m.sort_order, m.created_date DESC
</select>
```

## Thymeleaf Layout System

### Layout Structure

Using Thymeleaf Layout Dialect for consistent page structure:

**main.html (layout template):**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title layout:title-pattern="$CONTENT_TITLE - $LAYOUT_TITLE">Template App</title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    
    <!-- Custom CSS -->
    <link th:href="@{/css/custom.css}" rel="stylesheet">
</head>
<body>
    <!-- Header -->
    <div th:replace="~{layout/header :: header}"></div>
    
    <div class="container-fluid">
        <div class="row">
            <!-- Sidebar -->
            <div class="col-md-2 p-0">
                <div th:replace="~{layout/sidebar :: sidebar}"></div>
            </div>
            
            <!-- Main Content -->
            <div class="col-md-10 p-4">
                <div layout:fragment="content">
                    <!-- Page content goes here -->
                </div>
            </div>
        </div>
    </div>
    
    <!-- Footer -->
    <div th:replace="~{layout/footer :: footer}"></div>
    
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    
    <!-- Custom JS -->
    <script th:src="@{/js/app.js}"></script>
</body>
</html>
```

**Usage in content pages:**
```html
<!DOCTYPE html>
<html th:replace="~{layout/main :: layout(~{::title}, ~{::content})}">
<head><title>User Management</title></head>
<body>
<div th:fragment="content">
    <!-- Page-specific content -->
</div>
</body>
</html>
```

### Common Components

**header.html:**
- Logo/brand
- User profile dropdown
- Logout button
- Breadcrumb

**sidebar.html:**
- Dynamic menu tree from `${menuTree}`
- Recursive rendering for nested menus
- Active state highlighting

**footer.html:**
- Copyright notice
- Version info

## List Page Pattern

### Standard List Template Structure

```html
<!-- Page Header -->
<div class="d-flex justify-content-between align-items-center mb-4">
    <h4 class="mb-0">Resource Management</h4>
    <a th:if="${#authorization.expression('hasAuthority(''RESOURCE_CREATE'')')}"
       th:href="@{/resources/new}" 
       class="btn btn-primary">
        <i class="bi bi-plus"></i> Add Resource
    </a>
</div>

<!-- Flash Messages -->
<div th:if="${successMessage}" 
     class="alert alert-success alert-dismissible fade show">
    <span th:text="${successMessage}"></span>
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
<div th:if="${errorMessage}" 
     class="alert alert-danger alert-dismissible fade show">
    <span th:text="${errorMessage}"></span>
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>

<!-- Search Box -->
<form th:action="@{/resources}" method="get" class="mb-3">
    <div class="input-group">
        <input type="text" 
               name="keyword" 
               class="form-control"
               th:value="${keyword}" 
               placeholder="Search...">
        <button type="submit" class="btn btn-outline-secondary">
            <i class="bi bi-search"></i> Search
        </button>
        <a th:href="@{/resources}" class="btn btn-outline-secondary">
            <i class="bi bi-arrow-clockwise"></i> Refresh
        </a>
    </div>
</form>

<!-- Table -->
<div class="card">
    <div class="card-body p-0">
        <table class="table table-hover mb-0">
            <thead>
                <tr>
                    <th>#</th>
                    <th>Column 1</th>
                    <th>Column 2</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="item, stat : ${items}">
                    <td th:text="${stat.index + 1}"></td>
                    <td th:text="${item.field1}"></td>
                    <td th:text="${item.field2}"></td>
                    <td>
                        <a th:if="${#authorization.expression('hasAuthority(''RESOURCE_EDIT'')')}"
                           th:href="@{/resources/{id}/edit(id=${item.id})}"
                           class="btn btn-sm btn-warning">
                            <i class="bi bi-pencil"></i>
                        </a>
                        <form th:if="${#authorization.expression('hasAuthority(''RESOURCE_DELETE'')')}"
                              th:action="@{/resources/{id}/delete(id=${item.id})}"
                              method="post" 
                              class="d-inline"
                              onsubmit="return confirm('Delete this item?')">
                            <input type="hidden" name="_csrf" th:value="${_csrf.token}">
                            <button type="submit" class="btn btn-sm btn-danger">
                                <i class="bi bi-trash"></i>
                            </button>
                        </form>
                    </td>
                </tr>
                <tr th:if="${items.isEmpty()}">
                    <td colspan="4" class="text-center text-muted">No data found</td>
                </tr>
            </tbody>
        </table>
    </div>
</div>

<!-- Pagination -->
<nav th:if="${pagination.totalPages > 1}" class="mt-3">
    <ul class="pagination justify-content-center">
        <li class="page-item" 
            th:classappend="${pagination.currentPage == 1} ? 'disabled'">
            <a class="page-link" 
               th:href="@{/resources(page=1, keyword=${keyword})}">First</a>
        </li>
        <li class="page-item"
            th:classappend="${pagination.currentPage == 1} ? 'disabled'">
            <a class="page-link" 
               th:href="@{/resources(page=${pagination.currentPage - 1}, keyword=${keyword})}">
                <i class="bi bi-chevron-left"></i>
            </a>
        </li>
        <li th:each="i : ${#numbers.sequence(pagination.startPage, pagination.endPage)}" 
            class="page-item"
            th:classappend="${i == pagination.currentPage} ? 'active'">
            <a class="page-link" 
               th:href="@{/resources(page=${i}, keyword=${keyword})}"
               th:text="${i}"></a>
        </li>
        <li class="page-item"
            th:classappend="${pagination.currentPage == pagination.totalPages} ? 'disabled'">
            <a class="page-link" 
               th:href="@{/resources(page=${pagination.currentPage + 1}, keyword=${keyword})}">
                <i class="bi bi-chevron-right"></i>
            </a>
        </li>
        <li class="page-item" 
            th:classappend="${pagination.currentPage == pagination.totalPages} ? 'disabled'">
            <a class="page-link" 
               th:href="@{/resources(page=${pagination.totalPages}, keyword=${keyword})}">Last</a>
        </li>
    </ul>
</nav>
```

### Pagination Logic

**PageResult DTO:**
```java
@Data
@Builder
public class PageResult<T> {
    private List<T> data;
    private Pagination pagination;
    
    public static <T> PageResult<T> of(List<T> data, int total, int page, int size) {
        return PageResult.<T>builder()
            .data(data)
            .pagination(Pagination.of(total, page, size))
            .build();
    }
}

@Data
@Builder
public class Pagination {
    private int totalRecords;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private int startPage;
    private int endPage;
    
    public static Pagination of(int total, int page, int size) {
        int totalPages = (int) Math.ceil((double) total / size);
        
        // Calculate start and end page for pagination display (max 10 pages)
        int startPage = Math.max(1, page - 5);
        int endPage = Math.min(totalPages, page + 4);
        
        if (endPage - startPage < 9) {
            if (startPage == 1) {
                endPage = Math.min(totalPages, startPage + 9);
            } else {
                startPage = Math.max(1, endPage - 9);
            }
        }
        
        return Pagination.builder()
            .totalRecords(total)
            .totalPages(totalPages)
            .currentPage(page)
            .pageSize(size)
            .startPage(startPage)
            .endPage(endPage)
            .build();
    }
}
```

## Form Page Pattern

### Standard Form Template Structure

```html
<!-- Page Header -->
<div class="mb-4">
    <h4 th:text="${resource.id == null ? 'Add Resource' : 'Edit Resource'}"></h4>
</div>

<!-- Form -->
<div class="card">
    <div class="card-body">
        <form th:action="${resource.id == null ? '/resources' : '/resources/' + resource.id}" 
              method="post" 
              th:object="${resource}">
            
            <input type="hidden" name="_csrf" th:value="${_csrf.token}">
            
            <!-- Text Input -->
            <div class="mb-3">
                <label for="name" class="form-label">Name <span class="text-danger">*</span></label>
                <input type="text" 
                       class="form-control" 
                       th:classappend="${#fields.hasErrors('name')} ? 'is-invalid'"
                       id="name" 
                       th:field="*{name}">
                <div class="invalid-feedback" th:errors="*{name}"></div>
            </div>
            
            <!-- Textarea -->
            <div class="mb-3">
                <label for="description" class="form-label">Description</label>
                <textarea class="form-control" 
                          th:classappend="${#fields.hasErrors('description')} ? 'is-invalid'"
                          id="description" 
                          rows="3" 
                          th:field="*{description}"></textarea>
                <div class="invalid-feedback" th:errors="*{description}"></div>
            </div>
            
            <!-- Checkbox -->
            <div class="mb-3 form-check">
                <input type="checkbox" 
                       class="form-check-input" 
                       id="enabled" 
                       th:field="*{enabled}">
                <label class="form-check-label" for="enabled">Enabled</label>
            </div>
            
            <!-- Buttons -->
            <div class="d-flex gap-2">
                <button type="submit" class="btn btn-primary">
                    <i class="bi bi-save"></i> Save
                </button>
                <a th:href="@{/resources}" class="btn btn-secondary">
                    <i class="bi bi-x-circle"></i> Cancel
                </a>
            </div>
        </form>
    </div>
</div>
```

### Validation

**Request DTO with Jakarta Validation:**
```java
@Data
public class ResourceRequest {
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;
    
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    private Boolean enabled = true;
}
```

**Controller validation:**
```java
@PostMapping
public String save(@Valid @ModelAttribute("resource") ResourceRequest request,
                   BindingResult bindingResult,
                   RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        return "resource/form";
    }
    service.create(request);
    redirectAttributes.addFlashAttribute("successMessage", "Resource saved successfully");
    return "redirect:/resources";
}
```

## Exception Handling

### Global Exception Handler

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException ex,
                                          RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:" + ex.getRedirectUrl();
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(HttpServletRequest request, Model model) {
        model.addAttribute("message", "You don't have permission to access this page");
        model.addAttribute("requestUrl", request.getRequestURL());
        return "error/403";
    }
    
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(HttpServletRequest request, Model model) {
        model.addAttribute("message", "Page not found");
        model.addAttribute("requestUrl", request.getRequestURL());
        return "error/404";
    }
    
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        model.addAttribute("message", "An unexpected error occurred");
        model.addAttribute("detail", ex.getMessage());
        return "error/500";
    }
}
```

### Custom Business Exception

```java
@Getter
public class BusinessException extends RuntimeException {
    private final String redirectUrl;
    
    public BusinessException(String message, String redirectUrl) {
        super(message);
        this.redirectUrl = redirectUrl;
    }
}
```

### Error Pages

Standard error templates in `templates/error/`:
- `403.html` - Access denied
- `404.html` - Not found
- `500.html` - Server error

## Utilities

### SecurityUtils

```java
public class SecurityUtils {
    
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() 
            && !(auth instanceof AnonymousAuthenticationToken)) {
            return auth.getName();
        }
        return "system";
    }
    
    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(authority));
    }
    
    public static boolean hasAnyAuthority(String... authorities) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Set<String> authSet = Arrays.stream(authorities).collect(Collectors.toSet());
        return auth.getAuthorities().stream()
            .anyMatch(a -> authSet.contains(a.getAuthority()));
    }
}
```

### MenuTreeBuilder

```java
public class MenuTreeBuilder {
    
    public static List<MenuTreeNode> buildTree(List<Menu> flatMenus) {
        Map<Long, MenuTreeNode> nodeMap = new HashMap<>();
        List<MenuTreeNode> roots = new ArrayList<>();
        
        // Convert to nodes
        for (Menu menu : flatMenus) {
            if (!menu.getVisible() || menu.getDeleted()) continue;
            
            MenuTreeNode node = MenuTreeNode.builder()
                .id(menu.getId())
                .name(menu.getName())
                .url(menu.getUrl())
                .icon(menu.getIcon())
                .sortOrder(menu.getSortOrder())
                .parentId(menu.getParentId())
                .children(new ArrayList<>())
                .build();
            
            nodeMap.put(menu.getId(), node);
        }
        
        // Build parent-child relationships
        for (MenuTreeNode node : nodeMap.values()) {
            if (node.getParentId() == null) {
                roots.add(node);
            } else {
                MenuTreeNode parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        
        // Sort recursively by sort_order
        sortRecursive(roots);
        
        return roots;
    }
    
    private static void sortRecursive(List<MenuTreeNode> nodes) {
        nodes.sort(Comparator.comparing(MenuTreeNode::getSortOrder));
        nodes.forEach(node -> {
            if (!node.getChildren().isEmpty()) {
                sortRecursive(node.getChildren());
            }
        });
    }
}
```

## Audit Trail

### BaseEntity

All entities extend BaseEntity for automatic audit fields:

```java
@Data
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
    
    @Column(name = "deleted")
    private Boolean deleted = false;
    
    @Version
    @Column(name = "version")
    private Integer version = 0;
}
```

### Audit Population

Services populate audit fields on create/update:

```java
// On create
entity.setCreatedBy(SecurityUtils.getCurrentUsername());
entity.setCreatedDate(LocalDateTime.now());

// On update
entity.setUpdatedBy(SecurityUtils.getCurrentUsername());
entity.setUpdatedDate(LocalDateTime.now());

// On delete (soft delete)
entity.setDeleted(true);
entity.setUpdatedBy(SecurityUtils.getCurrentUsername());
entity.setUpdatedDate(LocalDateTime.now());
```

## Configuration

### application.yml

```yaml
spring:
  application:
    name: template-spring-boot
  
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/template_db}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    
  thymeleaf:
    cache: false
    encoding: UTF-8
    mode: HTML
    prefix: classpath:/templates/
    suffix: .html
    
  security:
    user:
      name: admin
      password: admin123

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.template.entity
  configuration:
    map-underscore-to-camel-case: true
    
mapper:
  mappers: tk.mybatis.mapper.common.Mapper
  not-empty: false
  identity: POSTGRES

logging:
  level:
    root: INFO
    com.template: DEBUG
    org.springframework.security: DEBUG
```

### application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/template_db
    username: postgres
    password: postgres
    
logging:
  level:
    org.springframework.web: DEBUG
    com.template.mapper: TRACE
```

## Testing Strategy

### Unit Testing

**Service layer tests:**
- Mock mappers with Mockito
- Test business logic in isolation
- Verify audit field population
- Test validation logic

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void shouldCreateUserWithEncodedPassword() {
        // Given
        UserRequest request = new UserRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        
        // When
        userService.create(request);
        
        // Then
        verify(userMapper).insert(argThat(user -> 
            user.getUsername().equals("testuser") &&
            user.getPassword().equals("$2a$10$encoded") &&
            user.getCreatedBy() != null
        ));
    }
}
```

### Integration Testing

**Controller tests:**
- Use `@SpringBootTest` with test configuration
- Test full request/response cycle
- Verify security annotations

```java
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", authorities = {"USER_VIEW", "USER_CREATE"})
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldDisplayUserList() throws Exception {
        mockMvc.perform(get("/users"))
            .andExpect(status().isOk())
            .andExpect(view().name("user/list"))
            .andExpect(model().attributeExists("users"))
            .andExpect(model().attributeExists("pagination"));
    }
    
    @Test
    @WithAnonymousUser
    void shouldRedirectToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }
}
```

**Mapper tests:**
- Use `@MybatisTest` for MyBatis slice testing
- Test with in-memory H2 database or Testcontainers PostgreSQL
- Verify complex queries

```java
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserMapperTest {
    
    @Autowired
    private UserMapper userMapper;
    
    @Test
    void shouldFindUserByUsername() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encoded");
        userMapper.insert(user);
        
        // When
        User found = userMapper.findByUsername("testuser");
        
        // Then
        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("testuser");
    }
}
```

## Out of Scope (Future Phases)

The following features are NOT included in Phase 1+2:

- Generic CRUD templates and form components (Phase 3)
- Reusable UI components (modal, toast, confirm dialog) (Phase 4)
- Asynchronous search with AJAX
- Advanced pagination (10/20/50/100 rows per page selector)
- Sorting on table columns
- Production monitoring and health checks (Phase 5)
- Docker containerization (Phase 5)
- Dashboard widgets
- File upload functionality
- Excel import/export
- Email templates
- Scheduler/cron jobs
- REST API with Swagger/OpenAPI
- Internationalization (i18n)
- Multi-tenancy
- Theme switching
- WebSocket notifications
- Activity log
- Approval workflow

## Success Criteria

Phase 1+2 is considered complete when:

- ✅ User can login with username/password
- ✅ BCrypt password hashing implemented
- ✅ Remember Me functionality works
- ✅ Session management (1 concurrent session)
- ✅ Account locks after 5 failed attempts
- ✅ User can logout successfully
- ✅ Dynamic menu displays based on user's role
- ✅ Menu supports unlimited hierarchical nesting
- ✅ User CRUD with role assignment
- ✅ Role CRUD with permission and menu assignment
- ✅ Permission CRUD (list and view only)
- ✅ Menu CRUD with parent-child relationship
- ✅ All pages have search functionality
- ✅ All lists have pagination (First/Prev/Next/Last)
- ✅ Form validation (client and server-side)
- ✅ Flash messages for success/error
- ✅ Delete confirmation dialog
- ✅ Audit fields auto-populated (created/updated by/date)
- ✅ Soft delete for all entities
- ✅ Permission-based button visibility
- ✅ Global exception handling (403, 404, 500)
- ✅ CSRF protection enabled
- ✅ Responsive layout with Bootstrap 5
- ✅ Flyway migrations execute successfully
- ✅ Initial admin user can login

## Implementation Notes

1. **Start with database**: Run Flyway migrations first to establish schema
2. **Build from bottom up**: Entity → Mapper → Service → Controller → View
3. **Test as you go**: Write unit tests for services after implementing each module
4. **Reuse patterns**: Once User module is complete, replicate pattern for Role, Permission, Menu
5. **Security first**: Implement authentication before adding RBAC modules
6. **Menu last**: Menu module depends on Role being complete for proper testing

## Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Thymeleaf Extras -->
    <dependency>
        <groupId>org.thymeleaf.extras</groupId>
        <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    </dependency>
    <dependency>
        <groupId>nz.net.ultraq.thymeleaf</groupId>
        <artifactId>thymeleaf-layout-dialect</artifactId>
    </dependency>
    
    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>
    <dependency>
        <groupId>tk.mybatis</groupId>
        <artifactId>mapper-spring-boot-starter</artifactId>
        <version>4.2.3</version>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter-test</artifactId>
        <version>3.0.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-27 | Initial design for Phase 1 + Phase 2 |
