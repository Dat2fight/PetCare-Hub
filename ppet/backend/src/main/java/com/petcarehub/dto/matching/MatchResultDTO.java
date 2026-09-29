package com.petcarehub.dto.matching;

import com.petcarehub.dto.pet.PetDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultDTO {
    private PetDTO pet;
    private double score;
    private String explanation;
}

