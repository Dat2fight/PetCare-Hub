package com.petcarehub.service.impl;

import com.petcarehub.dto.cart.AddToCartRequest;
import com.petcarehub.dto.cart.CartDTO;
import com.petcarehub.dto.cart.UpdateCartItemRequest;
import com.petcarehub.service.CartService;
import org.springframework.stereotype.Service;
import com.petcarehub.exception.ResourceNotFoundException;

@Service
public class CartServiceImpl implements CartService {

    @Override
    public CartDTO getCurrentUserCart(Long userId) {
        // TODO: Implement fetching cart from DB
        return new CartDTO();
    }

    @Override
    public CartDTO addItemToCart(Long userId, AddToCartRequest request) {
        // TODO: Implement adding item
        return new CartDTO();
    }

    @Override
    public CartDTO updateCartItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        // TODO: Implement updating quantity
        return new CartDTO();
    }

    @Override
    public void removeItemFromCart(Long userId, Long cartItemId) {
        // TODO: Implement removing item
    }

    @Override
    public void clearCart(Long userId) {
        // TODO: Implement clearing cart
    }
}
