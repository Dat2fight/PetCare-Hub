package com.petcarehub.dto.order;

import lombok.Data;

@Data
public class OrderItemDTO {
    @jakarta.validation.constraints.NotNull(message="Sản phẩm là bắt buộc")
    private Long productId;
    @jakarta.validation.constraints.NotNull(message="Số lượng là bắt buộc")
    @jakarta.validation.constraints.Positive(message="Số lượng phải lớn hơn 0")
    private Integer quantity;
    private Double price;
}
