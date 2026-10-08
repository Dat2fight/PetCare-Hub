package com.petcarehub.dto.health;

import lombok.Data;

@Data
public class TreatmentDTO {
    private Long id;
    private Long medicalRecordId;
    private String description;
    private String medicine;
    private String dosage;
}
