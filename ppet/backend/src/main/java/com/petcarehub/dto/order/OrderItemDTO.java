package com.petcarehub.dto.order;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    @jakarta.validation.constraints.Null(message="Mua thú cưng chưa khả dụng trên phiên bản này")
    private Long petId;
    @jakarta.validation.constraints.NotNull(message="Số lượng là bắt buộc")
    @jakarta.validation.constraints.Positive(message="Số lượng phải lớn hơn 0")
    private Integer quantity;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private Double price;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String productName;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String petName;
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String imageUrl;

    @jakarta.validation.constraints.AssertTrue(message="Chọn đúng một productId hoặc petId")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isItemReferenceValid() { return (productId == null) != (petId == null); }

    @jakarta.validation.constraints.AssertTrue(message="Số lượng thú cưng phải bằng 1")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isPetQuantityValid() { return petId == null || Integer.valueOf(1).equals(quantity); }
}
