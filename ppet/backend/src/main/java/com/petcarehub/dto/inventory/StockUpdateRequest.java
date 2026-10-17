package com.petcarehub.dto.inventory;

import lombok.Data;

@Data
public class StockUpdateRequest {
    @jakarta.validation.constraints.NotNull(message="Sản phẩm là bắt buộc")
    private Long productId;
    @jakarta.validation.constraints.NotNull(message="Số lượng là bắt buộc")
    @jakarta.validation.constraints.Positive(message="Số lượng phải lớn hơn 0")
    private Integer quantity;
    private String reason;
}
