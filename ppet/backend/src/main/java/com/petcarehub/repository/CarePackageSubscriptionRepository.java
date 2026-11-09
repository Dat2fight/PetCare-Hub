package com.petcarehub.repository;

import com.petcarehub.entity.CarePackageSubscription;
import com.petcarehub.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CarePackageSubscriptionRepository extends JpaRepository<CarePackageSubscription, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"pet","carePackage","orderItem"})
    List<CarePackageSubscription> findByCustomerIdOrderByIdDesc(Long customerId);
    List<CarePackageSubscription> findByCustomerId(Long customerId);
    List<CarePackageSubscription> findByStatus(SubscriptionStatus status);
}
