# Sunday Challenge — Online Assessment Platform

**Sunday Challenge** is an advanced online assessment platform designed for conducting aptitude, reasoning, and coding exams with automatic evaluation, real-time proctoring, and performance analytics.

> [!IMPORTANT]
> **CURRENT STAGE: STAGE 2 — GOOGLE AUTHENTICATION & USER DATABASE**
> Stage 2 integrates Google OAuth 2.0 authentication, Spring Security 6 session management, MySQL `Sunday_challenge` user persistence, and the Student Dashboard REST API (`GET /api/user/me`).

---

## 1. Technology Stack

- **Backend Framework**: Spring Boot 3.3.4
- **Security & OAuth**: Spring Security 6, Spring Security OAuth2 Client
- **Language**: Java 21 (LTS)
- **Build Tool**: Apache Maven (with Maven Wrapper `mvnw` / `mvnw.cmd`)
- **Database**: MySQL 8.x (`Sunday_challenge`)
- **Persistence Layer**: Spring Data JPA / Hibernate
- **Validation**: Spring Boot Starter Validation
- **Frontend**: Vanilla HTML5, Custom Glassmorphism CSS3 Design System, ES6 JavaScript (`fetch` API with `credentials: 'include'`)

---

## 2. Environment Variables & Credentials Setup

Set the following environment variables before starting the backend application:

### Database Credentials
```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=Sunday_challenge
export DB_USER=root
export DB_PASS=your_mysql_password
```

### Google Cloud OAuth 2.0 Credentials
```bash
export GOOGLE_CLIENT_ID=your_google_client_id
export GOOGLE_CLIENT_SECRET=your_google_client_secret
```

> [!CAUTION]
> Never hardcode or commit actual Google client secrets or database passwords into source code or Git repository files!

---

## 3. Google Cloud Console Configuration

To enable Google OAuth 2.0 authentication for local development:

1. Open the [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select a project for **Sunday Challenge**.
3. Navigate to **APIs & Services > Credentials**.
4. Create an **OAuth 2.0 Client ID** (Application type: **Web application**).
5. Add Authorized JavaScript origins:
   - `http://localhost:8080`
   - `http://localhost:5500`
   - `http://127.0.0.1:5500`
6. Add Authorized Redirect URI:
   - `http://localhost:8080/login/oauth2/code/google`
7. Copy the generated **Client ID** and **Client Secret** into your environment variables (`GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`).

---

## 4. Starting the Application

### A. MySQL Database
Ensure MySQL Server is running and create the database:
```sql
CREATE DATABASE IF NOT EXISTS Sunday_challenge;
```

### B. Spring Boot Backend
Navigate into the `backend/` directory and run:
```bash
cd backend
./mvnw spring-boot:run
```
*(Backend runs on `http://localhost:8080`)*

### C. Frontend Server
Serve the `frontend/` folder using a local HTTP server (such as VS Code Live Server or `npx serve` on port `5500`):
```bash
# Example serving frontend on http://localhost:5500
npx serve frontend -p 5500
```

---

## 5. REST API Endpoints (Stage 2)

| Method | Endpoint | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | **Public** | Returns `{ "status": "UP", "message": "Sunday Challenge backend is running" }` |
| `GET` | `/oauth2/authorization/google` | **Public** | Initiates Google OAuth2 login flow |
| `GET` | `/api/user/me` | **Authenticated** | Returns current user profile DTO `{ id, name, email, profileImage, role }` |
| `GET` | `/logout` | **Public / Auth** | Invalidates session cookie & redirects to login page |

---

## 6. Security Checklist

- [x] Google Client ID and Secret configured via environment variables.
- [x] Password authentication disabled (Google OAuth2 exclusively).
- [x] New Google OAuth logins automatically assigned `STUDENT` role.
- [x] User role choice from frontend prevented.
- [x] `/api/user/me` requires authentication (returns `401 Unauthorized` when unauthenticated).
- [x] `/api/health` remains public.
- [x] Session-based server-side authentication (`JSESSIONID` with HTTP-Only & `SameSite=Lax`).
- [x] CORS configured with `allowCredentials(true)` for trusted origins (`http://localhost:5500`).
- [x] Logout invalidates session and clears browser cookies.
