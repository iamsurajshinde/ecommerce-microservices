# E-Commerce UML Table Entities

Use only the following database entities when creating the UML diagram.
These are the eight existing tables in `ecommerce.sql`. Do not add service,
controller, repository, client, DTO, security, or infrastructure classes.

## `User` — `users`

**Fields**

- `id: Long`
- `name: String`
- `email: String`
- `password: String`
- `role: String`

**Keys and Indexes**

- Primary key: `id`
- Unique constraint: `email`

## `Category` — `categories`

**Fields**

- `id: Long`
- `name: String`
- `description: String`

**Keys and Indexes**

- Primary key: `id`
- Unique constraint: `name`

## `Product` — `products`

**Fields**

- `id: Long`
- `name: String`
- `description: String`
- `price: Double`
- `stockQuantity: Integer`
- `categoryId: Long`

**Keys and Indexes**

- Primary key: `id`
- Foreign key: `categoryId -> Category.id`
- Index: `idx_products_category_id(categoryId)`

## `Cart` — `carts`

**Fields**

- `id: Long`
- `userId: Long`
- `version: Long`

**Keys and Indexes**

- Primary key: `id`
- Unique constraint: `userId`
- `userId` is a logical reference to `User.id`; no database foreign key

## `CartItem` — `cart_items`

**Fields**

- `id: Long`
- `cartId: Long`
- `productId: Long`
- `quantity: Integer`
- `price: Double`

**Keys and Indexes**

- Primary key: `id`
- Foreign key: `cartId -> Cart.id`
- Index: `idx_cart_items_cart_id(cartId)`
- `productId` is a logical reference to `Product.id`; no database foreign key

## `Order` — `orders`

**Fields**

- `id: Long`
- `userId: Long`
- `totalPrice: Double`
- `status: String`
- `paymentStatus: String`

**Keys and Indexes**

- Primary key: `id`
- `userId` is a logical reference to `User.id`; no database foreign key

## `OrderItem` — `order_items`

**Fields**

- `id: Long`
- `orderId: Long`
- `productId: Long`
- `quantity: Integer`
- `price: Double`

**Keys and Indexes**

- Primary key: `id`
- Foreign key: `orderId -> Order.id`
- Index: `idx_order_items_order_id(orderId)`
- `productId` is a logical reference to `Product.id`; no database foreign key

## `Payment` — `payments`

**Fields**

- `id: Long`
- `orderId: Long`
- `amount: Double`
- `paymentMethod: String`
- `status: String`
- `transactionId: String`
- `failureReason: String`
- `createdAt: Instant`

**Keys and Indexes**

- Primary key: `id`
- Unique index: `uk_payments_order_id(orderId)`
- `orderId` is a unique logical reference to `Order.id`; no database foreign key


## Entity Relationships

- `Category "1" --> "0..*" Product`
- `Cart "1" *-- "0..*" CartItem`
- `Order "1" *-- "1..*" OrderItem`
- `User "1" ..> "0..1" Cart` through `Cart.userId`
- `User "1" ..> "0..*" Order` through `Order.userId`
- `Product "1" ..> "0..*" CartItem` through `CartItem.productId`
- `Product "1" ..> "0..*" OrderItem` through `OrderItem.productId`
- `Order "1" ..> "0..1" Payment` through unique `Payment.orderId`
