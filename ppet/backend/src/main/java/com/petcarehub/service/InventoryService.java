package com.petcarehub.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.petcarehub.dto.inventory.InventoryTransactionDTO;
import com.petcarehub.dto.inventory.InventoryDTO;
import com.petcarehub.dto.inventory.StockUpdateRequest;

public interface InventoryService {
    boolean checkStock(Long productId, Integer requiredQuantity);
    void reduceStock(Long productId, Integer quantity);
    void addStock(StockUpdateRequest request);
    InventoryDTO getInventory(Long productId);
    List<InventoryDTO> getLowStockItems(Integer threshold);
    org.springframework.data.domain.Page<com.petcarehub.dto.inventory.InventoryTransactionDTO> getTransactionHistory(Long inventoryId, org.springframework.data.domain.Pageable pageable);
}

