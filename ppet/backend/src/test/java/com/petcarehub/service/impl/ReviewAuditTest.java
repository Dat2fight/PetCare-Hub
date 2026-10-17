package com.petcarehub.service.impl;

import com.petcarehub.dto.review.ReviewCreateRequest;
import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewAuditTest {
    @Test void targetPurchaseDuplicateAndModerationChecks() {
        var reviews = mock(ReviewRepository.class); var users = mock(UserRepository.class); var orders = mock(OrderRepository.class);
        var em = mock(EntityManager.class);
        var service = new ReviewServiceImpl(reviews, users, orders, mock(AppointmentRepository.class), em, mock(ApplicationEventPublisher.class));
        User account = User.builder().username("audit").build(); account.setId(1L);
        var principal = UserDetailsImpl.build(account);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            when(users.findById(1L)).thenReturn(Optional.of(account)); when(users.findLockedById(1L)).thenReturn(Optional.of(account));
            var request = new ReviewCreateRequest(); request.setTargetType("PRODUCT"); request.setTargetId(2L); request.setRating(5); request.setComment("Tốt");
            assertThrows(com.petcarehub.exception.ResourceNotFoundException.class, () -> service.createReview(request));
            when(em.find(Product.class, 2L)).thenReturn(new Product());
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.createReview(request));
            when(orders.hasDeliveredTarget(1L, "PRODUCT", 2L)).thenReturn(true);
            when(reviews.save(any())).thenAnswer(i -> i.getArgument(0));
            assertEquals("PENDING", service.createReview(request).getStatus());
            when(reviews.existsByCustomerIdAndReviewableTypeAndReviewableId(1L, "PRODUCT", 2L)).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> service.createReview(request));
            Review pending = Review.builder().customer(account).approved(false).build(); when(reviews.findById(3L)).thenReturn(Optional.of(pending));
            SecurityContextHolder.clearContext();
            assertThrows(com.petcarehub.exception.ResourceNotFoundException.class, () -> service.getReviewById(3L));
        } finally { SecurityContextHolder.clearContext(); }
    }
}
