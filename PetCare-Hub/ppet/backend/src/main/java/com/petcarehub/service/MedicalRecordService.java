package com.petcarehub.service;

import com.petcarehub.dto.health.MedicalRecordDTO;
import com.petcarehub.dto.health.MedicalRecordCreateRequest;
import java.util.List;

public interface MedicalRecordService {
    MedicalRecordDTO createMedicalRecord(MedicalRecordCreateRequest request);
    MedicalRecordDTO getMedicalRecordById(Long id);
    List<MedicalRecordDTO> getMedicalRecordsByPetId(Long petId);
    MedicalRecordDTO updateMedicalRecord(Long id, MedicalRecordCreateRequest request);
    void deleteMedicalRecord(Long id);
}
