package com.petcarehub.service;

import com.petcarehub.dto.order.OrderCreateRequest;
import com.petcarehub.dto.order.OrderDTO;

public interface OrderService {
    OrderDTO createOrder(OrderCreateRequest request);
    OrderDTO confirmOrder(Long orderId);
    OrderDTO processOrder(Long orderId);
    OrderDTO shipOrder(Long orderId);
    OrderDTO deliverOrder(Long orderId);
    OrderDTO cancelOrder(Long orderId);
}
