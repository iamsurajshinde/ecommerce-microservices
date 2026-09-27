# E-Commerce Java 17 Microservices Project

This project contains a fully modular, enterprise-grade e-commerce microservices architecture implemented with **Java 17**, **Spring Boot 3.x**, **Spring Cloud**, **Spring Security (JWT)**, **PostgreSQL**.

## Architecture & Services
1. **discovery-server** (Port 8761): Eureka Service Discovery.
2. **api-gateway** (Port 8080): Spring Cloud Gateway routing.
3. **user-service** (Port 8081): User registration, authentication, roles, and JWT issuance.
4. **product-service** (Port 8082): Product catalog, category management, keyword search.

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

## Sprint Roadmap Covered
- **Sprint 1:** Infrastructure Setup (Discovery Server & API Gateway)
- **Sprint 2:** User Management Module (Spring Security, JWT, BCrypt)
- **Sprint 3:** Product Catalog Module (JPA entities, search & filtering)