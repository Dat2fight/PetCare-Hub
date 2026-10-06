package com.petcarehub.controller;

import com.petcarehub.dto.health.MedicalRecordDTO;
import com.petcarehub.dto.health.MedicalRecordCreateRequest;
import com.petcarehub.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
    public ResponseEntity<MedicalRecordDTO> createMedicalRecord(@RequestBody MedicalRecordCreateRequest request) {
        return new ResponseEntity<>(medicalRecordService.createMedicalRecord(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordDTO> getMedicalRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordById(id));
    }

    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<MedicalRecordDTO>> getMedicalRecordsByPetId(@PathVariable Long petId) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordsByPetId(petId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
    public ResponseEntity<MedicalRecordDTO> updateMedicalRecord(@PathVariable Long id, @RequestBody MedicalRecordCreateRequest request) {
        return ResponseEntity.ok(medicalRecordService.updateMedicalRecord(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
    public ResponseEntity<Void> deleteMedicalRecord(@PathVariable Long id) {
        medicalRecordService.deleteMedicalRecord(id);
        return ResponseEntity.noContent().build();
    }
}
