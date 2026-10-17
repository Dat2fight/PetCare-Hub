package com.petcarehub.dto.health;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class VaccinationCreateRequest {
    @NotNull(message="Thú cưng là bắt buộc")
    private Long petId;
    @NotNull(message="Bác sĩ là bắt buộc")
    private Long veterinarianId;
    @NotBlank(message="Tên vaccine là bắt buộc") @Size(max=100, message="Tên vaccine tối đa 100 ký tự")
    private String vaccineName;
    @Size(max=100, message="Mã lô tối đa 100 ký tự")
    private String vaccineBatchNumber;
    @NotNull(message="Ngày tiêm là bắt buộc") @PastOrPresent(message="Ngày tiêm không được trong tương lai")
    private LocalDate dateAdministered;
    private LocalDate nextDueDate;
    private String notes;
}
