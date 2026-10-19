package com.petcarehub.controller;

import com.petcarehub.dto.cart.AddToCartRequest;
import com.petcarehub.dto.cart.CartDTO;
import com.petcarehub.dto.cart.UpdateCartItemRequest;
import com.petcarehub.entity.User;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final com.petcarehub.security.Ownership ownership;

    private Long getCurrentUserId() {
        return ownership.userId();
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCart() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(cartService.getCurrentUserCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDTO> addItem(@RequestBody AddToCartRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(cartService.addItemToCart(userId, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> updateItemQuantity(@PathVariable Long itemId, @RequestBody UpdateCartItemRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long itemId) {
        Long userId = getCurrentUserId();
        cartService.removeItemFromCart(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        Long userId = getCurrentUserId();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
