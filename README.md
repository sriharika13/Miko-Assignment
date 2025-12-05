# Asynchronous API Gateway Using Vert.x

## Overview

This project implements a simple asynchronous API Gateway using Vert.x. The gateway:

- Exposes an HTTP server listening on port `8080`.
- Defines a single endpoint `/aggregate` that calls two external APIs in parallel.
- Aggregates the responses from these APIs into a single JSON object.
- Handles failures gracefully and returns meaningful error messages.
- Follows a modular design, separating configuration, services, handlers, and exception handling.

---

## Project Structure
com.gateway <br>
│<br>
├── ApiGatewayVerticle.java # Main Verticle that sets up server and routes<br>
├── config<br>
│ └── AppConfig.java # Configuration class for server, API URLs, and WebClient<br>
├── service<br>
│ └── ApiService.java # Service class for HTTP requests<br>
├── handler<br>
│ ├── AggregateHandler.java # Handles /aggregate endpoint<br>
│ └── GlobalExceptionHandler.java # Global error handling<br>
└── exception<br>
└── ApiException.java # Custom exception for API failures<br>

## Approach

### 1. Vert.x Server Setup

- `ApiGatewayVerticle` is the main entry point.
- Configures router, body handler, and routes.
- Registers `AggregateHandler` for `/aggregate`.
- Registers `GlobalExceptionHandler` for route failures.
- Uses `VertxOptions` to set thread pool and event loop size for non-blocking performance.

### 2. Configuration

- `AppConfig` holds server port, external API URLs, HTTP timeout, and `WebClient` instance.
- `WebClient` is configured with connect and idle timeouts, keep-alive, and SSL support.
- Centralized configuration ensures modularity and easy future changes.

### 3. Service Layer

- `ApiService` encapsulates all HTTP calls.
- Exposes a simple method:

```
Future<JsonObject> fetch(String url)
```

- Keeps handlers thin and focused on business logic.

### 4. AggregateHandler Logic
- Uses Circuit Breaker for each API to prevent cascading failures.
- Calls two external APIs in parallel using CompositeFuture.
- Combines responses into a single JSON object:
```
{
  "post_title": "API response title",
  "author_name": "API response name"
}
```

- Implements partial fallback: if one API fails, the gateway still returns available data with a warning.
- Handles full failures by delegating to GlobalExceptionHandler.

### 5. Global Exception Handling
- All exceptions are captured by GlobalExceptionHandler.
- Returns a standardized JSON response:
```
{
  "error": true,
  "message": "Descriptive message",
  "errorCode": "CATEGORY_CODE",
  "statusCode": 500
}
```
-Ensures consistent error format for clients.

### 6. Testing

- Tests are written using JUnit 5 and VertxTestContext. Components tested:
- ApiGatewayVerticle – verifies server startup and /aggregate endpoint.
- AggregateHandler – tests aggregation logic and partial fallback behavior.
- ApiService – tests HTTP client functionality.
- GlobalExceptionHandler – tests error handling responses.
- AppConfig – tests configuration and WebClient initialization.

- Focus is on readable, maintainable, minimal tests without overengineering.

### 7. Asynchronous & Non-blocking Design
- The gateway is fully non-blocking.
- Parallel API calls are achieved using CompositeFuture.
- Circuit breakers prevent downstream failures from affecting the gateway.

## How to Run the Code-
- Build the project
```
mvn clean package
```
- Run the code
```
mvn clean compile exec:java
```
- Test /aggregate endpoint
```
http://localhost:8080/aggregate
```
- Run all tests-
```
mvn clean test
```

