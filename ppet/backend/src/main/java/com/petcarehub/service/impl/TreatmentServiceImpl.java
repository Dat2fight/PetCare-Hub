package com.petcarehub.service.impl;

import com.petcarehub.dto.health.*;
import com.petcarehub.entity.Treatment;
import com.petcarehub.repository.*;
import com.petcarehub.security.Ownership;
import com.petcarehub.service.TreatmentService;
import com.petcarehub.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TreatmentServiceImpl implements TreatmentService {
    private final TreatmentRepository treatments;
    private final MedicalRecordRepository records;
    private final Ownership ownership;
    @Override public TreatmentDTO createTreatment(TreatmentCreateRequest request) { return save(new Treatment(), request); }
    @Override public TreatmentDTO updateTreatment(Long id, TreatmentCreateRequest request) { return save(find(id), request); }
    private TreatmentDTO save(Treatment treatment, TreatmentCreateRequest request) {
        if (request.getStartDate() == null || request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) throw new BadRequestException("endDate: Ngày kết thúc không được trước ngày bắt đầu");
        treatment.setMedicalRecord(records.findById(request.getMedicalRecordId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ y tế")));
        ownership.requirePet(treatment.getMedicalRecord().getPet().getId());
        treatment.setMedication(request.getMedicine()); treatment.setDosage(request.getDosage()); treatment.setFrequency(request.getFrequency());
        treatment.setStartDate(request.getStartDate()); treatment.setEndDate(request.getEndDate()); treatment.setNotes(request.getDescription());
        if (treatment.getStatus() == null) treatment.setStatus(com.petcarehub.enums.TreatmentStatus.ACTIVE);
        return dto(treatments.save(treatment));
    }
    private Treatment find(Long id) { return treatments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy điều trị")); }
    @Override public TreatmentDTO getTreatmentById(Long id) { Treatment t=find(id); ownership.requirePet(t.getMedicalRecord().getPet().getId()); return dto(t); }
    @Override public List<TreatmentDTO> getTreatmentsByMedicalRecordId(Long recordId) {
        var record=records.findById(recordId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ y tế")); ownership.requirePet(record.getPet().getId());
        return treatments.findByMedicalRecordId(recordId).stream().map(this::dto).toList();
    }
    @Override public void deleteTreatment(Long id) { Treatment t=find(id); ownership.requirePet(t.getMedicalRecord().getPet().getId()); treatments.delete(t); }
    private TreatmentDTO dto(Treatment t) { TreatmentDTO d=new TreatmentDTO(); d.setId(t.getId()); d.setMedicalRecordId(t.getMedicalRecord().getId()); d.setMedicine(t.getMedication()); d.setDosage(t.getDosage()); d.setFrequency(t.getFrequency()); d.setStartDate(t.getStartDate()); d.setEndDate(t.getEndDate()); d.setDescription(t.getNotes()); return d; }
}
