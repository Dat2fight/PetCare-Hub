package com.petcarehub.repository;

import com.petcarehub.entity.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {
    List<Vaccination> findByPetId(Long petId);

    @Query("SELECT v FROM Vaccination v WHERE v.nextDueDate <= :date AND v.nextDueDate IS NOT NULL")
    List<Vaccination> findDueVaccinations(LocalDate date);
}
