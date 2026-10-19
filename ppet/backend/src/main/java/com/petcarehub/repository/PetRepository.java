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
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @org.springframework.data.jpa.repository.Query("select p from Pet p where p.id = :id")
    java.util.Optional<Pet> findByIdForUpdate(Long id);
    Page<Pet> findByAvailabilityStatus(AvailabilityStatus status, Pageable pageable);
    List<Pet> findBySpeciesIdAndAvailabilityStatus(Long speciesId, AvailabilityStatus status);
    List<Pet> findByHealthStatus(HealthStatus healthStatus);
    List<Pet> findByOwnerId(Long ownerId);
    long countByOwnerId(Long ownerId);
}
