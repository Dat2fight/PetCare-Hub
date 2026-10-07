package com.petcarehub.service;

import com.petcarehub.dto.cart.AddToCartRequest;
import com.petcarehub.dto.cart.CartDTO;
import com.petcarehub.dto.cart.UpdateCartItemRequest;

public interface CartService {
    CartDTO getCurrentUserCart(Long userId);
    CartDTO addItemToCart(Long userId, AddToCartRequest request);
    CartDTO updateCartItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request);
    void removeItemFromCart(Long userId, Long cartItemId);
    void clearCart(Long userId);
}
