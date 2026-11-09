package com.petcarehub.dto.order;

import lombok.Data;
import java.util.List;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(value = {"userId", "customerId", "ownerId", "price", "totalAmount", "availability"}, ignoreUnknown = true)
public class OrderCreateRequest extends QuoteRequest {
    @jakarta.validation.constraints.NotBlank(message="Địa chỉ giao hàng là bắt buộc")
    @jakarta.validation.constraints.Size(max=2000, message="Địa chỉ tối đa 2000 ký tự")
    private String shippingAddress;
    // Only COD is supported until a real payment provider is integrated; omitted means legacy COD.
    @jakarta.validation.constraints.Pattern(regexp="COD", message="Chỉ hỗ trợ thanh toán COD")
    private String paymentMethod;
    @jakarta.validation.constraints.Size(max=2000, message="Ghi chú tối đa 2000 ký tự")
    private String notes;
    private java.util.UUID checkoutKey;
}
