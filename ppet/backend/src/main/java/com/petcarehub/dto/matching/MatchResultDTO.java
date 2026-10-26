package com.petcarehub.dto.matching;

import com.petcarehub.dto.pet.PublicPetDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultDTO {
    private PublicPetDTO pet;
    private double score;
    private String explanation;
}

