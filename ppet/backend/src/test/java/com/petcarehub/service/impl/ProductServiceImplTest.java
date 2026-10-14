package com.petcarehub.service.impl;

import com.petcarehub.entity.Inventory;
import com.petcarehub.entity.Product;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.InventoryRepository;
import com.petcarehub.repository.ProductCategoryRepository;
import com.petcarehub.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock ProductRepository products;
    @Mock ProductCategoryRepository categories;
    @Mock InventoryRepository inventory;
    @InjectMocks ProductServiceImpl service;

    @Test
    void detailUsesInventoryQuantity() {
        Product product = new Product();
        product.setId(1L);
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(inventory.findByProductId(1L)).thenReturn(Optional.of(Inventory.builder().quantity(45).build()));
        assertEquals(45, service.getProductById(1L).getStockQuantity());
    }

    @Test
    void missingInventoryMeansOutOfStock() {
        Product product = new Product();
        product.setId(1L);
        when(products.findById(1L)).thenReturn(Optional.of(product));
        assertEquals(0, service.getProductById(1L).getStockQuantity());
    }

    @Test
    void missingProductUsesNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () -> service.getProductById(999L));
    }

    @Test
    void categoryFiltersWithoutActiveParameter() {
        var pageable = PageRequest.of(0, 20);
        Product product = new Product();
        product.setId(2L);
        when(products.findByCategoryId(3L, pageable)).thenReturn(new PageImpl<>(List.of(product)));
        assertEquals(2L, service.getAllProducts(3L, null, pageable).getContent().get(0).getId());
        verify(products, never()).findAll(pageable);
    }
}
