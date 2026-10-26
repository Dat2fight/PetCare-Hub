package com.petcarehub.service.impl;

import com.petcarehub.dto.inventory.StockUpdateRequest;
import com.petcarehub.entity.Inventory;
import com.petcarehub.enums.TransactionType;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceImplTest {
    final InventoryRepository inventory = mock(InventoryRepository.class);
    final InventoryTransactionRepository transactions = mock(InventoryTransactionRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final InventoryServiceImpl service = new InventoryServiceImpl(inventory, transactions, users);

    @Test void nonPositiveQuantitiesNeverTouchDatabase() {
        for (int quantity : new int[]{-1, 0}) {
            var request = new StockUpdateRequest(); request.setQuantity(quantity); request.setProductId(1L);
            assertThrows(BadRequestException.class, () -> service.addStock(request));
            assertThrows(BadRequestException.class, () -> service.reduceStock(1L, quantity));
        }
        verifyNoInteractions(inventory, transactions);
    }

    @Test void stockNeverBecomesNegativeAndOnlySuccessfulOperationsAreLogged() {
        var stock = Inventory.builder().quantity(3).build();
        when(inventory.findLockedByProductId(1L)).thenReturn(Optional.of(stock));
        assertThrows(IllegalStateException.class, () -> service.reduceStock(1L, 4));
        verifyNoInteractions(transactions);
        service.reduceStock(1L, 3);
        assertEquals(0, stock.getQuantity());
        assertThrows(IllegalStateException.class, () -> service.reduceStock(1L, 1));
        verify(transactions).save(argThat(t -> t.getQuantity() == 3 && t.getTransactionType() == TransactionType.STOCK_OUT));
        var request = new StockUpdateRequest(); request.setProductId(1L); request.setQuantity(5);
        service.addStock(request); assertEquals(5, stock.getQuantity());
        service.adjustStock(1L, 2, "Kiểm kê"); assertEquals(2, stock.getQuantity());
        verify(transactions).save(argThat(t -> t.getQuantity() == -3 && t.getTransactionType() == TransactionType.ADJUSTMENT));
        assertThrows(BadRequestException.class, () -> service.adjustStock(1L, -1, "Kiểm kê"));
        assertThrows(BadRequestException.class, () -> service.adjustStock(1L, 1, ""));
    }
}
