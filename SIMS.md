# 🎯 SIMS (Sistem Informasi Manajemen Sekolah)

---

## 🏗️ 1. Arsitektur Aplikasi

Menggunakan pendekatan **Modular Monolith (awal)** → dapat dikembangkan menjadi **Microservices** jika skala sistem semakin besar.

```        |
[ API Gateway / Backend ]
        |
-------------------------------------------------
| Auth | Akademik | E-Raport | SPMB | HR | Keuangan |
-------------------------------------------------
        |
[ Database (Relational) ]
        |
[ File Storage (Dokumen, Raport, dll) ]
```

---

## 🧰 2. Rekomendasi Tech Stack

### 🔙 Backend
- Java 21 + Spring Boot 3.x  
- Spring Security (RBAC Multi Role)  
- Spring Data JPA / MyBatis  
- Redis (cache session & lookup)  

### 🎨 Frontend
- Bootstrap, jquery, css3,Javascript | Vanilla JS / HTMX (optional)
- thyemleaf

### 🗄️ Database
- PostgreSQL (relational & kuat untuk transaksi)  

### ⚙️ Infrastructure
- Docker + Docker Compose  
- Nginx (reverse proxy)  
- MinIO (file storage: raport, dokumen SPMB)  , local sotrage

### 🔗 Integrasi Tambahan
- RabbitMQ (notifikasi async)  
- JasperReports / OpenPDF (export raport & slip gaji)  

---

## 👥 3. Role & Akses

| Role          | Akses Utama                         |
|--------------|----------------------------------|
| Admin        | Full access                      |
| Guru         | Akademik, nilai, absensi        |
| Siswa        | Jadwal, nilai, raport           |
| Tata Usaha   | Surat, data siswa               |
| Keuangan     | Pembayaran & payroll            |

---

## 🧩 4. Modul Utama

### 1. Manajemen Pengguna
- Master data:
  - Guru  
  - Siswa  
  - User login  
- Role-based access control  

---

### 2. Akademik
- Jadwal pelajaran  
- Absensi:
  - Guru  
  - Siswa (QR Code opsional)  
- Ujian online (CBT)  

---

### 3. E-Raport
- Nilai formatif & sumatif  
- Perhitungan otomatis  
- Export PDF raport  

---

### 4. SPMB (Pendaftaran)
- Registrasi online  
- Upload dokumen  
- Verifikasi & status  

---

### 5. HR & Payroll
- Data kepegawaian  
- Penggajian:
  - Gaji pokok  
  - Tunjangan  
  - Potongan  

---

### 6. Keuangan & Tata Usaha
- Pembayaran SPP  
- Arus kas  
- Surat menyurat  
