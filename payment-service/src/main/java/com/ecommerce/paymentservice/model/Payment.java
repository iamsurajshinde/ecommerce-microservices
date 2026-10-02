package com.ecommerce.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Double amount;
    private String paymentMethod;
    private String status;
    private String transactionId;
    private String failureReason;
    private Instant createdAt;
    private String paymentLinkUrl;
}