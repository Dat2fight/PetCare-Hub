package com.petcarehub.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.petcarehub.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceEntityDTO {
    
    private Long id;
    private String name;
    private String description;
    private ServiceType serviceType;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean active;
    private LocalDateTime createdAt;
}
