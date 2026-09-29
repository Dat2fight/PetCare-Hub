package com.petcarehub.controller;

import com.petcarehub.dto.inventory.InventoryDTO;
import com.petcarehub.dto.inventory.StockUpdateRequest;
import com.petcarehub.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryDTO> getInventory(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }

    @PostMapping("/add")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<Void> addStock(@RequestBody StockUpdateRequest request) {
        inventoryService.addStock(request);
        return ResponseEntity.ok().build();
    }
    
    // Note: Stock reduction usually happens internally during order confirmation, 
    // but exposing an endpoint if necessary for staff/admins.
    @PostMapping("/reduce")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<Void> reduceStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        inventoryService.reduceStock(productId, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/low-stock")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<java.util.List<InventoryDTO>> getLowStockItems(
            @RequestParam(defaultValue = "10") Integer threshold) {
        return ResponseEntity.ok(inventoryService.getLowStockItems(threshold));
    }

    @GetMapping("/{inventoryId}/transactions")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<com.petcarehub.dto.inventory.InventoryTransactionDTO>> getTransactionHistory(
            @PathVariable Long inventoryId,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getTransactionHistory(inventoryId, pageable));
    }
}
