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
}
