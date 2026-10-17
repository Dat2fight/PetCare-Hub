package com.petcarehub.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
    @Pattern(regexp="(?s).*\\S.*", message="Tên sản phẩm không được trống") @Size(max=255, message="Tên tối đa 255 ký tự")
    private String name;
    private String description;
    @DecimalMin(value="0.0", message="Giá không được âm")
    private BigDecimal price;
    @Null(message="Dùng API nhập, xuất hoặc kiểm kê để thay đổi tồn kho")
    private Integer stockQuantity;
    private Long categoryId;
    private Boolean active;
    private String sku;
    private String imageUrl;
}
