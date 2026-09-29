package com.petcarehub.dto.order;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    private Integer quantity;
    private Double price;
}
