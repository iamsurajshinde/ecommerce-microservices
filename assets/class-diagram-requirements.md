# E-Commerce Class Diagram Requirements

## Purpose

Generate a UML class diagram for the current `ecommerce-microservices`
workspace. The diagram must be aligned with the Java entities, Spring Data
repositories, service clients, controllers, and the consolidated
[`ecommerce.sql`](./ecommerce.sql) schema.

The system is split into independent Spring Boot services:

| Service | Responsibility | Port |
| --- | --- | ---: |
| `user-service` | Registration, login, JWT issuance, user lookup | 8081 |
| `product-service` | Products, categories, inventory lookup | 8082 |
| `cart-service` | User carts and cart items | 8083 |
| `order-service` | Orders, order items, product/user validation, payment orchestration | 8084 |
| `payment-service` | Mock payment processing and payment lookup | 8085 |
| `api-gateway` | External routing and unified Swagger service selector | 8080 |

## Diagram scope

Show the following layers:

1. **Domain/entity classes**
2. **Spring Data repository interfaces**
3. **Application services**
4. **Feign client interfaces and DTOs**
5. **REST controllers**
6. **JWT/security components**
7. **API Gateway and Eureka service discovery**

Use separate package or service boundaries. Do not represent every generated
Lombok method. Show important fields, repository methods, service methods, and
cross-service calls.

## Domain entities

### User Service

`User`

- `id: Long` — primary key
- `name: String`
- `email: String` — required and unique
- `password: String` — required and write-only in JSON
- `role: String`

`UserRepository extends JpaRepository<User, Long>`

- `findByEmail(email): Optional<User>`
- `existsByEmail(email): boolean`

`UserService`

- `saveUser(user): User`
- `existsByEmail(email): boolean`
- `authenticate(email, password): User`
- `getUserById(id): User`

`UserController`

- `POST /api/users/register`
- `POST /api/users/login`
- `GET /api/users/{id}`

`JwtService` creates tokens containing the user email, user ID, role, issue
time, and expiration. `JwtAuthenticationFilter` validates bearer tokens for
protected requests.

### Product Service

`Category`

- `id: Long` — primary key
- `name: String` — required and unique
- `description: String`

`Product`

- `id: Long` — primary key
- `name: String`
- `description: String`
- `price: Double`
- `stockQuantity: Integer`
- `category: Category`

JPA relationship:

- One `Category` can be referenced by many `Product` records.
- `Product.category` is `@ManyToOne(fetch = LAZY)`.
- Database relationship: `products.category_id -> categories.id`.

`CategoryRepository extends JpaRepository<Category, Long>`

- `findByNameIgnoreCase(name): Optional<Category>`

`ProductRepository extends JpaRepository<Product, Long>`

- `findByCategory_NameIgnoreCase(category): List<Product>`
- `findByCategory_Id(categoryId): List<Product>`
- `findByNameContainingIgnoreCase(keyword): List<Product>`

`ProductController`

- `GET /api/products`
- `GET /api/products/category/{categoryId}`
- `GET /api/products/{id}`
- `POST /api/products`

`CategoryController`

- `GET /api/categories`
- `GET /api/categories/{id}`
- `POST /api/categories`

### Cart Service

`Cart`

- `id: Long` — primary key
- `version: Long` — JPA optimistic-locking field
- `userId: Long` — logical reference to `User.id`
- `items: List<CartItem>`

`CartItem`

- `id: Long` — primary key
- `productId: Long` — logical reference to `Product.id`
- `quantity: Integer`
- `price: Double`

JPA relationships:

- One `Cart` contains many `CartItem` records.
- `Cart.items` is `@OneToMany(cascade = ALL, orphanRemoval = true)`.
- Items use the `cart_id` join column.
- `Cart.version` is annotated with `@Version`.

`CartRepository extends JpaRepository<Cart, Long>`

- `findByUserId(userId): Optional<Cart>`

`CartService`

- `getOrCreateCart(userId): Cart`
- `addItemToCart(userId, productId, quantity): Cart`
- `removeItemFromCart(userId, productId): Cart`
- `calculateCartTotal(cart): Double`

`CartController`

- `GET /api/carts/{userId}`
- `POST /api/carts/{userId}/items?productId={id}&quantity={quantity}`
- `DELETE /api/carts/{userId}/items/{productId}`

Cart service calls:

- `UserClient -> user-service -> GET /api/users/{id}`
- `ProductClient -> product-service -> GET /api/products/{id}`

The product service supplies the authoritative item price and stock quantity;
the request must not be treated as the price source.

### Order Service

`Order`

- `id: Long` — primary key
- `userId: Long` — logical reference to `User.id`
- `totalPrice: Double`
- `status: String`
- `paymentStatus: String`
- `paymentMethod: String` — transient request field, not stored in `orders`
- `items: List<OrderItem>`

`OrderItem`

- `id: Long` — primary key
- `productId: Long` — logical reference to `Product.id`
- `quantity: Integer`
- `price: Double`

JPA relationship:

- One `Order` contains many `OrderItem` records.
- `Order.items` is `@OneToMany(cascade = ALL)`.
- Items use the `order_id` join column.

`OrderRepository extends JpaRepository<Order, Long>`

- `findByUserId(userId): List<Order>`

`OrderService`

- Validates the user through `UserClient`.
- Validates products and stock through `ProductClient`.
- Calculates authoritative item prices and order total.
- Saves a pending order.
- Calls `PaymentClient`.
- Changes the order to `CONFIRMED` and payment status `SUCCESS` after payment.
- Changes the order to `PAYMENT_FAILED` and payment status `FAILED` on failure.

`OrderController`

- `POST /api/orders`
- `GET /api/orders/user/{userId}`

Feign clients:

- `UserClient -> user-service -> UserDTO`
- `ProductClient -> product-service -> ProductDTO`
- `PaymentClient -> payment-service -> PaymentRequest / PaymentDTO`

The Feign interceptor forwards the incoming `Authorization: Bearer <JWT>`
header to downstream services.

### Payment Service

`Payment`

- `id: Long` — primary key
- `orderId: Long` — unique logical reference to `Order.id`
- `amount: Double`
- `paymentMethod: String`
- `status: String`
- `transactionId: String`
- `failureReason: String`
- `createdAt: Instant`

Payment methods currently supported by the mock processor:

- `CREDIT_CARD`
- `DEBIT_CARD`
- `UPI`
- `NET_BANKING`
- `WALLET`

`PaymentRepository extends JpaRepository<Payment, Long>`

- `findByOrderId(orderId): Optional<Payment>`

`PaymentService`

- Validates order ID and positive finite amount.
- Normalizes the payment method to uppercase.
- Rejects unsupported payment methods.
- Returns an existing matching payment for an idempotent retry.
- Rejects a different amount or method for an already-paid order.
- Creates a mock transaction ID and marks successful payments as `SUCCESS`.

`PaymentController`

- `POST /api/payments/process`
- `GET /api/payments/order/{orderId}`

## Database schema requirements

The diagram must match [`ecommerce.sql`](./ecommerce.sql).

Tables:

- `users`
- `categories`
- `products`
- `carts`
- `cart_items`
- `orders`
- `order_items`
- `payments`

Database constraints and indexes:

- Unique user email.
- Unique category name.
- Unique cart per user.
- Unique payment per order.
- `products.category_id` foreign key to `categories.id`.
- `cart_items.cart_id` foreign key to `carts.id` with cascade delete.
- `order_items.order_id` foreign key to `orders.id` with cascade delete.
- Indexes on product category, cart items cart ID, and order items order ID.

`userId`, `productId`, and `orderId` are cross-service logical IDs. They are
not database foreign keys because each service owns a separate database.

## Security and infrastructure relationships

Show these non-domain relationships:

```text
Client
  -> API Gateway
  -> Eureka service discovery
  -> user/product/cart/order/payment service

User Service
  -> issues JWT

Each protected service
  -> JwtAuthenticationFilter
  -> JwtService
  -> Spring Security

Cart Service
  -> UserClient
  -> ProductClient

Order Service
  -> UserClient
  -> ProductClient
  -> PaymentClient
```

All protected APIs use the shared JWT signing secret and bearer authentication.
Swagger UI exposes a `bearerAuth` security scheme.

## Required relationship labels

Use these labels in the generated diagram:

- `Category 1 ---- * Product`
- `Cart 1 *---- * CartItem` — composition
- `Order 1 *---- * OrderItem` — composition
- `User 1 ---- 0..1 Cart` — logical service relationship
- `User 1 ---- * Order` — logical service relationship
- `Product 1 ---- * CartItem` — logical service relationship
- `Product 1 ---- * OrderItem` — logical service relationship
- `Order 1 ---- 0..1 Payment` — logical service relationship with unique payment

Use a dashed line for logical cross-service relationships and a solid line for
JPA/database-owned relationships.

## Image-generation prompt

Create a clean professional UML class diagram for a Java 17 Spring Boot
e-commerce microservices system. Use five clearly separated service
boundaries: User Service, Product Service, Cart Service, Order Service, and
Payment Service. Include entity classes with their important fields,
repository interfaces with key query methods, service classes with their
business responsibilities, REST controllers with endpoint groups, Feign
clients, JWT authentication components, API Gateway, and Eureka discovery.

Show solid relationships for database-owned JPA associations:
`Category 1-to-many Product`, `Cart 1-to-many CartItem`, and
`Order 1-to-many OrderItem`. Show dashed relationships for cross-service
logical IDs: User-to-Cart, User-to-Order, Product-to-CartItem,
Product-to-OrderItem, and Order-to-Payment. Clearly label cardinalities,
foreign-key columns, cascade behavior, orphan removal, and the Cart optimistic
locking `version` field. Show that Order Service calls User Service, Product
Service, and Payment Service through Feign clients, and that Cart Service calls
User Service and Product Service. Show API Gateway routing all external API
requests and JWT bearer authentication protecting the services.

Use a left-to-right layout, service boundaries with distinct but restrained
colors, readable typography, no overlapping connectors, and no generated
method boilerplate. The final image should be suitable for a software
architecture document and must reflect the exact entities and relationships
listed in this requirements document.

## Verification checklist

- Every SQL table is represented by its owning entity.
- Every JPA `@OneToMany`, `@ManyToOne`, join column, and cascade rule is shown.
- Cross-service IDs are shown as logical references, not database foreign keys.
- Repository interfaces are attached to the correct entity.
- Feign clients point to the correct downstream service.
- Order-to-payment flow is visible.
- JWT issuance and validation flow is visible.
- API Gateway routes are visible.
- The diagram does not invent inventory, shipment, review, or address entities
  that are not present in the workspace.
