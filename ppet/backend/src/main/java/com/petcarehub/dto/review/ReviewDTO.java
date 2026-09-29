package com.petcarehub.dto.review;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    private Long id;
    private Long reviewerId;
    private Integer rating;
    private String comment;
    private String targetType;
    private Long targetId;
    private String status;
    private LocalDateTime createdAt;
}
