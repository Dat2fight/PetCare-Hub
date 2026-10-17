package com.petcarehub.exception;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super("Không thể chuyển trạng thái theo yêu cầu");
    }

    public InvalidStateTransitionException(String currentState, String targetState) {
        super(String.format("Không thể chuyển trạng thái từ %s sang %s", currentState, targetState));
    }
}
