package com.petcarehub.state.order;

import com.petcarehub.entity.Order;
import com.petcarehub.enums.OrderStatus;
import com.petcarehub.exception.InvalidStateTransitionException;

public class ShippingState implements OrderState {
    @Override
    public OrderState confirm(Order order) {
        throw new InvalidStateTransitionException("Order is shipping, cannot confirm");
    }

    @Override
    public OrderState process(Order order) {
        throw new InvalidStateTransitionException("Order is shipping, cannot process");
    }

    @Override
    public OrderState ship(Order order) {
        throw new InvalidStateTransitionException("Order is already shipping");
    }

    @Override
    public OrderState deliver(Order order) {
        order.setStatus(OrderStatus.DELIVERED);
        return new DeliveredState();
    }

    @Override
    public OrderState cancel(Order order) {
        throw new InvalidStateTransitionException("Cannot cancel an order that is shipping");
    }
}


