package com.petcarehub.service;

import com.petcarehub.dto.health.TreatmentDTO;
import com.petcarehub.dto.health.TreatmentCreateRequest;
import java.util.List;

public interface TreatmentService {
    TreatmentDTO createTreatment(TreatmentCreateRequest request);
    TreatmentDTO getTreatmentById(Long id);
    List<TreatmentDTO> getTreatmentsByMedicalRecordId(Long recordId);
    TreatmentDTO updateTreatment(Long id, TreatmentCreateRequest request);
    void deleteTreatment(Long id);
}
