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
}
