package com.petcarehub.service.impl;

import com.petcarehub.dto.health.MedicalRecordDTO;
import com.petcarehub.dto.health.MedicalRecordCreateRequest;
import com.petcarehub.service.MedicalRecordService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {
    
    @Override
    public MedicalRecordDTO createMedicalRecord(MedicalRecordCreateRequest request) {
        return new MedicalRecordDTO();
    }

    @Override
    public MedicalRecordDTO getMedicalRecordById(Long id) {
        return new MedicalRecordDTO();
    }

    @Override
    public List<MedicalRecordDTO> getMedicalRecordsByPetId(Long petId) {
        return new ArrayList<>();
    }

    @Override
    public MedicalRecordDTO updateMedicalRecord(Long id, MedicalRecordCreateRequest request) {
        return new MedicalRecordDTO();
    }

    @Override
    public void deleteMedicalRecord(Long id) {
    }
}
