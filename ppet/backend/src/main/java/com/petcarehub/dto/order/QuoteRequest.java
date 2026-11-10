package com.petcarehub.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteRequest {
    @Valid @NotEmpty(message="Đơn hàng phải có sản phẩm")
    private List<@NotNull(message="Dòng hàng không được trống") OrderItemDTO> items;
    @Size(max=50)
    private String promotionCode;
    @PositiveOrZero
    private int pointsToUse;
}
