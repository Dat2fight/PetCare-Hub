package com.petcarehub.service;

import com.petcarehub.dto.health.VaccinationDTO;
import com.petcarehub.dto.health.VaccinationCreateRequest;
import java.util.List;

public interface VaccinationService {
    VaccinationDTO createVaccination(VaccinationCreateRequest request);
    VaccinationDTO getVaccinationById(Long id);
    List<VaccinationDTO> getVaccinationsByPetId(Long petId);
    VaccinationDTO updateVaccination(Long id, VaccinationCreateRequest request);
    void deleteVaccination(Long id);
}
