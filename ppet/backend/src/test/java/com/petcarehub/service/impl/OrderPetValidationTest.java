package com.petcarehub.service.impl;

import com.petcarehub.dto.order.*;
import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.Ownership;
import com.petcarehub.service.InventoryService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderPetValidationTest {
    @Test void nullOrNonPositivePetPriceNeverReachesInventoryOrPersistence() {
        var orders=mock(OrderRepository.class);var products=mock(ProductRepository.class);var pets=mock(PetRepository.class);
        var users=mock(UserRepository.class);var stock=mock(InventoryService.class);var ownership=mock(Ownership.class);
        var service=new OrderServiceImpl(orders,products,pets,users,stock,ownership);
        when(ownership.userId()).thenReturn(1L);when(ownership.hasRole("CUSTOMER")).thenReturn(true);
        when(users.findById(1L)).thenReturn(Optional.of(new User()));
        Pet pet=Pet.builder().build();when(pets.findByIdForUpdate(2L)).thenReturn(Optional.of(pet));
        var item=new OrderItemDTO();item.setPetId(2L);item.setQuantity(1);
        var request=new OrderCreateRequest();request.setItems(List.of(item));
        for(BigDecimal price:Arrays.asList(null,BigDecimal.ZERO,BigDecimal.ONE.negate())) {
            pet.setPrice(price);assertThrows(IllegalStateException.class,()->service.createOrder(request));
        }
        verifyNoInteractions(stock,orders,products);verify(pets,never()).save(any());
    }
}
