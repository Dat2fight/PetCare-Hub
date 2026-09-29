package com.petcarehub.dto.pet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetCreateRequest {
    @NotBlank
    private String name;

    @NotNull
    private Long speciesId;

    private Long breedId;

    private Integer age;

    @NotBlank
    private String healthStatus;

    @NotBlank
    private String availabilityStatus;

    private String description;
    
    private String imageUrl;
}
