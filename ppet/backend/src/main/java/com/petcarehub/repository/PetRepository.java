package com.petcarehub.repository;

import com.petcarehub.entity.Pet;
import com.petcarehub.enums.AvailabilityStatus;
import com.petcarehub.enums.HealthStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long>, JpaSpecificationExecutor<Pet> {
    Page<Pet> findByAvailabilityStatus(AvailabilityStatus status, Pageable pageable);
    List<Pet> findBySpeciesIdAndAvailabilityStatus(Long speciesId, AvailabilityStatus status);
    List<Pet> findByHealthStatus(HealthStatus healthStatus);
}
