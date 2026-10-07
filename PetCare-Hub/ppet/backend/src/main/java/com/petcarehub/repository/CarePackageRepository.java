package com.petcarehub.repository;

import com.petcarehub.entity.CarePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CarePackageRepository extends JpaRepository<CarePackage, Long> {
    List<CarePackage> findByActiveTrue();
}
