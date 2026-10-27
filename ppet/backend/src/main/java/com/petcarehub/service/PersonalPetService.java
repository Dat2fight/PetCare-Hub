package com.petcarehub.service;

import com.petcarehub.dto.pet.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.Ownership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

@Service @RequiredArgsConstructor @org.springframework.validation.annotation.Validated
public class PersonalPetService {
    private final Ownership ownership;
    private final UserRepository users;
    private final PetRepository pets;
    private final PetService service;
    private final com.petcarehub.config.PersonalPetProperties limits;

    @Transactional @PreAuthorize("hasRole('CUSTOMER')")
    public PetDTO create(@jakarta.validation.Valid PersonalPetCreateRequest input) {
        Long id=ownership.userId();
        users.findLockedById(id).orElseThrow(() -> new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy tài khoản"));
        // Serialize concurrent registrations on the stable owner row before counting.
        if (pets.countByOwnerId(id) >= limits.getMaxPersonalPets())
            throw new IllegalStateException("Bạn đã đạt giới hạn thú cưng cá nhân");
        var request=new PetCreateRequest();
        request.setName(input.getName().strip());request.setSpeciesId(input.getSpeciesId());request.setBreedId(input.getBreedId());
        request.setAge(input.getAge());request.setBirthDate(input.getBirthDate());request.setDescription(input.getDescription());request.setImageUrl(input.getImageUrl());
        request.setOwnerId(id);request.setPrice(java.math.BigDecimal.ZERO);
        request.setHealthStatus("HEALTHY");request.setAvailabilityStatus("UNAVAILABLE");
        return service.createPet(request);
    }
}
