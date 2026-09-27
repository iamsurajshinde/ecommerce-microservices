package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.service.CartService;
import com.ecommerce.cartservice.client.UserClient;
import com.ecommerce.cartservice.client.UserDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserClient userClient;

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable Long userId,
                                        Authentication authentication) {
        verifyOwner(userId, authentication);
        Cart cart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<Cart> addItem(@PathVariable Long userId,
                                        @RequestParam Long productId,
                                        @RequestParam Integer quantity,
                                        Authentication authentication) {
        verifyOwner(userId, authentication);
        Cart updatedCart = cartService.addItemToCart(userId, productId, quantity);
        return ResponseEntity.ok(updatedCart);
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<Cart> removeItem(@PathVariable Long userId, @PathVariable Long productId,
                                           Authentication authentication) {
        verifyOwner(userId, authentication);
        Cart updatedCart = cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok(updatedCart);
    }

    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<Cart> updateItem(@PathVariable Long userId, @PathVariable Long productId,
                                           @RequestParam Integer quantity,
                                           Authentication authentication) {
        verifyOwner(userId, authentication);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, productId, quantity));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId,
                                          Authentication authentication) {
        verifyOwner(userId, authentication);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    private void verifyOwner(Long userId, Authentication authentication) {
        UserDTO user = userClient.getUserById(userId);
        if (user == null || authentication == null
                || !user.email().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cart access denied.");
        }
    }
}