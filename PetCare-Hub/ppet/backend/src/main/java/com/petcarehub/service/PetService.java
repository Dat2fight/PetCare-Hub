package com.petcarehub.service;

import com.petcarehub.dto.pet.PetCreateRequest;
import com.petcarehub.dto.pet.PetDTO;
import com.petcarehub.dto.pet.PetUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PetService {
    Page<PetDTO> getPets(String healthStatus, String availabilityStatus, Long speciesId, Long breedId, Pageable pageable);
    PetDTO getPetById(Long id);
    java.util.List<PetDTO> getPetsByOwner(Long ownerId);
    PetDTO createPet(PetCreateRequest request);
    PetDTO updatePet(Long id, PetUpdateRequest request);
    void deletePet(Long id);
}
