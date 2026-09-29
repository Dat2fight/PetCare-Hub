package com.petcarehub.dto.order;

import lombok.Data;
import java.util.List;

@Data
public class OrderCreateRequest {
    private Long userId;
    private List<OrderItemDTO> items;
}
