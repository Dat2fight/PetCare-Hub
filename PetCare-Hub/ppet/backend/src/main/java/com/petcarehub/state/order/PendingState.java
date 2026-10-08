package com.petcarehub.state.order;

import com.petcarehub.entity.Order;
import com.petcarehub.enums.OrderStatus;
import com.petcarehub.exception.InvalidStateTransitionException;

public class PendingState implements OrderState {
    @Override
    public OrderState confirm(Order order) {
        order.setStatus(OrderStatus.CONFIRMED);
        return new ConfirmedState();
    }

    @Override
    public OrderState process(Order order) {
        throw new InvalidStateTransitionException("Cannot process a pending order");
    }

    @Override
    public OrderState ship(Order order) {
        throw new InvalidStateTransitionException("Cannot ship a pending order");
    }

    @Override
    public OrderState deliver(Order order) {
        throw new InvalidStateTransitionException("Cannot deliver a pending order");
    }

    @Override
    public OrderState cancel(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        return new CancelledState();
    }
}


