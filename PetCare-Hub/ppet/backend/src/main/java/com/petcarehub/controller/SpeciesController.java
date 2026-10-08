package com.petcarehub.controller;

import com.petcarehub.dto.pet.BreedDTO;
import com.petcarehub.dto.pet.SpeciesDTO;
import com.petcarehub.service.SpeciesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;

    @GetMapping
    public ResponseEntity<List<SpeciesDTO>> getAllSpecies() {
        return ResponseEntity.ok(speciesService.getAllSpecies());
    }

    @GetMapping("/{speciesId}/breeds")
    public ResponseEntity<List<BreedDTO>> getBreedsBySpeciesId(@PathVariable Long speciesId) {
        return ResponseEntity.ok(speciesService.getBreedsBySpeciesId(speciesId));
    }
}
