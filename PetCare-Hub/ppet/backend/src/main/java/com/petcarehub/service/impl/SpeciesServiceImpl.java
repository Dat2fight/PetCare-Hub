package com.petcarehub.service.impl;

import com.petcarehub.dto.pet.BreedDTO;
import com.petcarehub.dto.pet.SpeciesDTO;
import com.petcarehub.entity.Breed;
import com.petcarehub.entity.Species;
import com.petcarehub.repository.BreedRepository;
import com.petcarehub.repository.SpeciesRepository;
import com.petcarehub.service.SpeciesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpeciesServiceImpl implements SpeciesService {

    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;

    @Override
    public List<SpeciesDTO> getAllSpecies() {
        return speciesRepository.findAll().stream()
                .map(species -> new SpeciesDTO(species.getId(), species.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<BreedDTO> getBreedsBySpeciesId(Long speciesId) {
        return breedRepository.findBySpeciesId(speciesId).stream()
                .map(breed -> new BreedDTO(breed.getId(), breed.getName(), breed.getSpecies().getId()))
                .collect(Collectors.toList());
    }
}
