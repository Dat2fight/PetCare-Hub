package com.petcarehub.repository;

import com.petcarehub.entity.CarePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CarePackageRepository extends JpaRepository<CarePackage, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from CarePackage p where p.id=:id")
    java.util.Optional<CarePackage> findLockedById(Long id);
    List<CarePackage> findByActiveTrue();
}
