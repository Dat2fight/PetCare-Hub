package com.petcarehub.dto.order;

import lombok.Data;

@Data
public class OrderItemDTO {
    @jakarta.validation.constraints.NotNull(message="Sản phẩm là bắt buộc")
    private Long productId;
    // Pet purchase belongs to the separate feature branch; never silently treat it as a product order.
    @jakarta.validation.constraints.Null(message="Mua thú cưng chưa khả dụng trên phiên bản này")
    private Long petId;
    @jakarta.validation.constraints.NotNull(message="Số lượng là bắt buộc")
    @jakarta.validation.constraints.Positive(message="Số lượng phải lớn hơn 0")
    private Integer quantity;
    private Double price;
    private String productName;
}
