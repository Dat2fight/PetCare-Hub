package com.petcarehub.repository;

import com.petcarehub.entity.ServiceEntity;
import com.petcarehub.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceEntityRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByServiceTypeAndActiveTrue(ServiceType serviceType);
    List<ServiceEntity> findByActive(boolean active);
    java.util.Optional<ServiceEntity> findByIdAndActiveTrue(Long id);
    List<ServiceEntity> findByActiveTrue();
}
