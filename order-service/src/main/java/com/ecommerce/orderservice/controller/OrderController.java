package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.client.UserClient;
import com.ecommerce.orderservice.client.UserDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserClient userClient;

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.placeOrder(order));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrderHistory(@PathVariable Long userId,
                                                       Authentication authentication) {
        verifyOwner(userId, authentication);
        List<Order> orders = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId,
                                          Authentication authentication) {
        Order order = orderService.getOrder(orderId);
        verifyOwner(order.getUserId(), authentication);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long orderId,
                                             Authentication authentication) {
        Order order = orderService.getOrder(orderId);
        verifyOwner(order.getUserId(), authentication);
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Long orderId,
                                              @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateStatus(orderId, status));
    }

    private void verifyOwner(Long userId, Authentication authentication) {
        UserDTO user = userClient.getUserById(userId);
        if (user == null || authentication == null
                || !user.email().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order access denied.");
        }
    }
}