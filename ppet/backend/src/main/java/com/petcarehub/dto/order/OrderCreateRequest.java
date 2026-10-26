package com.petcarehub.dto.order;

import lombok.Data;
import java.util.List;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreateRequest {
    private Long userId;
    @jakarta.validation.Valid
    @jakarta.validation.constraints.NotEmpty(message="Đơn hàng phải có sản phẩm")
    private List<OrderItemDTO> items;
    @jakarta.validation.constraints.NotBlank(message="Địa chỉ giao hàng là bắt buộc")
    @jakarta.validation.constraints.Size(max=2000, message="Địa chỉ tối đa 2000 ký tự")
    private String shippingAddress;
    // Only COD is supported until a real payment provider is integrated; omitted means legacy COD.
    @jakarta.validation.constraints.Pattern(regexp="COD", message="Chỉ hỗ trợ thanh toán COD")
    private String paymentMethod;
}
