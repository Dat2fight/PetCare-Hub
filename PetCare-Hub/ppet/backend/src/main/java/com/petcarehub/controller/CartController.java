package com.petcarehub.controller;

import com.petcarehub.dto.cart.AddToCartRequest;
import com.petcarehub.dto.cart.CartDTO;
import com.petcarehub.dto.cart.UpdateCartItemRequest;
import com.petcarehub.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCart(@RequestParam Long userId) {
        // In a real app, userId would be extracted from the authenticated principal
        return ResponseEntity.ok(cartService.getCurrentUserCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDTO> addItem(@RequestParam Long userId, @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItemToCart(userId, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> updateItemQuantity(@RequestParam Long userId, @PathVariable Long itemId, @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(@RequestParam Long userId, @PathVariable Long itemId) {
        cartService.removeItemFromCart(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestParam Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
