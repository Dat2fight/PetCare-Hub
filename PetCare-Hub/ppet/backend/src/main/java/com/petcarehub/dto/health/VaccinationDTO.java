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
public class VaccinationDTO {
    private Long id;
    private Long petId;
    private Long veterinarianId;
    private String veterinarianName;
    private String vaccineName;
    private String vaccineBatchNumber;
    private LocalDate dateAdministered;
    private LocalDate nextDueDate;
    private String notes;
    private LocalDateTime createdAt;
}
