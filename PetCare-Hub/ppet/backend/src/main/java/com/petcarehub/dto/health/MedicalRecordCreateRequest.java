package com.petcarehub.dto.health;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MedicalRecordCreateRequest {
    private Long petId;
    private Long veterinarianId;
    private Long appointmentId;
    private LocalDate examinationDate;
    private String findings;
    private String diagnosis;
    private String recommendations;
}
