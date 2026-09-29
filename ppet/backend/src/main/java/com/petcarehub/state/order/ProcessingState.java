package com.petcarehub.state.order;

import com.petcarehub.entity.Order;
import com.petcarehub.enums.OrderStatus;
import com.petcarehub.exception.InvalidStateTransitionException;

public class ProcessingState implements OrderState {
    @Override
    public OrderState confirm(Order order) {
        throw new InvalidStateTransitionException("Order is already processing, cannot confirm");
    }

    @Override
    public OrderState process(Order order) {
        throw new InvalidStateTransitionException("Order is already processing");
    }

    @Override
    public OrderState ship(Order order) {
        order.setStatus(OrderStatus.SHIPPING);
        return new ShippingState();
    }

    @Override
    public OrderState deliver(Order order) {
        throw new InvalidStateTransitionException("Cannot deliver an order that is still processing");
    }

    @Override
    public OrderState cancel(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        return new CancelledState();
    }
}


