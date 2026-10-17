package com.petcarehub.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {
    @NotBlank(message="Tên sản phẩm không được trống") @Size(max=255, message="Tên tối đa 255 ký tự")
    private String name;
    private String description;
    @NotNull(message="Giá là bắt buộc") @DecimalMin(value="0.0", message="Giá không được âm")
    private BigDecimal price;
    @PositiveOrZero(message="Tồn kho không được âm")
    private Integer stockQuantity;
    @NotNull(message="Danh mục là bắt buộc")
    private Long categoryId;
    private Boolean active;
    private String sku;
    private String imageUrl;
}
