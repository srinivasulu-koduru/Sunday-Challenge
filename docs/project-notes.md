# Project Notes — Sunday Challenge (Stage 2)

## Architectural Foundation (Stage 2: Google Authentication & User Foundation)

### 1. Authentication Flow Architecture
Sunday Challenge relies exclusively on **Google OAuth 2.0** for user identity management. Username/password login is intentionally omitted.

```text
Browser / Frontend (http://localhost:5500)
    │
    ├─► 1. User clicks "Continue with Google"
    │      Redirects to: GET http://localhost:8080/oauth2/authorization/google
    │
Backend / Spring Security (http://localhost:8080)
    │
    ├─► 2. Initiates OAuth2 authorization code flow with Google
    │
Google OAuth Provider
    │
    ├─► 3. User authenticates & grants scopes (openid, profile, email)
    │
Spring Security Callback
    │
    ├─► 4. Receives authorization code at: http://localhost:8080/login/oauth2/code/google
    ├─► 5. CustomOAuth2UserService intercepts claims (sub, name, email, picture)
    ├─► 6. Performs User Lookup / Account Link / Provisioning in MySQL (Sunday_challenge)
    │      - Always assigns STUDENT role to new accounts.
    │      - Preserves existing role for returning users.
    ├─► 7. Establishes server-side HTTP session (JSESSIONID cookie)
    │
Frontend Redirect
    │
    └─► 8. OAuth2AuthenticationSuccessHandler redirects browser to:
           http://localhost:5500/pages/student-dashboard.html
```

---

### 2. Spring Security 6 Authorization Rules

- **Public Endpoints**:
  - `GET /api/health`
  - `/oauth2/**`
  - `/login/**`
  - `/error`
  - `/logout`
- **Protected Endpoints**:
  - `GET /api/user/me` (Requires authentication, returns 401 Unauthorized if unauthenticated)

---

### 3. Session & CORS Configuration
- **CORS**: Configured with `allowCredentials(true)` for `http://localhost:5500` and `http://127.0.0.1:5500`.
- **Cookies**: HTTP-only session cookies (`JSESSIONID`) managed by the browser.
- **Frontend Fetch Calls**: Must include `credentials: "include"` when issuing REST requests to `http://localhost:8080`.

---

### 4. Account Linking Strategy
If a Google account logs in with an email address matching an existing record without a `googleId`, `CustomOAuth2UserService` links the `googleId` to that record securely, updating name and profile picture without altering the user's assigned `role`.
