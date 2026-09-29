package com.petcarehub.dto.review;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ReviewCreateRequest {
    @NotNull(message = "Reviewer ID is required")
    private Long reviewerId;
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;
    
    @NotBlank(message = "Comment is required")
    private String comment;
    
    @NotBlank(message = "Target type is required")
    private String targetType;
    
    @NotNull(message = "Target ID is required")
    private Long targetId;
}
