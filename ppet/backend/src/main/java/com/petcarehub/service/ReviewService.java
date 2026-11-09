package com.petcarehub.service;

import com.petcarehub.dto.review.ReviewCreateRequest;
import com.petcarehub.dto.review.ReviewDTO;

import java.util.List;

public interface ReviewService {
    boolean eligible(String type, Long id);
    org.springframework.data.domain.Page<ReviewDTO> mine(org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<ReviewDTO> pending(org.springframework.data.domain.Pageable pageable);
    com.petcarehub.dto.review.ReviewStats stats(String type, Long id);
    ReviewDTO moderate(Long id, String action, String reason);
    ReviewDTO edit(Long id, com.petcarehub.dto.review.ReviewChanges change);
    void remove(Long id);
    ReviewDTO approveReview(Long id);
    ReviewDTO createReview(ReviewCreateRequest request);
    ReviewDTO getReviewById(Long id);
    List<ReviewDTO> getReviewsByReviewable(String type, Long id);
}
