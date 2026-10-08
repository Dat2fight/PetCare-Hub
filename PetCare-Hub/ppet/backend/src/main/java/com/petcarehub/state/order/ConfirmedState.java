package com.petcarehub.state.order;

import com.petcarehub.entity.Order;
import com.petcarehub.enums.OrderStatus;
import com.petcarehub.exception.InvalidStateTransitionException;

public class ConfirmedState implements OrderState {
    @Override
    public OrderState confirm(Order order) {
        throw new InvalidStateTransitionException("Order is already confirmed");
    }

    @Override
    public OrderState process(Order order) {
        order.setStatus(OrderStatus.PROCESSING);
        return new ProcessingState();
    }

    @Override
    public OrderState ship(Order order) {
        throw new InvalidStateTransitionException("Cannot ship a confirmed order before processing");
    }

    @Override
    public OrderState deliver(Order order) {
        throw new InvalidStateTransitionException("Cannot deliver a confirmed order");
    }

    @Override
    public OrderState cancel(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        return new CancelledState();
    }
}


