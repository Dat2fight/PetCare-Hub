package com.petcarehub.dto.health;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VaccinationCreateRequest {
    private Long medicalRecordId;
    private String name;
    private LocalDate dateAdministered;
    private LocalDate nextDueDate;
}
