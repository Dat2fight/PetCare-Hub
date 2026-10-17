package com.petcarehub.enums;

public enum AppointmentStatus {
    SCHEDULED,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    public boolean canTransitionTo(AppointmentStatus target) {
        if (target == null) return false;
        return switch (this) {
            case SCHEDULED -> java.util.Set.of(CONFIRMED, IN_PROGRESS, CANCELLED, NO_SHOW).contains(target);
            case CONFIRMED -> java.util.Set.of(IN_PROGRESS, CANCELLED, NO_SHOW).contains(target);
            case IN_PROGRESS -> java.util.Set.of(COMPLETED, CANCELLED).contains(target);
            default -> false;
        };
    }
}
