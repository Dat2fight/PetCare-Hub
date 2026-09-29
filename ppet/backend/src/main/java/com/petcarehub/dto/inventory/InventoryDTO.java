package com.petcarehub.dto.inventory;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InventoryDTO {
    private Long id;
    private Long productId;
    private Integer availableStock;
    private Integer reservedStock;
    private LocalDateTime lastUpdatedAt;
}
