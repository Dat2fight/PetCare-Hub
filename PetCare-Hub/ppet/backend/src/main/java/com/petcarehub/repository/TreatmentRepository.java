package com.petcarehub.repository;

import com.petcarehub.entity.Treatment;
import com.petcarehub.enums.TreatmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    List<Treatment> findByMedicalRecordId(Long medicalRecordId);
    List<Treatment> findByStatus(TreatmentStatus status);
}
