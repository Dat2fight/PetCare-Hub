package com.petcarehub.service.impl;

import com.petcarehub.dto.health.VaccinationDTO;
import com.petcarehub.dto.health.VaccinationCreateRequest;
import com.petcarehub.entity.Pet;
import com.petcarehub.entity.User;
import com.petcarehub.entity.Vaccination;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.PetRepository;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.repository.VaccinationRepository;
import com.petcarehub.service.VaccinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaccinationServiceImpl implements VaccinationService {

    private final VaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public VaccinationDTO createVaccination(VaccinationCreateRequest request) {
        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
                
        User vet = userRepository.findById(request.getVeterinarianId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinarian not found"));

        Vaccination vaccination = Vaccination.builder()
                .pet(pet)
                .veterinarian(vet)
                .vaccineName(request.getVaccineName())
                .vaccineBatchNumber(request.getVaccineBatchNumber())
                .dateAdministered(request.getDateAdministered())
                .nextDueDate(request.getNextDueDate())
                .notes(request.getNotes())
                .build();
                
        return mapToDTO(vaccinationRepository.save(vaccination));
    }

    @Override
    @Transactional(readOnly = true)
    public VaccinationDTO getVaccinationById(Long id) {
        Vaccination vaccination = vaccinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination not found"));
        return mapToDTO(vaccination);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VaccinationDTO> getVaccinationsByPetId(Long petId) {
        return vaccinationRepository.findByPetId(petId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VaccinationDTO updateVaccination(Long id, VaccinationCreateRequest request) {
        Vaccination vaccination = vaccinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination not found"));
                
        vaccination.setVaccineName(request.getVaccineName());
        vaccination.setVaccineBatchNumber(request.getVaccineBatchNumber());
        vaccination.setDateAdministered(request.getDateAdministered());
        vaccination.setNextDueDate(request.getNextDueDate());
        vaccination.setNotes(request.getNotes());
        
        return mapToDTO(vaccinationRepository.save(vaccination));
    }

    @Override
    @Transactional
    public void deleteVaccination(Long id) {
        Vaccination vaccination = vaccinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination not found"));
        vaccinationRepository.delete(vaccination);
    }
    
    private VaccinationDTO mapToDTO(Vaccination vaccination) {
        return VaccinationDTO.builder()
                .id(vaccination.getId())
                .petId(vaccination.getPet().getId())
                .veterinarianId(vaccination.getVeterinarian().getId())
                .veterinarianName(vaccination.getVeterinarian().getFirstName() + " " + vaccination.getVeterinarian().getLastName())
                .vaccineName(vaccination.getVaccineName())
                .vaccineBatchNumber(vaccination.getVaccineBatchNumber())
                .dateAdministered(vaccination.getDateAdministered())
                .nextDueDate(vaccination.getNextDueDate())
                .notes(vaccination.getNotes())
                .createdAt(vaccination.getCreatedAt())
                .build();
    }
}
