package com.petcarehub.dto.loyalty;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTransactionDTO {
    private Long id;
    private Long accountId;
    private Integer points;
    private String description;
    private String transactionType; 
    private LocalDateTime transactionDate;
}
