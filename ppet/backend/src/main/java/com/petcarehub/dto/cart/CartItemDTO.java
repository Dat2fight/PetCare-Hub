package com.petcarehub.dto.cart;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Long id;
    private Long productId;
    private Long petId;
    private String petName;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}
