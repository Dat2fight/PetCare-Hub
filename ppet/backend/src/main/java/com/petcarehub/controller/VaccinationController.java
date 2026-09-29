package com.petcarehub.controller;

import com.petcarehub.dto.health.VaccinationDTO;
import com.petcarehub.dto.health.VaccinationCreateRequest;
import com.petcarehub.service.VaccinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vaccinations")
@RequiredArgsConstructor
public class VaccinationController {

    private final VaccinationService vaccinationService;

    @PostMapping
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ResponseEntity<VaccinationDTO> createVaccination(@RequestBody VaccinationCreateRequest request) {
        return new ResponseEntity<>(vaccinationService.createVaccination(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VaccinationDTO> getVaccinationById(@PathVariable Long id) {
        return ResponseEntity.ok(vaccinationService.getVaccinationById(id));
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<List<VaccinationDTO>> getVaccinationsByMedicalRecordId(@PathVariable Long recordId) {
        return ResponseEntity.ok(vaccinationService.getVaccinationsByMedicalRecordId(recordId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ResponseEntity<VaccinationDTO> updateVaccination(@PathVariable Long id, @RequestBody VaccinationCreateRequest request) {
        return ResponseEntity.ok(vaccinationService.updateVaccination(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ResponseEntity<Void> deleteVaccination(@PathVariable Long id) {
        vaccinationService.deleteVaccination(id);
        return ResponseEntity.noContent().build();
    }
}
