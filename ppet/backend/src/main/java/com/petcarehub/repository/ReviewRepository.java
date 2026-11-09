package com.petcarehub.repository;

import com.petcarehub.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("from Review r where r.id=:id")
    java.util.Optional<Review> locked(Long id);
    @org.springframework.data.jpa.repository.Query("from Review r where r.approved=false and (r.moderationState is null or r.moderationState='PENDING')")
    Page<Review> pending(Pageable pageable);
    @org.springframework.data.jpa.repository.Query("select new com.petcarehub.dto.review.ReviewStats(count(r), avg(r.rating)) from Review r where r.approved=true and r.reviewableType=:type and r.reviewableId=:id")
    com.petcarehub.dto.review.ReviewStats stats(String type, Long id);
    Page<Review> findByReviewableTypeAndReviewableId(String type, Long id, Pageable pageable);
    boolean existsByCustomerIdAndReviewableTypeAndReviewableId(Long customerId, String type, Long id);
    Page<Review> findByReviewableTypeAndReviewableIdAndApprovedTrue(String type, Long id, Pageable pageable);
    Page<Review> findByCustomerId(Long customerId, Pageable pageable);
    Page<Review> findByApprovedTrue(Pageable pageable);
}
