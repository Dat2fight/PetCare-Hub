package com.petcarehub.controller;

import com.petcarehub.dto.pet.PetCreateRequest;
import com.petcarehub.dto.pet.PetDTO;
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

    @GetMapping
    public ResponseEntity<Page<PetDTO>> getAllPets(
            @RequestParam(required = false) String healthStatus,
            @RequestParam(required = false) String availabilityStatus,
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) Long breedId,
            Pageable pageable) {
        return ResponseEntity.ok(petService.getPets(healthStatus, availabilityStatus, speciesId, breedId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetDTO> getPetById(@PathVariable Long id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<PetDTO> createPet(@Valid @RequestBody PetCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<PetDTO> updatePet(@PathVariable Long id, @Valid @RequestBody PetUpdateRequest request) {
        return ResponseEntity.ok(petService.updatePet(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }
}
