package com.petcarehub.dto.health;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VaccinationDTO {
    private Long id;
    private Long medicalRecordId;
    private String name;
    private LocalDate dateAdministered;
    private LocalDate nextDueDate;
}
