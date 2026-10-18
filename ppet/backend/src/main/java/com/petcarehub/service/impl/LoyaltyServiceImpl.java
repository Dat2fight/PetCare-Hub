package com.petcarehub.service.impl;

import com.petcarehub.dto.loyalty.LoyaltyAccountDTO;
import com.petcarehub.dto.loyalty.LoyaltyTransactionDTO;
import com.petcarehub.entity.LoyaltyAccount;
import com.petcarehub.entity.LoyaltyTransaction;
import com.petcarehub.enums.MembershipLevel;
import com.petcarehub.enums.LoyaltyTransactionType;
import com.petcarehub.enums.LoyaltySourceType;
import com.petcarehub.entity.User;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.LoyaltyAccountRepository;
import com.petcarehub.repository.LoyaltyTransactionRepository;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.service.LoyaltyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoyaltyServiceImpl implements LoyaltyService {

    // Per-operation safety limit; membership thresholds remain unchanged.
    public static final int MAX_POINTS_PER_OPERATION = 1_000_000;
    private void validatePoints(Integer points) {
        if (points == null || points <= 0 || points > MAX_POINTS_PER_OPERATION)
            throw new com.petcarehub.exception.BadRequestException("Điểm phải từ 1 đến " + MAX_POINTS_PER_OPERATION);
    }

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public LoyaltyServiceImpl(LoyaltyAccountRepository accountRepository, LoyaltyTransactionRepository transactionRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private LoyaltyAccount getOrCreateAccount(Long userId) {
        // Serialize on the stable parent even before the first loyalty account exists.
        // Every caller holds this lock until its balance and ledger transaction commits.
        User user = userRepository.findLockedById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        return accountRepository.findByCustomerId(userId).orElseGet(() -> {
            LoyaltyAccount account = new LoyaltyAccount();
            account.setCustomer(user);
            account.setAvailablePoints(0);
            account.setTotalPoints(0);
            account.setMembershipLevel(MembershipLevel.BRONZE);
            return accountRepository.save(account);
        });
    }

    @Override
    @Transactional
    public LoyaltyAccountDTO getAccountByUserId(Long userId) {
        return mapToDTO(getOrCreateAccount(userId));
    }

    @Override
    @Transactional
    public LoyaltyAccountDTO addPoints(Long userId, Integer points, String description) {
        validatePoints(points);
        LoyaltyAccount account = getOrCreateAccount(userId);
        if ((long) account.getAvailablePoints() + points > Integer.MAX_VALUE
                || (long) account.getTotalPoints() + points > Integer.MAX_VALUE)
            throw new com.petcarehub.exception.BadRequestException("Tổng điểm vượt giới hạn lưu trữ");
        account.setAvailablePoints(account.getAvailablePoints() + points);
        account.setTotalPoints(account.getTotalPoints() + points);
        
        updateMembershipLevel(account);
        account = accountRepository.save(account);
        
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setLoyaltyAccount(account);
        tx.setPoints(points);
        tx.setDescription(description);
        tx.setTransactionType(LoyaltyTransactionType.EARN);
        tx.setSourceType(LoyaltySourceType.PURCHASE);
        transactionRepository.save(tx);
        
        return mapToDTO(account);
    }

    @Override
    @Transactional
    public LoyaltyAccountDTO spendPoints(Long userId, Integer points, String description) {
        validatePoints(points);
        LoyaltyAccount account = getOrCreateAccount(userId);
        if (account.getAvailablePoints() < points) {
            throw new com.petcarehub.exception.BadRequestException("Không đủ điểm");
        }
        account.setAvailablePoints(account.getAvailablePoints() - points);
        account = accountRepository.save(account);
        
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setLoyaltyAccount(account);
        tx.setPoints(points);
        tx.setDescription(description);
        tx.setTransactionType(LoyaltyTransactionType.REDEEM);
        tx.setSourceType(LoyaltySourceType.PURCHASE);
        transactionRepository.save(tx);
        
        return mapToDTO(account);
    }

    @Override
    public List<LoyaltyTransactionDTO> getTransactions(Long accountId, org.springframework.data.domain.Pageable pageable) {
        return transactionRepository.findByLoyaltyAccountId(accountId, pageable).getContent().stream().map(tx -> {
            LoyaltyTransactionDTO dto = new LoyaltyTransactionDTO();
            dto.setId(tx.getId());
            dto.setPoints(tx.getPoints());
            dto.setDescription(tx.getDescription());
            dto.setTransactionType(tx.getTransactionType().name());
            dto.setTransactionDate(tx.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
    
    private static final java.util.Map<String, Integer> THRESHOLDS = java.util.Map.of("BRONZE", 0, "SILVER", 1000, "GOLD", 5000, "PLATINUM", 10000);

    private void updateMembershipLevel(LoyaltyAccount account) {
        int lifetime = account.getTotalPoints() == null ? 0 : account.getTotalPoints();
        if (lifetime >= THRESHOLDS.get("PLATINUM")) {
            account.setMembershipLevel(MembershipLevel.PLATINUM);
        } else if (lifetime >= THRESHOLDS.get("GOLD")) {
            account.setMembershipLevel(MembershipLevel.GOLD);
        } else if (lifetime >= THRESHOLDS.get("SILVER")) {
            account.setMembershipLevel(MembershipLevel.SILVER);
        } else {
            account.setMembershipLevel(MembershipLevel.BRONZE);
        }
    }
    
    private LoyaltyAccountDTO mapToDTO(LoyaltyAccount account) {
        LoyaltyAccountDTO dto = new LoyaltyAccountDTO();
        dto.setId(account.getId());
        dto.setUserId(account.getCustomer().getId());
        dto.setCurrentPoints(account.getAvailablePoints());
        dto.setLifetimePoints(account.getTotalPoints());
        dto.setMembershipLevel(account.getMembershipLevel() != null ? account.getMembershipLevel().name() : "BRONZE");
        dto.setThresholds(THRESHOLDS);
        dto.setTier(dto.getMembershipLevel());
        var next = THRESHOLDS.entrySet().stream().filter(e -> e.getValue() > account.getTotalPoints()).min(java.util.Map.Entry.comparingByValue());
        dto.setNextTier(next.map(java.util.Map.Entry::getKey).orElse(null));
        dto.setPointsToNextTier(next.map(e -> e.getValue() - account.getTotalPoints()).orElse(0));
        return dto;
    }
}

