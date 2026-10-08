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
}
