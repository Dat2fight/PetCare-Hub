package com.petcarehub.dto.order;

import lombok.Data;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private Long userId;
    private String status;
    private Double totalPrice;
    private List<OrderItemDTO> items;
    private String orderNumber;
    private String shippingAddress;
    private java.time.LocalDateTime expiresAt;
    private java.time.LocalDateTime expiredAt;
    private String notes;
    private String paymentMethod;
    private String paymentStatus;
    private java.time.LocalDateTime createdAt;
}
