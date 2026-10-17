package com.petcarehub.dto.health;

import lombok.Data;

@Data
public class TreatmentDTO {
    private Long id;
    private Long medicalRecordId;
    private String description;
    private String medicine;
    private String dosage;
    private String frequency;
    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;
}
