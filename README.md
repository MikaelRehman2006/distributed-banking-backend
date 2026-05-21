# Distributed Banking Backend

Transactional banking API built with **Java 17**, **Spring Boot 3**, **Spring Security**, **Spring Data JPA**, and **PostgreSQL**.

## Features

- JWT authentication (`/auth/register`, `/auth/login`)
- Account balance lookup with ownership checks
- ACID transfers with pessimistic row locks (`SELECT … FOR UPDATE`)
- Deadlock-free locking (accounts locked in ascending ID order)
- Idempotent transfers via `Idempotency-Key` header

## Prerequisites

- Java 17+
- Docker (for PostgreSQL)
- Maven 3.9+ (or use the Maven Wrapper once generated)

## Quick start

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Run the API

```bash
mvn spring-boot:run
```

Set a strong JWT secret (at least 32 characters):

```bash
# PowerShell
$env:JWT_SECRET="replace-with-a-long-random-secret-key-32chars+"
mvn spring-boot:run
```

API base URL: `http://localhost:8080`

### 3. Example flow

**Register**

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"alice@example.com\",\"password\":\"password123\"}"
```

**Login**

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"alice@example.com\",\"password\":\"password123\"}"
```

**Get account** (use `accountId` from register/login response)

```bash
curl http://localhost:8080/accounts/1 \
  -H "Authorization: Bearer YOUR_JWT"
```

**Transfer** (requires a unique idempotency key per logical transfer)

```bash
curl -X POST http://localhost:8080/transfers \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d "{\"sourceAccountId\":1,\"destinationAccountId\":2,\"amount\":10.00}"
```

## API

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | No | Create user + USD account |
| POST | `/auth/login` | No | Issue JWT |
| GET | `/accounts/{id}` | JWT | Account details (owner only) |
| POST | `/transfers` | JWT + `Idempotency-Key` | Execute transfer |
| GET | `/transfers/{id}` | JWT | Transfer details |

## Project layout

```
src/main/java/com/mikaelrehman/banking/
├── config/          Security, JWT properties
├── controller/      REST endpoints
├── service/         Auth, transfers, idempotency
├── repository/      JPA + pessimistic lock query
├── entity/          User, Account, Transfer, IdempotentRequest
├── dto/             Request/response records
├── security/        JWT filter, token provider
└── exception/       API error handling
```

## Tests

```bash
mvn test
```

Uses Testcontainers to spin up PostgreSQL for the context-load test.

## License

MIT
