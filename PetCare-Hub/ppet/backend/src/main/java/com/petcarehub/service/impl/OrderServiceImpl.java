package com.petcarehub.service.impl;

import com.petcarehub.dto.order.OrderCreateRequest;
import com.petcarehub.dto.order.OrderDTO;
import com.petcarehub.entity.Order;
import com.petcarehub.enums.OrderStatus;
import com.petcarehub.service.InventoryService;
import com.petcarehub.service.OrderService;
import com.petcarehub.state.order.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @Override
    public OrderDTO createOrder(OrderCreateRequest request) {
        // Implementation for creating order
        return new OrderDTO();
    }

    @Override
    public OrderDTO confirmOrder(Long orderId) {
        Order order = getOrderById(orderId);
        OrderState state = getState(order.getStatus());
        state.confirm(order);
        // deduct stock
        // inventoryService.deductStock(order.getItems());
        // save order
        return mapToDTO(order);
    }

    @Override
    public OrderDTO processOrder(Long orderId) {
        Order order = getOrderById(orderId);
        OrderState state = getState(order.getStatus());
        state.process(order);
        // save order
        return mapToDTO(order);
    }

    @Override
    public OrderDTO shipOrder(Long orderId) {
        Order order = getOrderById(orderId);
        OrderState state = getState(order.getStatus());
        state.ship(order);
        // save order
        return mapToDTO(order);
    }

    @Override
    public OrderDTO deliverOrder(Long orderId) {
        Order order = getOrderById(orderId);
        OrderState state = getState(order.getStatus());
        state.deliver(order);
        // save order
        return mapToDTO(order);
    }

    @Override
    public OrderDTO cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);
        OrderState state = getState(order.getStatus());
        state.cancel(order);
        // save order
        return mapToDTO(order);
    }

    private OrderState getState(OrderStatus status) {
        if (status == null) return new PendingState();
        switch (status) {
            case PENDING: return new PendingState();
            case CONFIRMED: return new ConfirmedState();
            case PROCESSING: return new ProcessingState();
            case SHIPPING: return new ShippingState();
            case DELIVERED: return new DeliveredState();
            case CANCELLED: return new CancelledState();
            default: return new PendingState();
        }
    }

    private Order getOrderById(Long orderId) {
        // Fetch order from DB
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    private OrderDTO mapToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setStatus(order.getStatus().name());
        return dto;
    }
}


