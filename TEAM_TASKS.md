# Team Task Distribution

## Team Overview

| Member | Role | Focus Area |
|--------|------|------------|
| **Member A** | Backend Lead | Core server, NIO, HTTP protocol, configuration, routing |
| **Member B** | Features Lead | CGI, sessions, cookies, file handling, error pages, uploads |

---

## Task Distribution

### Member A - Backend Lead

| Priority | Task | Description | Dependencies |
|----------|------|-------------|--------------|
| 1 | Project Structure | Create directories: `src/`, `error_pages/`, `config.json` | None |
| 2 | ConfigLoader | Parse JSON config (hosts, ports, routes, limits) | Task A1 |
| 3 | HTTP Request Parser | Parse request line, headers, body, chunked encoding | None |
| 4 | HTTP Response Builder | Build HTTP/1.1 responses with status codes & headers | None |
| 5 | Core Server (NIO) | Implement `Selector`-based event loop, `ServerSocketChannel` | Tasks A2, A3, A4 |
| 6 | Router | Route matching, methods validation, redirections | Tasks A3, A4, A5 |
| 7 | Multiple Servers | Support multiple ports/virtual hosts from config | Task A5 |
| 8 | Static File Serving | Serve files, index files, directory listing | Task A6 |
| 9 | Timeout Handling | Implement request timeout mechanism | Task A5 |

**Files to create:**
- `src/Main.java`
- `src/Server.java`
- `src/ConfigLoader.java`
- `src/HttpRequest.java`
- `src/HttpResponse.java`
- `src/Router.java`
- `config.json`

---

### Member B - Features Lead

| Priority | Task | Description | Dependencies |
|----------|------|-------------|--------------|
| 1 | Cookie Utilities | Parse and build HTTP cookies | None |
| 2 | Session Management | Session creation, storage, expiration | Task B1 |
| 3 | Error Pages | Create HTML pages + error response handler | None |
| 4 | CGI Handler | Execute `.py` scripts via `ProcessBuilder`, `PATH_INFO` | None |
| 5 | File Uploads | Handle `multipart/form-data`, body size limits | Tasks B1, A3 |
| 6 | Integration & Testing | Connect all components, stress test with siege | All tasks |

**Files to create:**
- `src/utils/Cookie.java`
- `src/utils/Session.java`
- `src/ErrorHandler.java`
- `src/CGIHandler.java`
- `src/FileUploadHandler.java`
- `error_pages/400.html`
- `error_pages/403.html`
- `error_pages/404.html`
- `error_pages/405.html`
- `error_pages/413.html`
- `error_pages/500.html`

---

## Timeline & Synchronization Points

```
Week 1: Foundation (Parallel Work)
├── Member A: Project Structure → ConfigLoader → HTTP Parser → HTTP Response
└── Member B: Cookie Utils → Session Management → Error Pages

SYNC POINT 1: Integration meeting - agree on interfaces
─────────────────────────────────────────────────────────

Week 2: Core Features (Parallel Work)
├── Member A: Server (NIO) → Router → Multiple Servers
└── Member B: CGI Handler → Error Handler integration → File Uploads

SYNC POINT 2: Integration meeting - connect components
─────────────────────────────────────────────────────────

Week 3: Integration & Polish
├── Member A: Static File Serving → Timeout Handling → Bug fixes
└── Member B: Full Integration → Stress Testing (siege) → Bug fixes

SYNC POINT 3: Final testing - 99.5% availability target
```

---

## Interface Contracts

Define these interfaces early so team members can work independently:

### 1. HttpRequest Interface (Member A owns)
```java
public class HttpRequest {
    String getMethod();           // GET, POST, DELETE
    String getPath();             // /api/users
    String getHeader(String key); // Content-Type, Host, etc.
    byte[] getBody();             // Request body
    Map<String, String> getCookies();
    boolean isChunked();
}
```

### 2. HttpResponse Interface (Member A owns)
```java
public class HttpResponse {
    void setStatus(int code, String message);
    void setHeader(String key, String value);
    void setBody(byte[] body);
    void setCookie(Cookie cookie);
    byte[] build();               // Build raw HTTP response
}
```

### 3. Route Interface (Member A owns)
```java
public class Route {
    String path;
    List<String> allowedMethods;
    String root;
    String defaultFile;
    String cgiExtension;
    boolean directoryListing;
    String redirect;
}
```

### 4. Config Interface (Member A owns)
```java
public class Config {
    String host;
    List<Integer> ports;
    int clientBodyLimit;
    Map<Integer, String> errorPages;
    List<Route> routes;
    String defaultServer;
}
```

### 5. Session Interface (Member B owns)
```java
public class Session {
    String getId();
    void setAttribute(String key, Object value);
    Object getAttribute(String key);
    boolean isExpired();
}
```

---

## Communication Guidelines

### Daily Sync
- 15-minute standup: blockers, progress, needs

### Git Workflow
```
main
├── feature/backend     (Member A)
└── feature/features    (Member B)
```

### Merge Order
1. Member A merges `backend` to `main` first (foundation)
2. Member B merges `features` to `main` second (depends on backend)

---

## Critical Dependencies Map

```
┌──────────────────────────────────────────────────────────────────┐
│                        MEMBER A                                   │
│  ┌──────────┐   ┌──────────┐   ┌─────────┐   ┌────────┐         │
│  │ Project  │──▶│  Config  │──▶│ Request │──▶│Response│         │
│  │ Structure│   │  Loader  │   │ Parser  │   │Builder │         │
│  └──────────┘   └────┬─────┘   └────┬────┘   └───┬────┘         │
│                      │              │            │              │
│                      ▼              ▼            ▼              │
│                 ┌────────┐      ┌────────┐  ┌──────────┐       │
│                 │ Server │─────▶│ Router │─▶│  Static  │       │
│                 │ (NIO)  │      │        │  │  Files   │       │
│                 └───┬────┘      └───┬────┘  └──────────┘       │
│                     │               │                          │
│                     ▼               │                          │
│                 ┌──────────┐        │                          │
│                 │ Multiple │        │                          │
│                 │ Servers  │        │                          │
│                 └──────────┘        │                          │
└─────────────────────────────────────┼──────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────┐
│                        MEMBER B                                   │
│  ┌──────────┐   ┌──────────┐   ┌─────────────┐   ┌──────────┐  │
│  │ Cookies  │──▶│ Sessions │   │ Error Pages │   │   CGI    │  │
│  └──────────┘   └──────────┘   └─────────────┘   │ Handler  │  │
│                                                   └──────────┘  │
│                 ┌──────────────┐                                │
│                 │ File Uploads │                                │
│                 └──────────────┘                                │
│                                                                  │
│                 ┌──────────────────────┐                        │
│                 │ Integration & Testing │                        │
│                 └──────────────────────┘                        │
└──────────────────────────────────────────────────────────────────┘
```

---

## Parallel Work Matrix

Shows what each member can work on simultaneously:

| Phase | Member A | Member B |
|-------|----------|----------|
| **1** | Project Structure | Cookie Utilities |
| **2** | ConfigLoader | Session Management |
| **3** | HTTP Request Parser | Error Pages (HTML) |
| **4** | HTTP Response Builder | CGI Handler |
| **5** | Server (NIO) | Error Handler Integration |
| **6** | Router | File Uploads |
| **7** | Multiple Servers | Integration Testing |
| **8** | Static File Serving | Stress Testing (siege) |
| **9** | Timeout Handling | Bug fixes |
| **10** | Bug fixes | Final Testing |

---

## Definition of Done

Each task is complete when:

- [ ] Code compiles without errors
- [ ] Unit tests pass (if applicable)
- [ ] Code reviewed by the other member
- [ ] Integrated with dependent components
- [ ] No memory leaks detected
- [ ] Documentation updated

## Final Acceptance Criteria

- [ ] Server handles GET, POST, DELETE
- [ ] File uploads work with size limits
- [ ] Cookies and sessions functional
- [ ] CGI scripts execute correctly
- [ ] All error pages display properly
- [ ] Multiple ports supported
- [ ] Request timeouts work
- [ ] `siege -b [IP]:[PORT]` achieves 99.5% availability
- [ ] No crashes under stress