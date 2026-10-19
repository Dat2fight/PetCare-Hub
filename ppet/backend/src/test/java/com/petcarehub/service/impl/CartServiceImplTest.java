package com.petcarehub.service.impl;

import com.petcarehub.dto.cart.AddToCartRequest;
import com.petcarehub.entity.*;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock CartRepository carts;
    @Mock UserRepository users;
    @Mock ProductRepository products;
    @Mock InventoryRepository inventory;
    @InjectMocks CartServiceImpl service;
    Product product;
    User customer;
    AddToCartRequest request;

    @BeforeEach
    void setup() {
        product = Product.builder().name("Product").active(true).build();
        product.setId(1L);
        customer = new User();
        customer.setId(5L);
        request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);
    }

    @Test
    void persistsProductInAuthenticatedCustomersCart() {
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(users.findLockedById(5L)).thenReturn(Optional.of(customer));
        when(inventory.findByProductId(1L)).thenReturn(Optional.of(Inventory.builder().quantity(5).build()));
        when(carts.saveAndFlush(any(Cart.class))).thenAnswer(call -> call.getArgument(0));
        var result = service.addItemToCart(5L, request);
        assertEquals(5L, result.getUserId());
        assertEquals(1L, result.getItems().get(0).getProductId());
        assertEquals(2, result.getItems().get(0).getQuantity());
        verify(carts).saveAndFlush(any(Cart.class));
    }

    @Test
    void rejectsCumulativeQuantityOverStockWithoutSaving() {
        Cart cart = Cart.builder().customer(customer).build();
        cart.addItem(CartItem.builder().product(product).quantity(4).build());
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(carts.findByCustomerId(5L)).thenReturn(Optional.of(cart));
        when(inventory.findByProductId(1L)).thenReturn(Optional.of(Inventory.builder().quantity(5).build()));
        when(users.findLockedById(5L)).thenReturn(Optional.of(customer));
        assertThrows(BadRequestException.class, () -> service.addItemToCart(5L, request));
        assertEquals(4, cart.getItems().get(0).getQuantity());
        verify(carts, never()).saveAndFlush(any());
    }

    @Test
    void rejectsNonPositiveQuantity() {
        request.setQuantity(0);
        assertThrows(BadRequestException.class, () -> service.addItemToCart(5L, request));
        verifyNoInteractions(carts, products, inventory);
    }

    @Test
    void readsOnlyRequestedCustomersCart() {
        Cart cart = Cart.builder().customer(customer).build();
        cart.addItem(CartItem.builder().product(product).quantity(2).build());
        when(carts.findByCustomerId(5L)).thenReturn(Optional.of(cart));
        var result = service.getCurrentUserCart(5L);
        assertEquals(5L, result.getUserId());
        assertEquals(2, result.getItems().get(0).getQuantity());
    }
}
