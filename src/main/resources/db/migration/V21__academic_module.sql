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

-- Menus under a top-level 'Akademi' parent.
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

-- TEACHER role: basic view permission so they can log in and see menus.
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

-- ADMIN manages all academic modules.
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