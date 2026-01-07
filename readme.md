LocalServer Project - Java Implementation
Task Breakdown for 3 Students
Student 1: Core Server & Event-Driven Architecture
Main Tasks:
Server Lifecycle & Socket Management

Implement Server.java with start/stop methods

Create non-blocking socket channels using java.nio

Handle multiple ports listening using ServerSocketChannel

Implement timeout mechanism for connections

Event-Driven I/O with Selector

Create EventLoop.java using Selector for non-blocking I/O

Handle OP_ACCEPT, OP_READ, OP_WRITE events

Manage connection states and timeouts

Configuration Parser

Implement ConfigLoader.java to parse JSON configuration

Support multiple servers on different host:port combinations

Validate configuration at startup

Main Entry Point

Create Main.java to bootstrap the server

Handle graceful shutdown (Ctrl+C)

Deliverables:
Server.java - Main server class

EventLoop.java - Event-driven selector loop

ConfigLoader.java - Configuration parser

Main.java - Entry point

Basic configuration file in JSON format

Key Requirements:
Single process, single thread

Never crashes (handle all exceptions)

Non-blocking I/O only

Support multiple ports

Student 2: HTTP Protocol Implementation & Request Handling
Main Tasks:
HTTP Request Parser

Implement HttpRequest.java to parse HTTP/1.1 requests

Handle chunked and unchunked request bodies

Parse headers, method, URI, and query parameters

Support GET, POST, DELETE methods

HTTP Response Builder

Implement HttpResponse.java to build HTTP responses

Generate proper status codes and headers

Handle content-length and chunked transfer encoding

Support cookies and session headers

Static File Server

Implement StaticFileHandler.java to serve static files

Support MIME type detection

Handle directory listings (configurable)

Implement file upload handling for POST requests

Error Handler

Create ErrorHandler.java for error responses

Generate default error pages for 400, 403, 404, 405, 413, 500

Support custom error pages from configuration

Deliverables:
HttpRequest.java - HTTP request parser

HttpResponse.java - HTTP response builder

StaticFileHandler.java - Static file server

ErrorHandler.java - Error response generator

Sample static files and error pages

Key Requirements:
HTTP/1.1 compliant

Handle file uploads

Support cookies and sessions

Timeout handling for slow requests

Student 3: Router, CGI & Session Management
Main Tasks:
Router & Route Configuration

Implement Router.java to match URLs to handlers

Support route configuration from JSON

Handle HTTP method restrictions per route

Implement URL redirections

CGI Handler Implementation

Create CGIHandler.java to execute external scripts

Implement at least one CGI (Python recommended)

Use ProcessBuilder to spawn CGI processes

Set proper environment variables (PATH_INFO, etc.)

Handle CGI input/output through pipes

Session & Cookie Management

Implement SessionManager.java for server-side sessions

Create CookieUtils.java for cookie parsing/generation

Secure session storage with timeout

Testing Suite

Create comprehensive test suite

Stress test with Siege (availability > 99.5%)

Memory leak detection tests

Test all error cases and edge conditions

Deliverables:
Router.java - Request router

CGIHandler.java - CGI executor

SessionManager.java - Session handler

CookieUtils.java - Cookie utilities

Complete test suite

Sample CGI scripts (Python)

Key Requirements:
Safe CGI execution with input sanitization

Thread-safe session management

Memory leak free

Comprehensive testing

