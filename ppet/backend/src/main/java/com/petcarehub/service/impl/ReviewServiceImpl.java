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
    private final com.petcarehub.repository.OrderRepository orders;
    private final com.petcarehub.repository.AppointmentRepository appointments;
    private final jakarta.persistence.EntityManager entityManager;
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
        } else {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Bạn cần đăng nhập để gửi đánh giá");
        }
        String type = request.getTargetType().toUpperCase(java.util.Locale.ROOT);
        Class<?> target = switch (type) {
            case "PRODUCT" -> com.petcarehub.entity.Product.class;
            case "PET" -> com.petcarehub.entity.Pet.class;
            case "SERVICE" -> com.petcarehub.entity.ServiceEntity.class;
            default -> throw new BadRequestException("Loại đánh giá không hợp lệ");
        };
        if (entityManager.find(target, request.getTargetId()) == null) throw new ResourceNotFoundException("Không tìm thấy đối tượng đánh giá");
        userRepository.findLockedById(customer.getId()).orElseThrow(); // Serialize duplicate submissions by this customer.
        boolean eligible = type.equals("SERVICE") ? appointments.existsByCustomerIdAndServiceIdAndStatus(customer.getId(), request.getTargetId(), com.petcarehub.enums.AppointmentStatus.COMPLETED)
                : orders.hasDeliveredTarget(customer.getId(), type, request.getTargetId());
        if (!eligible) throw new org.springframework.security.access.AccessDeniedException("Bạn cần hoàn thành giao dịch trước khi đánh giá");
        if (reviewRepository.existsByCustomerIdAndReviewableTypeAndReviewableId(customer.getId(), type, request.getTargetId())) throw new IllegalStateException("Bạn đã đánh giá đối tượng này");

        Review review = Review.builder()
                .customer(customer)
                .rating(request.getRating())
                .comment(request.getComment())
                .reviewableType(type)
                .reviewableId(request.getTargetId())
                .approved(false) // NEEDS DECISION: pending until an administrator approves.
                .build();

        review = reviewRepository.save(review);
        
        // Publish Notification Event
        eventPublisher.publishEvent(new NotificationEvent(this, review));

        return mapToDTO(review);
    }

    @Override
    public ReviewDTO getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        if (!Boolean.TRUE.equals(review.getApproved())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean visible = auth != null && (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                    || auth.getPrincipal() instanceof UserDetailsImpl p && p.getId().equals(review.getCustomer().getId()));
            if (!visible) throw new ResourceNotFoundException("Không tìm thấy đánh giá");
        }
        return mapToDTO(review);
    }

    @Override
    public List<ReviewDTO> getReviewsByReviewable(String type, Long id) {
        return reviewRepository.findByReviewableTypeAndReviewableIdAndApprovedTrue(type, id, org.springframework.data.domain.PageRequest.of(0, 100))
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewDTO approveReview(Long id) {
        Review review = reviewRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        review.setApproved(true); return mapToDTO(reviewRepository.save(review));
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
