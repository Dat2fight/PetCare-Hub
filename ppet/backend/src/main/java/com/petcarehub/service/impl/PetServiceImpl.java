package com.petcarehub.service.impl;

import com.petcarehub.dto.pet.PetCreateRequest;
import com.petcarehub.dto.pet.PetDTO;
import com.petcarehub.dto.pet.PetUpdateRequest;
import com.petcarehub.entity.Breed;
import com.petcarehub.entity.Pet;
import com.petcarehub.entity.Species;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.BreedRepository;
import com.petcarehub.repository.PetRepository;
import com.petcarehub.repository.SpeciesRepository;
import com.petcarehub.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final com.petcarehub.repository.UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PetDTO> getPets(String healthStatus, String availabilityStatus, Long speciesId, Long breedId, Pageable pageable) {
        Specification<Pet> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (healthStatus != null && !healthStatus.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("healthStatus"), healthStatus));
            }
            if (availabilityStatus != null && !availabilityStatus.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("availabilityStatus"), availabilityStatus));
            }
            if (speciesId != null) {
                predicates.add(cb.equal(root.get("species").get("id"), speciesId));
            }
            if (breedId != null) {
                predicates.add(cb.equal(root.get("breed").get("id"), breedId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return petRepository.findAll(spec, pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PetDTO getPetById(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id: " + id));
        return mapToDTO(pet);
    }

    @Override
    @Transactional
    public PetDTO createPet(PetCreateRequest request) {
        Species species = speciesRepository.findById(request.getSpeciesId())
                .orElseThrow(() -> new ResourceNotFoundException("Species not found with id: " + request.getSpeciesId()));
        
        Breed breed = null;
        if (request.getBreedId() != null) {
            breed = breedRepository.findById(request.getBreedId())
                    .orElseThrow(() -> new ResourceNotFoundException("Breed not found with id: " + request.getBreedId()));
        }

        Pet pet = new Pet();
        pet.setName(request.getName());
        pet.setSpecies(species);
        pet.setBreed(breed);
        // no age field in Pet entity
        pet.setHealthStatus(com.petcarehub.enums.HealthStatus.valueOf(request.getHealthStatus()));
        pet.setAvailabilityStatus(com.petcarehub.enums.AvailabilityStatus.valueOf(request.getAvailabilityStatus()));
        pet.setDescription(request.getDescription());
        pet.setImageUrl(request.getImageUrl());
        pet.setPrice(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO);
        if (request.getOwnerId() != null) pet.setOwner(userRepository.findById(request.getOwnerId()).orElse(null));
        pet.setCreatedAt(LocalDateTime.now());
        pet.setUpdatedAt(LocalDateTime.now());

        Pet savedPet = petRepository.save(pet);
        return mapToDTO(savedPet);
    }

    @Override
    @Transactional
    public PetDTO updatePet(Long id, PetUpdateRequest request) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id: " + id));

        Species species = speciesRepository.findById(request.getSpeciesId())
                .orElseThrow(() -> new ResourceNotFoundException("Species not found with id: " + request.getSpeciesId()));

        Breed breed = null;
        if (request.getBreedId() != null) {
            breed = breedRepository.findById(request.getBreedId())
                    .orElseThrow(() -> new ResourceNotFoundException("Breed not found with id: " + request.getBreedId()));
        }

        pet.setName(request.getName());
        pet.setSpecies(species);
        pet.setBreed(breed);
        // no age field in Pet entity
        pet.setHealthStatus(com.petcarehub.enums.HealthStatus.valueOf(request.getHealthStatus()));
        pet.setAvailabilityStatus(com.petcarehub.enums.AvailabilityStatus.valueOf(request.getAvailabilityStatus()));
        pet.setDescription(request.getDescription());
        pet.setImageUrl(request.getImageUrl());
        pet.setPrice(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO);
        if (request.getOwnerId() != null) pet.setOwner(userRepository.findById(request.getOwnerId()).orElse(null));
        pet.setUpdatedAt(LocalDateTime.now());

        Pet updatedPet = petRepository.save(pet);
        return mapToDTO(updatedPet);
    }

    @Override
    @Transactional
    public void deletePet(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id: " + id));
        petRepository.delete(pet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetDTO> getPetsByOwner(Long ownerId) {
        return petRepository.findByOwnerId(ownerId).stream()
                .map(this::mapToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    private PetDTO mapToDTO(Pet pet) {
        return new PetDTO(
                pet.getId(),
                pet.getName(),
                pet.getSpecies() != null ? pet.getSpecies().getId() : null,
                pet.getSpecies() != null ? pet.getSpecies().getName() : null,
                pet.getBreed() != null ? pet.getBreed().getId() : null,
                pet.getBreed() != null ? pet.getBreed().getName() : null,
                null,
                pet.getHealthStatus().name(),
                pet.getAvailabilityStatus().name(),
                pet.getOwner() != null ? pet.getOwner().getId() : null,
                pet.getDescription(),
                pet.getImageUrl(),
                pet.getCreatedAt(),
                pet.getUpdatedAt()
        );
    }
}


