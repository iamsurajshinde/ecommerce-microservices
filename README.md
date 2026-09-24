# E-Commerce Java 17 Microservices Project

This project contains a fully modular, enterprise-grade e-commerce microservices architecture implemented with **Java 17**, **Spring Boot 3.x**, **Spring Cloud**, **Spring Security (JWT)**, **PostgreSQL**.

## Architecture & Services
1. **discovery-server** (Port 8761): Eureka Service Discovery.
2. **api-gateway** (Port 8080): Spring Cloud Gateway routing & JWT security filter.
3. **user-service** (Port 8081): User registration, authentication, roles, profile management.

## Sprint Roadmap Covered
- **Sprint 1:** Infrastructure Setup (Discovery Server & API Gateway)
- **Sprint 2:** User Management Module (Spring Security, JWT, BCrypt)