# E-Commerce UML Class Diagram Specification

Create the UML diagram from the database table entities below. Each entity
must be shown as a class with its fields and main business functionality.
Do not add any other classes or services.

## Table Entities

These are the eight database tables defined in `ecommerce.sql`.

### `User` — `users`

**Fields**

- `id: Long` — primary key
- `name: String`
- `email: String` — required and unique
- `password: String` — required
- `role: String`

**Functionality**

- `+register(): User`
- `+login(): User`
- `+authenticate(): boolean`
- `+getProfile(): User`

### `Category` — `categories`

**Fields**

- `id: Long` — primary key
- `name: String` — required and unique
- `description: String`

**Functionality**

- `+createCategory(): Category`
- `+getCategory(): Category`
- `+listCategories(): List<Category>`

### `Product` — `products`

**Fields**

- `id: Long` — primary key
- `name: String`
- `description: String`
- `price: Double`
- `stockQuantity: Integer`
- `categoryId: Long` — foreign key to `Category.id`

**Functionality**

- `+createProduct(): Product`
- `+getProduct(): Product`
- `+listProducts(): List<Product>`
- `+findByCategory(): List<Product>`
- `+searchByName(): List<Product>`
- `+validateStock(quantity: Integer): boolean`
- `+getCurrentPrice(): Double`

### `Cart` — `carts`

**Fields**

- `id: Long` — primary key
- `userId: Long` — logical reference to `User.id`
- `version: Long` — optimistic-locking version

**Functionality**

- `+getCart(): Cart`
- `+addItem(productId: Long, quantity: Integer): Cart`
- `+removeItem(productId: Long): Cart`
- `+calculateTotal(): Double`
- `+validateConcurrentUpdate(): boolean`

### `CartItem` — `cart_items`

**Fields**

- `id: Long` — primary key
- `cartId: Long` — foreign key to `Cart.id`
- `productId: Long` — logical reference to `Product.id`
- `quantity: Integer`
- `price: Double`

**Functionality**

- `+increaseQuantity(quantity: Integer): CartItem`
- `+decreaseQuantity(quantity: Integer): CartItem`
- `+calculateSubtotal(): Double`

### `Order` — `orders`

**Fields**

- `id: Long` — primary key
- `userId: Long` — logical reference to `User.id`
- `totalPrice: Double`
- `status: String`
- `paymentStatus: String`

**Functionality**

- `+createOrder(): Order`
- `+calculateTotal(): Double`
- `+setPaymentPending(): Order`
- `+confirmOrder(): Order`
- `+markPaymentFailed(): Order`
- `+getOrderStatus(): String`

### `OrderItem` — `order_items`

**Fields**

- `id: Long` — primary key
- `orderId: Long` — foreign key to `Order.id`
- `productId: Long` — logical reference to `Product.id`
- `quantity: Integer`
- `price: Double`

**Functionality**

- `+calculateSubtotal(): Double`
- `+validateQuantity(): boolean`
- `+getProductPrice(): Double`

### `Payment` — `payments`

**Fields**

- `id: Long` — primary key
- `orderId: Long` — unique logical reference to `Order.id`
- `amount: Double`
- `paymentMethod: String`
- `status: String`
- `transactionId: String`
- `failureReason: String`
- `createdAt: Instant`

**Functionality**

- `+validatePayment(): boolean`
- `+processPayment(): Payment`
- `+completePayment(transactionId: String): Payment`
- `+failPayment(reason: String): Payment`
- `+isIdempotentRetry(amount: Double, paymentMethod: String): boolean`
- `+getPaymentStatus(): String`

## Entity Relationships

Use solid UML associations for relationships represented in the database:

- `Category "1" --> "0..*" Product`
- `Cart "1" *-- "0..*" CartItem`
- `Order "1" *-- "1..*" OrderItem`

Use dashed UML dependencies for cross-service logical references:

- `User "1" ..> "0..*" Cart` through `Cart.userId`
- `User "1" ..> "0..*" Order` through `Order.userId`
- `Product "1" ..> "0..*" CartItem` through `CartItem.productId`
- `Product "1" ..> "0..*" OrderItem` through `OrderItem.productId`
- `Order "1" ..> "0..1" Payment` through `Payment.orderId`
