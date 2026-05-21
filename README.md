# Distributed Banking Backend

Transactional banking API built with **Java**, **Spring Boot**, **Spring Security**, **Spring Data JPA**, and **PostgreSQL**.

## Goals

- JWT-authenticated REST API for accounts and transfers
- ACID-compliant money movement with pessimistic row locking
- Deadlock-free multi-account locking (ordered by account ID)
- Idempotent transfers via `Idempotency-Key` header (safe client retries)

## Stack

- Java 21, Spring Boot 3.x
- Spring Security + JWT
- Spring Data JPA, PostgreSQL
- JUnit 5, Testcontainers (integration + concurrency tests)
- Maven

## Status

Architecture and implementation in progress. See project docs in this repo as layers are added.

## License

MIT
