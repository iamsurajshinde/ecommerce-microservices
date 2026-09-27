# E-Commerce Database UML Diagram

```mermaid
classDiagram
    class User {
        <<table users>>
        +Long id PK
        +String name
        +String email UNIQUE
        +String password
        +String role
        +register()
        +login()
        +updateProfile()
    }

    class Category {
        <<table categories>>
        +Long id PK
        +String name UNIQUE
        +String description
        +createCategory()
        +getProductsByCategory()
    }

    class Product {
        <<table products>>
        +Long id PK
        +String name
        +String description
        +Double price
        +Integer stockQuantity
        +Long categoryId FK
        +Index idx_products_category_id(categoryId)
        +updateStock()
        +checkAvailability()
    }

    class Cart {
        <<table carts>>
        +Long id PK
        +Long userId UNIQUE
        +Long version
        +getOrCreateCart()
        +calculateTotal()
    }

    class CartItem {
        <<table cart_items>>
        +Long id PK
        +Long cartId FK
        +Long productId
        +Integer quantity
        +Double price
        +Index idx_cart_items_cart_id(cartId)
        +updateQuantity()
        +getSubtotal()
    }

    class Order {
        <<table orders>>
        +Long id PK
        +Long userId
        +Double totalPrice
        +String status
        +String paymentStatus
        +placeOrder()
        +updateOrderStatus()
    }

    class OrderItem {
        <<table order_items>>
        +Long id PK
        +Long orderId FK
        +Long productId
        +Integer quantity
        +Double price
        +Index idx_order_items_order_id(orderId)
        +calculateItemTotal()
    }

    class Payment {
        <<table payments>>
        +Long id PK
        +Long orderId UNIQUE
        +Double amount
        +String paymentMethod
        +String status
        +String transactionId
        +String failureReason
        +Instant createdAt
        +UniqueIndex uk_payments_order_id(orderId)
        +processPayment()
        +refund()
    }

    Category "1" --> "1..*" Product : contains
    User "1" --> "1..*" Order : places
    User "1" --> "0..1" Cart : owns
    Cart "1" --> "1..*" CartItem : contains
    Order "1" --> "1..*" OrderItem : contains
    Order "1" --> "0..1" Payment : has
```

**Legend**

- `PK` = primary key.
- `FK` = database foreign key.
- `UNIQUE` = unique constraint or unique column.
- `Index` and `UniqueIndex` show database indexes.
- Solid relationships represent database-owned foreign keys.
- Dashed relationships represent logical cross-service references; these are
  not database foreign keys.
