package com.petcarehub.controller;

import com.petcarehub.dto.health.TreatmentDTO;
import com.petcarehub.dto.health.TreatmentCreateRequest;
import com.petcarehub.service.TreatmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/treatments")
@RequiredArgsConstructor
public class TreatmentController {

    private final TreatmentService treatmentService;

    @PostMapping
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ResponseEntity<TreatmentDTO> createTreatment(@RequestBody TreatmentCreateRequest request) {
        return new ResponseEntity<>(treatmentService.createTreatment(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TreatmentDTO> getTreatmentById(@PathVariable Long id) {
        return ResponseEntity.ok(treatmentService.getTreatmentById(id));
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<List<TreatmentDTO>> getTreatmentsByMedicalRecordId(@PathVariable Long recordId) {
        return ResponseEntity.ok(treatmentService.getTreatmentsByMedicalRecordId(recordId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ResponseEntity<TreatmentDTO> updateTreatment(@PathVariable Long id, @RequestBody TreatmentCreateRequest request) {
        return ResponseEntity.ok(treatmentService.updateTreatment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ResponseEntity<Void> deleteTreatment(@PathVariable Long id) {
        treatmentService.deleteTreatment(id);
        return ResponseEntity.noContent().build();
    }
}
