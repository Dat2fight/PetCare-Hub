package com.petcarehub.mapper;

import com.petcarehub.dto.pet.PetDTO;
import com.petcarehub.entity.Pet;
import com.petcarehub.enums.AvailabilityStatus;
import java.time.LocalDate;
import java.time.Period;

public final class PetMapper {
    private PetMapper() {}
    public static PetDTO toDTO(Pet pet) {
        PetDTO dto = new PetDTO();
        dto.setId(pet.getId()); dto.setName(pet.getName());
        if (pet.getSpecies() != null) { dto.setSpeciesId(pet.getSpecies().getId()); dto.setSpeciesName(pet.getSpecies().getName()); }
        if (pet.getBreed() != null) { dto.setBreedId(pet.getBreed().getId()); dto.setBreedName(pet.getBreed().getName()); }
        dto.setBirthDate(pet.getDateOfBirth());
        dto.setAge(pet.getDateOfBirth() == null ? null : Period.between(pet.getDateOfBirth(), LocalDate.now()).getYears());
        dto.setHealthStatus(pet.getHealthStatus() == null ? null : pet.getHealthStatus().name());
        dto.setAvailabilityStatus(pet.getAvailabilityStatus() == null ? null : pet.getAvailabilityStatus().name());
        dto.setOwnerId(pet.getOwner() == null ? null : pet.getOwner().getId());
        // NEEDS DECISION: prices are public only for unowned pets offered for sale.
        dto.setPrice(pet.getOwner() == null && (pet.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE || pet.getAvailabilityStatus() == AvailabilityStatus.PENDING) ? pet.getPrice() : null);
        dto.setDescription(pet.getDescription()); dto.setImageUrl(pet.getImageUrl());
        dto.setCreatedAt(pet.getCreatedAt()); dto.setUpdatedAt(pet.getUpdatedAt());
        return dto;
    }
}
