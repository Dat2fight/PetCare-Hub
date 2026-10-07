package com.petcarehub.service;

import com.petcarehub.dto.loyalty.LoyaltyAccountDTO;
import com.petcarehub.dto.loyalty.LoyaltyTransactionDTO;
import java.util.List;

public interface LoyaltyService {
    LoyaltyAccountDTO getAccountByUserId(Long userId);
    LoyaltyAccountDTO addPoints(Long userId, Integer points, String description);
    LoyaltyAccountDTO spendPoints(Long userId, Integer points, String description);
    List<LoyaltyTransactionDTO> getTransactions(Long accountId);
}
