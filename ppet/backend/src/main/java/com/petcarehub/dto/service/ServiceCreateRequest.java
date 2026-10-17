package com.petcarehub.dto.service;

import com.petcarehub.enums.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCreateRequest {

    @NotBlank(message = "Service name is required")
    private String name;

    private String description;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Duration is required")
    @Min(value = 5, message = "Thời lượng tối thiểu 5 phút")
    @jakarta.validation.constraints.Max(value = 480, message = "Thời lượng tối đa 480 phút")
    private Integer durationMinutes;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    private Boolean active;
}
