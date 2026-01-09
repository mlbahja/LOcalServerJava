# Team Task Distribution

## Team Overview

| Member | Role | Focus Area |
|--------|------|------------|
| **Member A** | Infrastructure Lead | Core server, NIO, configuration, networking |
| **Member B** | Protocol Lead | HTTP parsing, routing, response building |
| **Member C** | Features Lead | CGI, sessions, cookies, file handling, error pages |

---

## Task Distribution

### Member A - Infrastructure Lead

| Priority | Task | Description | Dependencies |
|----------|------|-------------|--------------|
| 1 | Project Structure | Create directories: `src/`, `error_pages/`, `config.json` | None |
| 2 | ConfigLoader | Parse JSON config (hosts, ports, routes, limits) | Task A1 |
| 3 | Core Server (NIO) | Implement `Selector`-based event loop, `ServerSocketChannel` | Task A2 |
| 4 | Multiple Servers | Support multiple ports/virtual hosts from config | Task A3 |
| 5 | Timeout Handling | Implement request timeout mechanism | Task A3 |

**Files to create:**
- `src/Main.java`
- `src/Server.java`
- `src/ConfigLoader.java`
- `config.json`

---

### Member B - Protocol Lead

| Priority | Task | Description | Dependencies |
|----------|------|-------------|--------------|
| 1 | HTTP Request Parser | Parse request line, headers, body, chunked encoding | None |
| 2 | HTTP Response Builder | Build HTTP/1.1 responses with status codes & headers | None |
| 3 | Router | Route matching, methods validation, redirections | Tasks B1, B2 |
| 4 | Static File Serving | Serve files, index files, directory listing | Task B3 |
| 5 | File Uploads | Handle `multipart/form-data`, body size limits | Tasks B1, B3 |

**Files to create:**
- `src/HttpRequest.java`
- `src/HttpResponse.java`
- `src/Router.java`

---

### Member C - Features Lead

| Priority | Task | Description | Dependencies |
|----------|------|-------------|--------------|
| 1 | Cookie Utilities | Parse and build HTTP cookies | None |
| 2 | Session Management | Session creation, storage, expiration | Task C1 |
| 3 | Error Pages | Create HTML pages + error response handler | None |
| 4 | CGI Handler | Execute `.py` scripts via `ProcessBuilder`, `PATH_INFO` | None |
| 5 | Integration & Testing | Connect all components, stress test with siege | All tasks |

**Files to create:**
- `src/utils/Cookie.java`
- `src/utils/Session.java`
- `src/ErrorHandler.java`
- `src/CGIHandler.java`
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
├── Member A: Project Structure → ConfigLoader → Start Server
├── Member B: HTTP Request Parser → HTTP Response Builder
└── Member C: Cookie Utils → Session Management → Error Pages

SYNC POINT 1: Integration meeting - agree on interfaces
─────────────────────────────────────────────────────────

Week 2: Core Features (Parallel Work)
├── Member A: Complete Server (NIO) → Multiple Servers
├── Member B: Router → Static File Serving
└── Member C: CGI Handler → Error Handler integration

SYNC POINT 2: Integration meeting - connect components
─────────────────────────────────────────────────────────

Week 3: Integration & Polish
├── Member A: Timeout Handling → Bug fixes
├── Member B: File Uploads → Bug fixes
└── Member C: Full Integration → Stress Testing (siege)

SYNC POINT 3: Final testing - 99.5% availability target
```

---

## Interface Contracts

Define these interfaces early so team members can work independently:

### 1. HttpRequest Interface (Member B owns)
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

### 2. HttpResponse Interface (Member B owns)
```java
public class HttpResponse {
    void setStatus(int code, String message);
    void setHeader(String key, String value);
    void setBody(byte[] body);
    void setCookie(Cookie cookie);
    byte[] build();               // Build raw HTTP response
}
```

### 3. Route Interface (Member B owns)
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

### 5. Session Interface (Member C owns)
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
├── feature/infrastructure    (Member A)
├── feature/http-protocol     (Member B)
└── feature/features          (Member C)
```

### Merge Order
1. Member A merges `infrastructure` to `main` first (foundation)
2. Member B merges `http-protocol` to `main` second
3. Member C merges `features` to `main` last (depends on both)

---

## Critical Dependencies Map

```
┌─────────────────────────────────────────────────────────────────┐
│                        MEMBER A                                  │
│  ┌──────────┐    ┌──────────┐    ┌────────┐    ┌──────────┐    │
│  │ Project  │───▶│  Config  │───▶│ Server │───▶│ Multiple │    │
│  │ Structure│    │  Loader  │    │  (NIO) │    │ Servers  │    │
│  └──────────┘    └────┬─────┘    └───┬────┘    └──────────┘    │
│                       │              │                          │
└───────────────────────┼──────────────┼──────────────────────────┘
                        │              │
                        ▼              ▼
┌───────────────────────────────────────────────────────────────────┐
│                        MEMBER B                                    │
│  ┌──────────┐    ┌──────────┐    ┌────────┐    ┌──────────────┐  │
│  │ Request  │───▶│ Response │───▶│ Router │───▶│ Static Files │  │
│  │ Parser   │    │ Builder  │    │        │    │ + Uploads    │  │
│  └──────────┘    └──────────┘    └───┬────┘    └──────────────┘  │
│                                      │                            │
└──────────────────────────────────────┼────────────────────────────┘
                                       │
                                       ▼
┌───────────────────────────────────────────────────────────────────┐
│                        MEMBER C                                    │
│  ┌──────────┐    ┌──────────┐    ┌─────────────┐    ┌──────────┐ │
│  │ Cookies  │───▶│ Sessions │    │ Error Pages │    │   CGI    │ │
│  └──────────┘    └──────────┘    └─────────────┘    │ Handler  │ │
│                                                      └──────────┘ │
│                         ┌──────────────────────┐                  │
│                         │ Integration & Testing │                  │
│                         └──────────────────────┘                  │
└───────────────────────────────────────────────────────────────────┘
```

---

## Parallel Work Matrix

Shows what each member can work on simultaneously:

| Phase | Member A | Member B | Member C |
|-------|----------|----------|----------|
| **1** | Project Structure | HTTP Request Parser | Cookie Utilities |
| **2** | ConfigLoader | HTTP Response Builder | Session Management |
| **3** | Server (NIO) | Router | Error Pages (HTML) |
| **4** | Multiple Servers | Static File Serving | CGI Handler |
| **5** | Timeout Handling | File Uploads | Error Handler |
| **6** | Bug fixes | Bug fixes | Integration Testing |
| **7** | Support | Support | Stress Testing (siege) |

---

## Definition of Done

Each task is complete when:

- [ ] Code compiles without errors
- [ ] Unit tests pass (if applicable)
- [ ] Code reviewed by at least one other member
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
