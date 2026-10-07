package com.petcarehub.dto.appointment;

import com.petcarehub.enums.AppointmentStatus;
import lombok.Data;

@Data
public class AppointmentUpdateRequest {
    private AppointmentStatus status;
    private String notes;
}
