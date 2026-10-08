package com.petcarehub.dto.inventory;

import com.petcarehub.enums.TransactionType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InventoryTransactionDTO {
    private Long id;
    private Long inventoryId;
    private TransactionType transactionType;
    private Integer quantity;
    private String referenceType;
    private Long referenceId;
    private String notes;
    private Long performedById;
    private LocalDateTime createdAt;
}
