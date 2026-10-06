package com.petcarehub.service.impl;

import com.petcarehub.dto.health.MedicalRecordDTO;
import com.petcarehub.dto.health.MedicalRecordCreateRequest;
import com.petcarehub.entity.MedicalRecord;
import com.petcarehub.entity.Pet;
import com.petcarehub.entity.User;
import com.petcarehub.entity.Appointment;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.MedicalRecordRepository;
import com.petcarehub.repository.PetRepository;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.repository.AppointmentRepository;
import com.petcarehub.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {
    
    private final MedicalRecordRepository medicalRecordRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public MedicalRecordDTO createMedicalRecord(MedicalRecordCreateRequest request) {
        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id: " + request.getPetId()));
                
        User vet = userRepository.findById(request.getVeterinarianId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinarian not found with id: " + request.getVeterinarianId()));
                
        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        }

        MedicalRecord record = MedicalRecord.builder()
                .pet(pet)
                .veterinarian(vet)
                .appointment(appointment)
                .examinationDate(request.getExaminationDate())
                .findings(request.getFindings())
                .diagnosis(request.getDiagnosis())
                .recommendations(request.getRecommendations())
                .build();
                
        return mapToDTO(medicalRecordRepository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordDTO getMedicalRecordById(Long id) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical Record not found with id: " + id));
        return mapToDTO(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordDTO> getMedicalRecordsByPetId(Long petId) {
        return medicalRecordRepository.findByPetId(petId, org.springframework.data.domain.Pageable.unpaged()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MedicalRecordDTO updateMedicalRecord(Long id, MedicalRecordCreateRequest request) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical Record not found with id: " + id));
                
        record.setExaminationDate(request.getExaminationDate());
        record.setFindings(request.getFindings());
        record.setDiagnosis(request.getDiagnosis());
        record.setRecommendations(request.getRecommendations());
        
        return mapToDTO(medicalRecordRepository.save(record));
    }

    @Override
    @Transactional
    public void deleteMedicalRecord(Long id) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical Record not found with id: " + id));
        medicalRecordRepository.delete(record);
    }
    
    private MedicalRecordDTO mapToDTO(MedicalRecord record) {
        return MedicalRecordDTO.builder()
                .id(record.getId())
                .petId(record.getPet().getId())
                .veterinarianId(record.getVeterinarian().getId())
                .veterinarianName(record.getVeterinarian().getFirstName() + " " + record.getVeterinarian().getLastName())
                .appointmentId(record.getAppointment() != null ? record.getAppointment().getId() : null)
                .examinationDate(record.getExaminationDate())
                .findings(record.getFindings())
                .diagnosis(record.getDiagnosis())
                .recommendations(record.getRecommendations())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
