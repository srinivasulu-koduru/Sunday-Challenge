# Database Documentation — Sunday Challenge (Stage 2)

## Overview

The **Sunday Challenge** platform uses **MySQL 8.x** as its relational database management system.

In **Stage 2 (Google Authentication & User Database)**, the database name is explicitly configured as **`Sunday_challenge`**.

---

## 1. Database Creation

Open your MySQL command line client or workbench tool (e.g., MySQL Workbench, DBeaver, phpMyAdmin, or `mysql` CLI) and execute:

```sql
-- Create database if it does not already exist
CREATE DATABASE IF NOT EXISTS Sunday_challenge
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

---

## 2. Spring Data JPA & Hibernate Auto-DDL

The backend is configured with:
```properties
spring.jpa.hibernate.ddl-auto=update
```
Hibernate automatically manages and creates the **`users`** table schema upon initial application boot without dropping existing data.

---

## 3. Database Table Verification

After starting the Spring Boot application and completing the first Google login:

### A. List Tables
```sql
USE Sunday_challenge;
SHOW TABLES;
```
*Expected Output: `users`*

### B. Describe Table Schema
```sql
DESCRIBE users;
```
*Schema Structure:*
- `id` (BIGINT, Primary Key, Auto-Increment)
- `google_id` (VARCHAR(255), UNIQUE)
- `name` (VARCHAR(255))
- `email` (VARCHAR(255), NOT NULL, UNIQUE)
- `profile_image` (VARCHAR(512))
- `role` (VARCHAR(255), Enum `STUDENT` / `ADMIN`)
- `created_at` (DATETIME, Auto Creation Timestamp)
- `updatedAt` (DATETIME, Auto Update Timestamp)

### C. Inspect Registered Users
```sql
SELECT id, google_id, name, email, role, created_at FROM users;
```

---

## 4. Promoting a User to ADMIN

All new users logging in via Google OAuth are assigned the **`STUDENT`** role by default.

To promote an existing student user to **`ADMIN`** for development and testing, run:

```sql
USE Sunday_challenge;

UPDATE users
SET role = 'ADMIN'
WHERE email = 'your-email@gmail.com';
```

Verify the role change:
```sql
SELECT email, role FROM users WHERE email = 'your-email@gmail.com';
```
