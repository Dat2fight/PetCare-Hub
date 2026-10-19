package com.petcarehub.dto.pet;

import java.math.BigDecimal;
import java.time.LocalDate;

// Explicit storefront contract: no owner identity or clinical status.
public record PublicPetDTO(Long id, String name, Long speciesId, String speciesName,
        Long breedId, String breedName, Integer age, LocalDate birthDate, BigDecimal price,
        String availabilityStatus, String description, String imageUrl) {
    public static PublicPetDTO from(PetDTO pet) {
        // Sold detail is a status-only tombstone: never expose the new owner's private pet profile.
        if ("SOLD".equals(pet.getAvailabilityStatus()))
            return new PublicPetDTO(pet.getId(), "Thú cưng đã bán", null, null, null, null, null, null, null, "SOLD", null, null);
        if (pet.getOwnerId() != null || !("AVAILABLE".equals(pet.getAvailabilityStatus()) || "PENDING".equals(pet.getAvailabilityStatus())))
            throw new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy thú cưng đang bán");
        return new PublicPetDTO(pet.getId(), pet.getName(), pet.getSpeciesId(), pet.getSpeciesName(),
                pet.getBreedId(), pet.getBreedName(), pet.getAge(), pet.getBirthDate(), pet.getPrice(),
                pet.getAvailabilityStatus(), pet.getDescription(), pet.getImageUrl());
    }
}
