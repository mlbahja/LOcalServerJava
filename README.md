# LocalServer - Java HTTP/1.1 Web Server

## Overview

A lightweight, crash-proof HTTP/1.1 web server built from scratch in Java. This server handles internal APIs and static content with minimal dependencies, using only Java core libraries.

**Role:** Backend engineer building a highly available, crash-proof solution that supports dynamic content via CGI scripts and is configurable for multiple environments.

## Learning Objectives

- Design and implement a custom HTTP/1.1-compliant server in Java
- Utilize non-blocking I/O mechanisms
- Parse and construct HTTP requests and responses manually
- Configure server routes, error pages, uploads, and CGI scripts
- Evaluate performance under stress and ensure memory and process safety

## Technical Skills

- Socket programming
- Asynchronous I/O
- File and process management
- Configuration parsing

---

## Constraints

| Constraint | Details |
|------------|---------|
| Language | Java (required) |
| Libraries | `java.nio` (non-blocking I/O), `java.net` (networking) |
| Architecture | Event-driven API for handling connections |
| Forbidden | Netty, Jetty, Grizzly, or any established server frameworks |
| Threading | Single process, single thread |

---

## Server Requirements

The server must:

- **Never crash** - robust error handling throughout
- **Timeout long requests** - prevent hanging connections
- **Listen on multiple ports** - instantiate multiple server instances
- **Use only one process and one thread** - event-driven architecture
- **HTTP/1.1 compliant** - proper request/response handling
- **Handle methods:** `GET`, `POST`, `DELETE`
- **Receive file uploads** - multipart/form-data support
- **Handle cookies and sessions** - state management
- **Provide default error pages:** 400, 403, 404, 405, 413, 500
- **Use event-driven, non-blocking I/O API** - NIO Selector
- **Manage chunked and unchunked requests** - Transfer-Encoding support
- **Set correct HTTP status in responses**

---

## CGI Support

| Requirement | Details |
|-------------|---------|
| Execution | Execute one type of CGI (e.g., `.py`) using `ProcessBuilder` |
| Arguments | Pass the file to process as the first argument |
| Environment | Use `PATH_INFO` environment variable to define full paths |
| Paths | Ensure correct relative path handling |

---

## Configuration File

The server configuration supports:

- Host and multiple ports
- Default server selection
- Custom error page paths
- Client body size limit
- Routes with:
  - Accepted methods
  - Redirections
  - Directory/file roots
  - Default file for directories
  - CGI by file extension
  - Directory listing toggle
  - Default directory response file

> **Note:** No need for regex support in configuration.

---

## Project Structure

```
/java-localserver
├── src/
│   ├── Main.java           # Entry point
│   ├── Server.java         # Handles server lifecycle, NIO event loop
│   ├── Router.java         # Routes requests
│   ├── CGIHandler.java     # Manages CGI execution
│   ├── ConfigLoader.java   # Parses configuration file
│   ├── HttpRequest.java    # HTTP request parsing
│   ├── HttpResponse.java   # HTTP response building
│   └── utils/
│       ├── Session.java    # Session management
│       └── Cookie.java     # Cookie utilities
├── config.json             # Server configuration
├── README.md               # Documentation
└── error_pages/            # Custom error HTML files
    ├── 400.html
    ├── 403.html
    ├── 404.html
    ├── 405.html
    ├── 413.html
    └── 500.html
```

---

## Work Plan

### Phase 1: Foundation

| # | Task | Description |
|---|------|-------------|
| 1 | Project Structure | Create `src/`, `config.json`, `error_pages/` directories |
| 2 | ConfigLoader | Parse JSON config for hosts, ports, routes, error pages, body limits |
| 3 | Core Server (NIO) | Implement `Selector`-based event loop with `ServerSocketChannel` |

### Phase 2: HTTP Protocol

| # | Task | Description |
|---|------|-------------|
| 4 | HTTP Request Parser | Parse request line, headers, body (chunked & unchunked) |
| 5 | HTTP Response Builder | Build HTTP/1.1 responses with proper status codes & headers |
| 6 | Router | Match routes, handle accepted methods, redirections, directory roots |

### Phase 3: Features

| # | Task | Description |
|---|------|-------------|
| 7 | Static File Serving | Serve files, default index files, directory listing toggle |
| 8 | File Uploads | Handle POST with `multipart/form-data`, respect body size limits |
| 9 | Cookies & Sessions | Parse cookies, manage sessions |
| 10 | CGI Handler | Execute `.py` scripts via `ProcessBuilder`, set `PATH_INFO` |
| 11 | Error Pages | Custom HTML for 400, 403, 404, 405, 413, 500 |

### Phase 4: Robustness

| # | Task | Description |
|---|------|-------------|
| 12 | Timeout Handling | Kill long-running requests |
| 13 | Multiple Servers | Support multiple ports/virtual hosts from config |
| 14 | Stress Testing | Validate with `siege -b [IP]:[PORT]` for 99.5% availability |

---

## Testing

| Test Type | Tool/Method | Target |
|-----------|-------------|--------|
| Stress Testing | `siege -b [IP]:[PORT]` | 99.5% availability |
| Functional Tests | Comprehensive tests | Redirections, configs, error pages |
| Memory Tests | Profiling | No memory leaks |

---

## Tips

- **Avoid hardcoding** - use the config file for all settings
- **Validate configs at startup** - fail fast on invalid configuration
- **Sanitize inputs for CGI** - prevent command injection
- **Modularize components** - separate concerns for maintainability
- **Use thread-safe data structures** - even in single-threaded context
- **Prevent file descriptor and memory leaks** - proper resource cleanup

---

## Resources

- [RFC 2616 - HTTP/1.1 Specification](https://www.rfc-editor.org/rfc/rfc2616)
- [Java NIO Documentation](https://docs.oracle.com/javase/8/docs/api/java/nio/package-summary.html)
- [CGI Protocol Overview](https://www.w3.org/CGI/)
- [Siege Load Testing Tool](https://github.com/JoeDog/siege)

---

## Disclaimer

This project is for educational use only. Using siege or any stress testing tool against a third-party server without explicit permission is illegal and unethical.
