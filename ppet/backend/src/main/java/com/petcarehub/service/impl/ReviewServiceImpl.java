package com.petcarehub.service.impl;

import com.petcarehub.dto.review.ReviewCreateRequest;
import com.petcarehub.dto.review.ReviewDTO;
import com.petcarehub.entity.Review;
import com.petcarehub.entity.User;
import com.petcarehub.event.NotificationEvent;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.ReviewRepository;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReviewDTO createReview(ReviewCreateRequest request) {
        // Zero Trust: Always extract authenticated user from SecurityContextHolder
        User customer = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                customer = userRepository.findById(userDetails.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userDetails.getId()));
            } else {
                customer = userRepository.findByUsername(authentication.getName())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with username " + authentication.getName()));
            }
        } else if (request.getReviewerId() != null) {
            customer = userRepository.findById(request.getReviewerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + request.getReviewerId()));
        } else {
            throw new BadRequestException("Bạn cần đăng nhập để gửi đánh giá");
        }

        Review review = Review.builder()
                .customer(customer)
                .rating(request.getRating())
                .comment(request.getComment())
                .reviewableType(request.getTargetType())
                .reviewableId(request.getTargetId())
                .approved(true) // default or based on logic
                .build();

        review = reviewRepository.save(review);
        
        // Publish Notification Event
        eventPublisher.publishEvent(new NotificationEvent(this, review));

        return mapToDTO(review);
    }

    @Override
    public ReviewDTO getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        return mapToDTO(review);
    }

    @Override
    public List<ReviewDTO> getReviewsByReviewable(String type, Long id) {
        return reviewRepository.findByReviewableTypeAndReviewableId(type, id, Pageable.unpaged())
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private ReviewDTO mapToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setReviewerId(review.getCustomer().getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setTargetType(review.getReviewableType());
        dto.setTargetId(review.getReviewableId());
        dto.setStatus(review.getApproved() ? "APPROVED" : "PENDING");
        return dto;
    }
}
