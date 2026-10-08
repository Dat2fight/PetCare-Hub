package com.petcarehub.service.impl;

import com.petcarehub.dto.inventory.InventoryDTO;
import com.petcarehub.dto.inventory.InventoryTransactionDTO;
import com.petcarehub.dto.inventory.StockUpdateRequest;
import com.petcarehub.entity.Inventory;
import com.petcarehub.entity.InventoryTransaction;
import com.petcarehub.entity.User;
import com.petcarehub.enums.TransactionType;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.InventoryRepository;
import com.petcarehub.repository.InventoryTransactionRepository;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository, 
                                InventoryTransactionRepository inventoryTransactionRepository,
                                UserRepository userRepository) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String username = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        if (username != null) {
            return userRepository.findByUsername(username)
                    .orElseGet(() -> userRepository.findById(1L).orElse(null));
        }
        return userRepository.findById(1L).orElse(null);
    }

    @Override
    public boolean checkStock(Long productId, Integer requiredQuantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        return inventory.getQuantity() >= requiredQuantity;
    }

    @Override
    @Transactional
    public void reduceStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));

        if (inventory.getQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock for product " + productId);
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventory(inventory);
        transaction.setTransactionType(TransactionType.STOCK_OUT);
        transaction.setQuantity(quantity);
        transaction.setNotes("Stock reduced");
        transaction.setPerformedBy(getCurrentUser());
        inventoryTransactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void addStock(StockUpdateRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + request.getProductId()));

        inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
        inventoryRepository.save(inventory);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventory(inventory);
        transaction.setTransactionType(TransactionType.STOCK_IN);
        transaction.setQuantity(request.getQuantity());
        transaction.setNotes(request.getReason());
        transaction.setPerformedBy(getCurrentUser());
        inventoryTransactionRepository.save(transaction);
    }

    @Override
    public InventoryDTO getInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        return mapToDTO(inventory);
    }

    @Override
    public List<InventoryDTO> getLowStockItems(Integer threshold) {
        List<Inventory> lowStockItems = inventoryRepository.findByQuantityLessThanEqual(threshold);
        return lowStockItems.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public Page<InventoryTransactionDTO> getTransactionHistory(Long inventoryId, Pageable pageable) {
        Page<InventoryTransaction> transactions = inventoryTransactionRepository.findByInventoryId(inventoryId, pageable);
        return transactions.map(this::mapToTransactionDTO);
    }

    private InventoryDTO mapToDTO(Inventory inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setId(inventory.getId());
        if (inventory.getProduct() != null) {
            dto.setProductId(inventory.getProduct().getId());
        }
        dto.setAvailableStock(inventory.getQuantity());
        dto.setLastUpdatedAt(inventory.getUpdatedAt());
        return dto;
    }

    private InventoryTransactionDTO mapToTransactionDTO(InventoryTransaction transaction) {
        InventoryTransactionDTO dto = new InventoryTransactionDTO();
        dto.setId(transaction.getId());
        if (transaction.getInventory() != null) {
            dto.setInventoryId(transaction.getInventory().getId());
        }
        dto.setTransactionType(transaction.getTransactionType());
        dto.setQuantity(transaction.getQuantity());
        dto.setReferenceType(transaction.getReferenceType());
        dto.setReferenceId(transaction.getReferenceId());
        dto.setNotes(transaction.getNotes());
        if (transaction.getPerformedBy() != null) {
            dto.setPerformedById(transaction.getPerformedBy().getId());
        }
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }
}
