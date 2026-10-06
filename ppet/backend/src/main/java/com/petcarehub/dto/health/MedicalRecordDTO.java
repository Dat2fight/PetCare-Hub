package com.petcarehub.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordDTO {
    private Long id;
    private Long petId;
    private Long veterinarianId;
    private String veterinarianName;
    private Long appointmentId;
    private LocalDate examinationDate;
    private String findings;
    private String diagnosis;
    private String recommendations;
    private LocalDateTime createdAt;
}
