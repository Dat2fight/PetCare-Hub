package com.petcarehub.dto.cart;

import lombok.Data;

@Data
public class AddToCartRequest {
    private Long productId;
    private Long petId;
    private Integer quantity;
}
