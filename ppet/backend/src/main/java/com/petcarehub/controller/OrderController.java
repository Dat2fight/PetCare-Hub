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

    @GetMapping
    public org.springframework.data.domain.Page<OrderDTO> mine(org.springframework.data.domain.Pageable pageable) {
        return orderService.getMyOrders(pageable);
    }

    @PostMapping("/quote")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER') and !hasAnyRole('ADMIN', 'SALES_STAFF', 'WAREHOUSE_STAFF', 'VETERINARIAN')")
    public com.petcarehub.dto.order.OrderQuote quote(@jakarta.validation.Valid @RequestBody com.petcarehub.dto.order.QuoteRequest request) {
        return orderService.quote(request);
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER') and !hasAnyRole('ADMIN', 'SALES_STAFF', 'WAREHOUSE_STAFF', 'VETERINARIAN')")
    public ResponseEntity<OrderDTO> createOrder(@RequestHeader(value = "Idempotency-Key", required = false) String key,
                                               @jakarta.validation.Valid @RequestBody OrderCreateRequest request) {
        try {
            var uuid = java.util.UUID.fromString(key == null ? "" : key);
            if (!uuid.toString().equalsIgnoreCase(key)) throw new IllegalArgumentException();
            request.setCheckoutKey(uuid);
        } catch (IllegalArgumentException invalid) {
            throw new com.petcarehub.exception.BadRequestException("Idempotency-Key phải là UUID hợp lệ");
        }
        var order = orderService.createOrder(request);
        var response = ResponseEntity.ok();
        if (order.isReplay()) response.header("Idempotent-Replay", "true");
        return response.body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
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
