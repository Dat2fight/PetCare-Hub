package com.petcarehub.state.order;

import com.petcarehub.entity.Order;
import com.petcarehub.exception.InvalidStateTransitionException;

public class CancelledState implements OrderState {
    @Override
    public OrderState confirm(Order order) {
        throw new InvalidStateTransitionException("Order is cancelled");
    }

    @Override
    public OrderState process(Order order) {
        throw new InvalidStateTransitionException("Order is cancelled");
    }

    @Override
    public OrderState ship(Order order) {
        throw new InvalidStateTransitionException("Order is cancelled");
    }

    @Override
    public OrderState deliver(Order order) {
        throw new InvalidStateTransitionException("Order is cancelled");
    }

    @Override
    public OrderState cancel(Order order) {
        throw new InvalidStateTransitionException("Order is already cancelled");
    }
}

