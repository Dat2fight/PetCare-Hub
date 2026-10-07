package com.petcarehub.dto.health;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VaccinationCreateRequest {
    private Long petId;
    private Long veterinarianId;
    private String vaccineName;
    private String vaccineBatchNumber;
    private LocalDate dateAdministered;
    private LocalDate nextDueDate;
    private String notes;
}
