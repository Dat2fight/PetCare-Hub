package com.petcarehub.service;

import com.petcarehub.dto.pet.BreedDTO;
import com.petcarehub.dto.pet.SpeciesDTO;

import java.util.List;

public interface SpeciesService {
    List<SpeciesDTO> getAllSpecies();
    List<BreedDTO> getBreedsBySpeciesId(Long speciesId);
}
