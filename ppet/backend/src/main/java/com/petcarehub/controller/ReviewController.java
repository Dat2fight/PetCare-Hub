package com.petcarehub.controller;

import com.petcarehub.dto.review.ReviewCreateRequest;
import com.petcarehub.dto.review.ReviewDTO;
import com.petcarehub.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PutMapping("/{id}/approve")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','SALES_STAFF')")
    public ResponseEntity<ReviewDTO> approveReview(@PathVariable Long id) { return ResponseEntity.ok(reviewService.approveReview(id)); }

    @GetMapping("/me")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public org.springframework.data.domain.Page<ReviewDTO> mine(org.springframework.data.domain.Pageable pageable) { return reviewService.mine(pageable); }

    @GetMapping("/eligibility")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public boolean eligible(@RequestParam String targetType, @RequestParam Long targetId) { return reviewService.eligible(targetType, targetId); }

    @GetMapping("/summary")
    public com.petcarehub.dto.review.ReviewStats summary(@RequestParam String targetType, @RequestParam Long targetId) { return reviewService.stats(targetType, targetId); }

    @GetMapping("/pending")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','SALES_STAFF')")
    public org.springframework.data.domain.Page<ReviewDTO> pending(org.springframework.data.domain.Pageable pageable) { return reviewService.pending(pageable); }

    public record Reason(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=500) String reason) {}

    @PutMapping("/{id}/reject")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','SALES_STAFF')")
    public ReviewDTO reject(@PathVariable Long id, @Valid @RequestBody Reason reason) { return reviewService.moderate(id,"REJECTED",reason.reason().strip()); }

    @PutMapping("/{id}/hide")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','SALES_STAFF')")
    public ReviewDTO hide(@PathVariable Long id) { return reviewService.moderate(id,"HIDDEN",null); }

    @PutMapping("/me/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public ReviewDTO edit(@PathVariable Long id, @Valid @RequestBody com.petcarehub.dto.review.ReviewChanges change) { return reviewService.edit(id,change); }

    @DeleteMapping("/me/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> remove(@PathVariable Long id) { reviewService.remove(id); return ResponseEntity.noContent().build(); }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewDTO> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        return new ResponseEntity<>(reviewService.createReview(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @GetMapping
    public ResponseEntity<List<ReviewDTO>> getReviewsByTarget(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return ResponseEntity.ok(reviewService.getReviewsByReviewable(targetType, targetId));
    }
}
