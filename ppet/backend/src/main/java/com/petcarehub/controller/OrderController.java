package com.petcarehub.controller;

import com.petcarehub.dto.order.OrderCreateRequest;
import com.petcarehub.dto.order.OrderDTO;
import com.petcarehub.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final com.petcarehub.repository.OrderRepository orders;
    private final com.petcarehub.security.Ownership ownership;

    @GetMapping
    public org.springframework.data.domain.Page<OrderDTO> mine(org.springframework.data.domain.Pageable pageable) {
        return orders.findByCustomerId(ownership.userId(), pageable).map(order -> {
            OrderDTO dto = new OrderDTO(); dto.setId(order.getId()); dto.setUserId(order.getCustomer().getId());
            dto.setStatus(order.getStatus().name()); dto.setTotalPrice(order.getTotalAmount().doubleValue()); return dto;
        });
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@jakarta.validation.Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderDTO> confirmOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<OrderDTO> processOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.processOrder(id));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<OrderDTO> shipOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.shipOrder(id));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<OrderDTO> deliverOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.deliverOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
