package com.petcarehub.exception;

public class CheckoutKeyConflictException extends RuntimeException {
    public CheckoutKeyConflictException() {
        super("Mã yêu cầu đã được dùng cho đơn có nội dung khác");
    }
}
