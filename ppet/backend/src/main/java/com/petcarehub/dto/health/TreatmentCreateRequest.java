package com.petcarehub.dto.health;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TreatmentCreateRequest {
    @NotNull(message="Hồ sơ y tế là bắt buộc")
    private Long medicalRecordId;
    private String description;
    @NotBlank(message="Thuốc là bắt buộc") @Size(max=100, message="Tên thuốc tối đa 100 ký tự")
    private String medicine;
    @NotBlank(message="Liều lượng là bắt buộc") @Size(max=100, message="Liều lượng tối đa 100 ký tự")
    private String dosage;
    @NotBlank(message="Tần suất dùng thuốc là bắt buộc") @Size(max=100, message="Tần suất tối đa 100 ký tự")
    private String frequency;
    @NotNull(message="Ngày bắt đầu là bắt buộc")
    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;
}
