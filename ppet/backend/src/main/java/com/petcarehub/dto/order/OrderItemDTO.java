package com.petcarehub.dto.order;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    private Long petId;
    private Long carePackageId;
    private Long beneficiaryPetId;
    @com.fasterxml.jackson.annotation.JsonProperty(access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String packageName;
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

    @jakarta.validation.constraints.AssertTrue(message="Chọn một sản phẩm, thú cưng hoặc một gói chăm sóc cho thú cưng của bạn")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isItemReferenceValid() { if (carePackageId != null) return beneficiaryPetId != null && productId == null && petId == null && Integer.valueOf(1).equals(quantity);
        return beneficiaryPetId == null && (productId == null) != (petId == null); }

    @jakarta.validation.constraints.AssertTrue(message="Số lượng thú cưng phải bằng 1")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isPetQuantityValid() { return petId == null || Integer.valueOf(1).equals(quantity); }
}
