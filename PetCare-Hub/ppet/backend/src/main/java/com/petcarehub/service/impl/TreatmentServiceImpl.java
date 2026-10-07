package com.petcarehub.service.impl;

import com.petcarehub.dto.health.TreatmentDTO;
import com.petcarehub.dto.health.TreatmentCreateRequest;
import com.petcarehub.service.TreatmentService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class TreatmentServiceImpl implements TreatmentService {

    @Override
    public TreatmentDTO createTreatment(TreatmentCreateRequest request) {
        return new TreatmentDTO();
    }

    @Override
    public TreatmentDTO getTreatmentById(Long id) {
        return new TreatmentDTO();
    }

    @Override
    public List<TreatmentDTO> getTreatmentsByMedicalRecordId(Long recordId) {
        return new ArrayList<>();
    }

    @Override
    public TreatmentDTO updateTreatment(Long id, TreatmentCreateRequest request) {
        return new TreatmentDTO();
    }

    @Override
    public void deleteTreatment(Long id) {
    }
}
