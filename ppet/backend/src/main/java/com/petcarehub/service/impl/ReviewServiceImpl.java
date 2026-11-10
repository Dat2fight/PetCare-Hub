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
    private final com.petcarehub.security.Ownership ownership;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReviewDTO createReview(ReviewCreateRequest request) {
        User customer = userRepository.findLockedById(ownership.userId()).orElseThrow();
        String type = request.getTargetType().toUpperCase(java.util.Locale.ROOT);
        Class<?> target = switch (type) {
            case "PRODUCT" -> com.petcarehub.entity.Product.class;
            case "PET" -> com.petcarehub.entity.Pet.class;
            case "SERVICE" -> com.petcarehub.entity.ServiceEntity.class;
            default -> throw new BadRequestException("Loại đánh giá không hợp lệ");
        };
        if (entityManager.find(target, request.getTargetId()) == null) throw new ResourceNotFoundException("Không tìm thấy đối tượng đánh giá");
        boolean eligible = purchased(customer.getId(), type, request.getTargetId());
        if (!eligible) throw new org.springframework.security.access.AccessDeniedException("Bạn cần hoàn thành giao dịch trước khi đánh giá");
        if (reviewRepository.existsByCustomerIdAndReviewableTypeAndReviewableId(customer.getId(), type, request.getTargetId())) throw new IllegalStateException("Bạn đã đánh giá đối tượng này");

        Review review = Review.builder()
                .customer(customer)
                .rating(request.getRating())
                .comment(request.getComment().strip())
                .reviewableType(type)
                .reviewableId(request.getTargetId())
                .approved(false)
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
        return moderate(id, "APPROVED", null);
    }

    private boolean purchased(Long customer, String type, Long target) {
        return "SERVICE".equals(type)
                ? appointments.existsByCustomerIdAndServiceIdAndStatus(customer, target, com.petcarehub.enums.AppointmentStatus.COMPLETED)
                : orders.hasDeliveredTarget(customer, type, target);
    }

    public boolean eligible(String type, Long id) {
        Long customer = ownership.userId();
        return purchased(customer, type, id) && !reviewRepository.existsByCustomerIdAndReviewableTypeAndReviewableId(customer, type, id);
    }

    public org.springframework.data.domain.Page<ReviewDTO> mine(Pageable pageable) {
        return reviewRepository.findByCustomerId(ownership.userId(), pageable).map(this::mapToDTO);
    }

    public org.springframework.data.domain.Page<ReviewDTO> pending(Pageable pageable) {
        return reviewRepository.pending(pageable).map(this::mapToDTO);
    }

    public com.petcarehub.dto.review.ReviewStats stats(String type, Long id) {
        return reviewRepository.stats(type, id);
    }

    private Review locked(Long id) {
        return reviewRepository.locked(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
    }

    private Review owned(Long id) {
        var review = locked(id);
        if (!review.getCustomer().getId().equals(ownership.userId())) throw new ResourceNotFoundException("Không tìm thấy đánh giá");
        return review;
    }

    @Transactional
    public ReviewDTO moderate(Long id, String action, String reason) {
        var review = locked(id);
        review.setApproved("APPROVED".equals(action));
        review.setModerationState(action);
        review.setModerationReason(reason);
        return mapToDTO(review);
    }

    @Transactional
    public ReviewDTO edit(Long id, com.petcarehub.dto.review.ReviewChanges change) {
        var review = owned(id);
        review.setComment(change.comment().strip());
        review.setRating(change.rating());
        review.setApproved(false);
        review.setModerationState("PENDING");
        review.setModerationReason(null);
        return mapToDTO(review);
    }

    @Transactional
    public void remove(Long id) {
        reviewRepository.delete(owned(id));
    }

    private ReviewDTO mapToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setReviewerId(review.getCustomer().getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setTargetType(review.getReviewableType());
        dto.setTargetId(review.getReviewableId());
        dto.setStatus(review.getApproved() ? "APPROVED" : java.util.Objects.requireNonNullElse(review.getModerationState(), "PENDING"));
        dto.setModerationReason(review.getModerationReason());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
