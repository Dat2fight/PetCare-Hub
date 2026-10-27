package com.petcarehub.controller;

import com.petcarehub.dto.pet.PetCreateRequest;
import com.petcarehub.dto.pet.PetDTO;
import com.petcarehub.dto.pet.PublicPetDTO;
import com.petcarehub.dto.pet.PetUpdateRequest;
import com.petcarehub.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final com.petcarehub.security.Ownership ownership;
    private final com.petcarehub.service.PersonalPetService personalPets;
    @PostMapping("/my-pets")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PetDTO> createMine(@Valid @RequestBody com.petcarehub.dto.pet.PersonalPetCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personalPets.create(request));
    }
    @PutMapping("/my-pets/{id}")
    public ResponseEntity<PetDTO> updateMine(@PathVariable Long id, @Valid @RequestBody PetUpdateRequest request) {
        ownership.ownedPet(id);
        PetDTO current = petService.getPetById(id);
        request.setOwnerId(ownership.userId()); request.setPrice(null);
        request.setHealthStatus(current.getHealthStatus()); request.setAvailabilityStatus(current.getAvailabilityStatus());
        return ResponseEntity.ok(petService.updatePet(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<PublicPetDTO>> getAllPets(
            @RequestParam(required = false) String healthStatus,
            @RequestParam(required = false) String availabilityStatus,
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) Long breedId,
            Pageable pageable) {
        return ResponseEntity.ok(petService.getPets(healthStatus, availabilityStatus, speciesId, breedId, pageable).map(PublicPetDTO::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicPetDTO> getPetById(@PathVariable Long id) {
        return ResponseEntity.ok(PublicPetDTO.from(petService.getPetById(id)));
    }

    @GetMapping("/my-pets/{id}")
    public ResponseEntity<PetDTO> getPrivatePet(@PathVariable Long id) {
        ownership.requirePet(id);
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @GetMapping("/my-pets")
    public ResponseEntity<java.util.List<PetDTO>> getMyPets(@org.springframework.security.core.annotation.AuthenticationPrincipal com.petcarehub.security.UserDetailsImpl userDetails) {
        if (userDetails == null) throw new org.springframework.security.authentication.InsufficientAuthenticationException("Bạn cần đăng nhập");
        return ResponseEntity.ok(petService.getPetsByOwner(userDetails.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public ResponseEntity<PetDTO> createPet(@Valid @RequestBody PetCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public ResponseEntity<PetDTO> updatePet(@PathVariable Long id, @Valid @RequestBody PetUpdateRequest request) {
        return ResponseEntity.ok(petService.updatePet(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }
}
