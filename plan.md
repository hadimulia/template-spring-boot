# Web Application Template Roadmap
## Spring Boot + Thymeleaf + RBAC

> Tujuan:
> Membangun template aplikasi web yang reusable sebagai pondasi untuk berbagai aplikasi bisnis menggunakan **Spring Boot** dan **Thymeleaf** dengan struktur yang mudah dikembangkan.

---

# 1. Technology Stack

| Component | Technology |
|------------|------------|
| Java | Java 21 |
| Framework | Spring Boot |
| Build Tool | Maven |
| View Engine | Thymeleaf |
| CSS | Bootstrap 5 |
| Javascript | Vanilla JS / HTMX (optional) |
| Database | PostgreSQL |
| ORM |  MyBatis |
| Security | Spring Security |
| Validation | Jakarta Validation |
| Authentication | Form Login |
| Authorization | RBAC |
| Migration | Flyway |
| Logging | Logback |
| JSON | Jackson |

---

# 2. Target Architecture

```
Browser
    │
    ▼
Controller
    │
    ▼
Service
    │
    ▼
Repository
    │
    ▼
Database
```

Setiap layer memiliki tanggung jawab yang jelas.

---

# 3. Modul Aplikasi

```
Authentication
│
├── Login
├── Logout
├── Remember Me
└── Change Password

Master
│
├── User
├── Role
├── Permission
├── Menu
└── Setting

Framework
│
├── Validation
├── Pagination
├── Search
├── Notification
├── Exception Handler
└── Audit Log
```

---

# 4. Project Structure

```
src/main/java

config/
controller/
service/
repository/
entity/
dto/
mapper/
security/
validator/
exception/
util/

src/main/resources

templates/
static/
messages/
db/migration/
```

---

# 5. Authentication Module

## Features

- Login
- Logout
- Session Management
- Remember Me
- Password Encoder (BCrypt)
- Login Failure
- Account Lock
- Change Password

---

# 6. Authorization (RBAC)

```
User
 │
 ├── Role
 │      │
 │      ├── Permission
 │      │
 │      └── Menu
```

Relationship

```
User
    |
UserRole
    |
Role
    |
RolePermission
    |
Permission

Role
    |
RoleMenu
    |
Menu
```

---

# Permission Format

```
USER_CREATE
USER_EDIT
USER_DELETE
USER_VIEW

ROLE_CREATE
ROLE_EDIT

MENU_CREATE
```

---

# Menu Structure

```
Dashboard

Master
    User
    Role
    Menu

Transaction
    Order
    Customer

Setting
```

Menu akan dibentuk berdasarkan Role User.

---

# 7. Dynamic Menu

Flow

```
Login

↓

Load User

↓

Load Role

↓

Load Menu

↓

Build Tree

↓

Render Thymeleaf
```

Support

- Unlimited Level
- Icon
- URL
- Parent Menu
- Sort Order
- Visible / Hidden

---

# 8. Generic CRUD Template

Setiap module mempunyai halaman standar.

```
List

Create

Update

Delete

Detail
```

---

# 9. Form Template

Komponen reusable

- Textbox
- Password
- TextArea
- Number
- Date
- Datetime
- Checkbox
- Radio
- Dropdown
- Multi Select
- File Upload

---

# Validation

Client

- Required
- Min Length
- Max Length

Server

- Jakarta Validation
- Custom Validator

Error ditampilkan langsung di field.

---

# 10. List Template

Standar halaman list

```
--------------------------------------
Title

Search Box

Button Add

--------------------------------------

Table

--------------------------------------

Paging
```

---

# Table Features

- Search
- Sorting
- Paging
- Refresh
- Export (future)
- Multi Select (future)

---

# 11. Asynchronous Search

Flow

```
Typing

↓

Ajax Request

↓

Controller

↓

Service

↓

Database

↓

Return HTML Fragment

↓

Replace Table
```

Tanpa reload halaman.

---

# Paging

Support

- First
- Previous
- Number
- Next
- Last

Configurable

```
10

20

50

100
```

rows per page.

---

# 12. Global Layout

```
Header

Sidebar

Content

Footer
```

Menggunakan Thymeleaf Layout.

---

# 13. Error Handling

Global Exception

```
404

403

500

Business Exception
```

Custom Error Page.

---

# 14. Notification

Success

Warning

Error

Info

Menggunakan Toast.

---

# 15. Audit

Automatically save

- Created By
- Created Date
- Updated By
- Updated Date

---

# 16. Base Entity

```
id

createdBy

createdDate

updatedBy

updatedDate

deleted

version
```

---

# 17. Search Framework

Support

```
Like

Equal

Between

Date

Number

Boolean
```

Search object

```
keyword
page
size
sort
direction
```

---

# 18. Reusable Components

```
Navbar

Sidebar

Breadcrumb

Pagination

Search Form

Modal

Alert

Toast

Table

Confirm Delete
```

---

# 19. Reusable Javascript

```
Ajax Helper

Loading Spinner

Modal Helper

Notification

Table Refresh

Form Validation
```

---

# 20. Security

- CSRF
- XSS Protection
- Session Timeout
- BCrypt
- Remember Me
- Access Denied
- Login Retry
- Secure Headers

---

# 21. Database

## User

```
id
username
password
fullname
enabled
```

---

## Role

```
id
name
description
```

---

## Permission

```
id
code
description
```

---

## Menu

```
id
parent_id
name
url
icon
sort_order
visible
```

---

# Relationship

```
user
user_role
role
role_permission
permission
role_menu
menu
```

---

# 22. Development Roadmap

## Phase 1 — Foundation

- Create Project
- Security
- Login
- Base Layout
- Database
- Flyway
- Logging

---

## Phase 2 — RBAC

- User
- Role
- Permission
- Menu
- Dynamic Menu

---

## Phase 3 — Generic CRUD

- Generic Form
- Generic Table
- Validation
- Search
- Paging

---

## Phase 4 — Reusable Components

- Modal
- Toast
- Loading
- Confirm Dialog
- Pagination
- Breadcrumb

---

## Phase 5 — Production Ready

- Audit
- Exception Handler
- Monitoring
- Health Check
- Docker
- Documentation

---

# 23. Coding Standard

```
Controller

↓
Service

↓
Repository

↓
Database
```

Controller tidak boleh mengakses Repository secara langsung.

---

# 24. Naming Convention

Controller

```
UserController
RoleController
```

Service

```
UserService
RoleService
```

Repository

```
UserRepository
RoleRepository
```

DTO

```
UserRequest
UserResponse
UserSearch
```

---

# 25. Future Enhancement

- Dashboard Widget
- File Upload
- Import Excel
- Export Excel
- Export PDF
- Email Template
- Scheduler
- REST API
- Swagger/OpenAPI
- Multi Language (i18n)
- Multi Tenant
- Theme Switching (Light/Dark)
- Notification Center
- WebSocket
- Activity Log
- Approval Workflow

---

# 26. Definition of Done (MVP)

Sebuah modul dianggap selesai jika telah memiliki:

- ✅ Login & autentikasi
- ✅ Otorisasi berbasis RBAC
- ✅ Dynamic menu
- ✅ Halaman List
- ✅ Pencarian asynchronous
- ✅ Sorting
- ✅ Paging
- ✅ Form Create/Edit
- ✅ Validasi client & server
- ✅ Konfirmasi Delete
- ✅ Notifikasi sukses/gagal
- ✅ Audit (created/updated)
- ✅ Error handling
- ✅ Unit test dasar (Service)
- ✅ Dokumentasi singkat

---

# 27. Vision

Template ini ditujukan sebagai **starter kit enterprise** untuk aplikasi internal perusahaan, sehingga setiap modul baru cukup fokus pada logika bisnis tanpa perlu membangun ulang fitur-fitur dasar. Dengan arsitektur yang modular, reusable, dan mengikuti praktik terbaik Spring Boot, template ini dapat menjadi fondasi bagi berbagai jenis aplikasi seperti ERP, CRM, HRIS, Inventory, Finance, maupun sistem administrasi lainnya.
