package com.petcarehub.dto.pet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BreedDTO {
    private Long id;
    private String name;
    private Long speciesId;
}
