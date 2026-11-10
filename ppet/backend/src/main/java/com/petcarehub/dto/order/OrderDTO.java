package com.petcarehub.dto.order;

import lombok.Data;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private Long userId;
    private String status;
    private Double totalPrice;
    private java.math.BigDecimal subtotal;
    private String promotionCode;
    private java.math.BigDecimal discountAmount;
    private Integer pointsUsed;
    private java.math.BigDecimal pointsDiscount;
    private java.math.BigDecimal shippingFee;
    private List<OrderItemDTO> items;
    private String orderNumber;
    private String checkoutKey;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private boolean replay;
    private String shippingAddress;
    private com.petcarehub.entity.ShippingInfo shipping;
    private java.time.LocalDateTime expiresAt;
    private java.time.LocalDateTime expiredAt;
    private String notes;
    private String paymentMethod;
    private String paymentStatus;
    private java.time.LocalDateTime createdAt;
}
