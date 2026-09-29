package com.petcarehub.dto.pet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetDTO {
    private Long id;
    private String name;
    private Long speciesId;
    private String speciesName;
    private Long breedId;
    private String breedName;
    private Integer age;
    private String healthStatus;
    private String availabilityStatus;
    private String description;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
