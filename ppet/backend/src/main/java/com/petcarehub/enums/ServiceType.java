package com.petcarehub.enums;

public enum ServiceType {
    GROOMING,
    VETERINARY;

    public RoleType staffRole() {
        // NEEDS DECISION: no specialty model; grooming uses SALES_STAFF.
        return this == VETERINARY ? RoleType.VETERINARIAN : RoleType.SALES_STAFF;
    }
}
