package com.petcarehub.dto.loyalty;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyAccountDTO {
    private Long id;
    private Long userId;
    private Integer currentPoints;
    private Integer lifetimePoints;
    private String membershipLevel;
}
