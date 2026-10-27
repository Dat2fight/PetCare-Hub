package com.petcarehub.service;

import com.petcarehub.dto.order.OrderCreateRequest;
import com.petcarehub.dto.order.OrderDTO;

public interface OrderService {
    org.springframework.data.domain.Page<OrderDTO> getMyOrders(org.springframework.data.domain.Pageable pageable);
    OrderDTO createOrder(OrderCreateRequest request);
    OrderDTO getOrderById(Long orderId);
    OrderDTO confirmOrder(Long orderId);
    OrderDTO processOrder(Long orderId);
    OrderDTO shipOrder(Long orderId);
    OrderDTO deliverOrder(Long orderId);
    OrderDTO cancelOrder(Long orderId);
}
