package com.petcarehub.state.order;

import com.petcarehub.entity.Order;
import com.petcarehub.exception.InvalidStateTransitionException;

public class DeliveredState implements OrderState {
    @Override
    public OrderState confirm(Order order) {
        throw new InvalidStateTransitionException("Order is delivered, cannot confirm");
    }

    @Override
    public OrderState process(Order order) {
        throw new InvalidStateTransitionException("Order is delivered, cannot process");
    }

    @Override
    public OrderState ship(Order order) {
        throw new InvalidStateTransitionException("Order is delivered, cannot ship");
    }

    @Override
    public OrderState deliver(Order order) {
        throw new InvalidStateTransitionException("Order is already delivered");
    }

    @Override
    public OrderState cancel(Order order) {
        throw new InvalidStateTransitionException("Cannot cancel a delivered order");
    }
}

