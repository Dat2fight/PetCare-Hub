package com.petcarehub.dto.health;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class MedicalRecordCreateRequest {
    @NotNull(message="Thú cưng là bắt buộc")
    private Long petId;
    @NotNull(message="Bác sĩ là bắt buộc")
    private Long veterinarianId;
    private Long appointmentId;
    @NotNull(message="Ngày khám là bắt buộc") @PastOrPresent(message="Ngày khám không được trong tương lai")
    private LocalDate examinationDate;
    @NotBlank(message="Kết quả khám là bắt buộc")
    private String findings;
    private String diagnosis;
    private String recommendations;
}
