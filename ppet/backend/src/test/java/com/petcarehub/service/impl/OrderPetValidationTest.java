package com.petcarehub.service.impl;

import com.petcarehub.dto.order.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.Ownership;
import com.petcarehub.service.InventoryService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderPetValidationTest {
    @Test void petPurchaseIsRejectedBeforeInventoryOrPersistenceRegardlessOfPrice() {
        var orders=mock(OrderRepository.class);var products=mock(ProductRepository.class);var pets=mock(PetRepository.class);
        var users=mock(UserRepository.class);var stock=mock(InventoryService.class);var ownership=mock(Ownership.class);
        var carts=mock(CartRepository.class);
        var service=new OrderServiceImpl(orders,products,pets,users,stock,ownership,carts,null,java.time.Clock.systemUTC(),null,null);
        when(ownership.userId()).thenReturn(1L);when(ownership.hasRole("CUSTOMER")).thenReturn(true);
        var item=new OrderItemDTO();item.setPetId(2L);item.setQuantity(1);
        var request=new OrderCreateRequest();request.setItems(List.of(item));request.setShippingAddress("Địa chỉ test");
        for(Double price:Arrays.asList(null,0.0,-1.0,1500.0)) {
            item.setPrice(price);
            var failure=assertThrows(com.petcarehub.exception.BadRequestException.class,()->service.createOrder(request));
            assertTrue(failure.getMessage().contains("chưa hỗ trợ mua thú cưng"));
        }
        verifyNoInteractions(stock,orders,products,pets,users,carts);
    }
}
