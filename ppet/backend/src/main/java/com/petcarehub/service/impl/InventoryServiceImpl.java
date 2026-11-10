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
import com.petcarehub.security.Ownership;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;
    private final Ownership ownership;

    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository, 
                                InventoryTransactionRepository inventoryTransactionRepository,
                                UserRepository userRepository, Ownership ownership) {
        this.ownership = ownership;
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        return userRepository.findById(ownership.userId())
                .orElseThrow(() -> new InsufficientAuthenticationException("Bạn cần đăng nhập"));
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
        requirePositive(quantity);
        changeStock(productId, quantity, TransactionType.STOCK_OUT, "Xuất kho", getCurrentUser(), null);
    }

    @Override
    @Transactional
    public void addStock(StockUpdateRequest request) {
        requirePositive(request.getQuantity());
        changeStock(request.getProductId(), request.getQuantity(), TransactionType.STOCK_IN,
                request.getReason(), getCurrentUser(), null);
    }

    @Override
    @Transactional
    public void reserveForOrder(Long productId, int quantity, long orderId) {
        changeStock(productId, quantity, TransactionType.STOCK_OUT, "Đặt hàng", null, orderId);
    }

    @Override
    @Transactional
    public void restoreForOrder(Long productId, int quantity, long orderId) {
        changeStock(productId, quantity, TransactionType.STOCK_IN, "Hủy đơn", null, orderId);
    }

    private void changeStock(Long productId, int quantity, TransactionType type, String notes, User actor, Long orderId) {
        requirePositive(quantity);
        Inventory inventory = inventoryRepository.findLockedByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tồn kho"));
        long updated = (long) inventory.getQuantity() + (type == TransactionType.STOCK_IN ? quantity : -quantity);
        if (updated < 0) throw new IllegalStateException("Không đủ tồn kho cho sản phẩm " + productId);
        if (updated > Integer.MAX_VALUE) throw new BadRequestException("Số lượng tồn kho vượt giới hạn");
        inventory.setQuantity((int) updated);
        inventoryRepository.save(inventory);
        inventoryTransactionRepository.save(InventoryTransaction.builder()
                .inventory(inventory).quantity(quantity).transactionType(type).notes(notes)
                .source(orderId == null ? "USER" : "SYSTEM").performedBy(actor)
                .referenceType(orderId == null ? null : "ORDER").referenceId(orderId).build());
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

    private void requirePositive(Integer quantity) {
        if (quantity == null || quantity <= 0) throw new BadRequestException("quantity: Số lượng phải lớn hơn 0");
    }

    @Override
    @Transactional
    public void adjustStock(Long productId, Integer quantity, String reason) {
        if (quantity == null || quantity < 0 || reason == null || reason.isBlank()) {
            throw new BadRequestException("Số lượng kiểm kê không được âm và phải có lý do");
        }
        Inventory inventory = inventoryRepository.findLockedByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tồn kho"));
        User actor = getCurrentUser();
        int delta = quantity - inventory.getQuantity();
        inventory.setQuantity(quantity);
        inventoryRepository.save(inventory);
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventory(inventory);
        transaction.setTransactionType(TransactionType.ADJUSTMENT);
        transaction.setQuantity(delta); // Signed difference for stocktaking; stock in/out remain positive.
        transaction.setNotes(reason);
        transaction.setPerformedBy(actor);
        transaction.setSource("USER");
        inventoryTransactionRepository.save(transaction);
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
        dto.setSource(transaction.getSource());
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
