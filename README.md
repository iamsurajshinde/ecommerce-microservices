# E-Commerce Java 17 Microservices Project

This project contains a fully modular, enterprise-grade e-commerce microservices architecture implemented with **Java 17**, **Spring Boot 3.x**, **Spring Cloud**, **Spring Security (JWT)**, **PostgreSQL**, and **OpenFeign**.

## Architecture & Services
1. **discovery-server** (Port 8761): Eureka Service Discovery.
2. **api-gateway** (Port 8080): Spring Cloud Gateway routing.
3. **user-service** (Port 8081): User registration, authentication, roles, and JWT issuance.
4. **product-service** (Port 8082): Product and category catalog.
5. **cart-service** (Port 8083): User carts with optimistic locking for concurrent updates.
6. **order-service** (Port 8084): Order creation, product validation, and payment orchestration.
7. **payment-service** (Port 8085): Secure mock payment processing.

## Authentication

Register and log in through the user service:

```text
POST http://localhost:8080/api/users/register
POST http://localhost:8080/api/users/login
```

Send the returned JWT to protected APIs:

```http
Authorization: Bearer <jwt>
```

Expired or invalid tokens are rejected by each service. Swagger UI exposes an
**Authorize** button for protected APIs.

Swagger UI is available at `/swagger-ui.html` for each service.

The API Gateway provides a unified Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Use the API selector in the UI to switch between User, Product, Cart, Order,
and Payment APIs. Their OpenAPI documents are proxied through the gateway at
`/v3/api-docs/{service-name}`.

## Sprint Roadmap Covered
- **Sprint 1:** Infrastructure Setup (Discovery Server & API Gateway)
- **Sprint 2:** User Management Module (Spring Security, JWT, BCrypt)