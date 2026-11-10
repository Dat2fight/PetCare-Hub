package com.petcarehub.dto.appointment;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AppointmentDTO {
    private Long id;
    private Long petId;
    private Long serviceId;
    private Long staffId;
    private Long customerId;
    private LocalDateTime appointmentTime;
    private String status;
    private String notes;
    private String code;
    private String serviceName;
    private String petName;
    private String staffName;
    private java.math.BigDecimal price;
    private String timezone;
    private java.time.Instant startsAt;
    private java.time.Instant endsAt;
    private LocalDateTime cancelUntil;
    private boolean cancellable;
}
