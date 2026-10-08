package com.petcarehub.dto.health;

import lombok.Data;

@Data
public class TreatmentCreateRequest {
    private Long medicalRecordId;
    private String description;
    private String medicine;
    private String dosage;
}
