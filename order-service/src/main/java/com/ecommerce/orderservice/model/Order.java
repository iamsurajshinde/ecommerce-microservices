package com.ecommerce.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Double totalPrice;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String paymentStatus;

    private String paymentLinkUrl;

    @Transient
    private String paymentMethod;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items;
}