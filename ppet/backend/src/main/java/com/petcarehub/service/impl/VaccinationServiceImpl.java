package com.petcarehub.service.impl;

import com.petcarehub.dto.health.VaccinationDTO;
import com.petcarehub.dto.health.VaccinationCreateRequest;
import com.petcarehub.service.VaccinationService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class VaccinationServiceImpl implements VaccinationService {

    @Override
    public VaccinationDTO createVaccination(VaccinationCreateRequest request) {
        return new VaccinationDTO();
    }

    @Override
    public VaccinationDTO getVaccinationById(Long id) {
        return new VaccinationDTO();
    }

    @Override
    public List<VaccinationDTO> getVaccinationsByMedicalRecordId(Long recordId) {
        return new ArrayList<>();
    }

    @Override
    public VaccinationDTO updateVaccination(Long id, VaccinationCreateRequest request) {
        return new VaccinationDTO();
    }

    @Override
    public void deleteVaccination(Long id) {
    }
}
