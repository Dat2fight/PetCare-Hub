package com.petcarehub.dto.appointment;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentCreateRequest {
    @NotNull
    private Long petId;
    @NotNull
    private Long serviceId;
    
    private Long staffId; // Adding staffId as it's required by entity
    private Long customerId; // Adding customerId as it's required by entity
    
    @NotNull
    private LocalDateTime appointmentTime;
    private String notes;
}
