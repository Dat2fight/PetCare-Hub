package com.petcarehub.dto.promotion;

import java.math.BigDecimal;
import com.petcarehub.enums.DiscountType;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCreateRequest {
    @NotBlank @Size(max = 50)
    private String code;
    private String description;
    @NotNull
    private DiscountType discountType;
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2)
    private BigDecimal discountValue;
    @DecimalMin("0") @Digits(integer = 8, fraction = 2)
    private BigDecimal minOrderAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean active = true;
    @Positive
    private Integer maxUses;
}
