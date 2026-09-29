package com.petcarehub.dto.health;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MedicalRecordDTO {
    private Long id;
    private Long petId;
    private Long vetId;
    private LocalDate visitDate;
    private String diagnosis;
    private String notes;
}
