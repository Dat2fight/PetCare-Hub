package com.petcarehub.repository;

import com.petcarehub.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByReviewableTypeAndReviewableId(String type, Long id, Pageable pageable);
    Page<Review> findByCustomerId(Long customerId, Pageable pageable);
    Page<Review> findByApprovedTrue(Pageable pageable);
}
