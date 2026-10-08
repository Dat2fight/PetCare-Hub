package com.petcarehub.dto.inventory;

import lombok.Data;

@Data
public class StockUpdateRequest {
    private Long productId;
    private Integer quantity;
    private String reason;
}
