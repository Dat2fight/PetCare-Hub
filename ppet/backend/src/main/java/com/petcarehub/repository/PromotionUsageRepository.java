package com.petcarehub.repository;

import com.petcarehub.entity.PromotionUsage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    long countByPromotionIdAndCustomerId(Long promotionId, Long customerId);
    Optional<PromotionUsage> findByOrderId(Long orderId);
}
