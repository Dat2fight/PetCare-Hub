package com.petcarehub.service;

import com.petcarehub.dto.review.ReviewCreateRequest;
import com.petcarehub.dto.review.ReviewDTO;

import java.util.List;

public interface ReviewService {
    ReviewDTO approveReview(Long id);
    ReviewDTO createReview(ReviewCreateRequest request);
    ReviewDTO getReviewById(Long id);
    List<ReviewDTO> getReviewsByReviewable(String type, Long id);
}
