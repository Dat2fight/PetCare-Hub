package com.petcarehub.state.order;

import com.petcarehub.entity.Order;

public interface OrderState {
    OrderState confirm(Order order);
    OrderState process(Order order);
    OrderState ship(Order order);
    OrderState deliver(Order order);
    OrderState cancel(Order order);
}

