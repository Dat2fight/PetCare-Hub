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

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public LoyaltyServiceImpl(LoyaltyAccountRepository accountRepository, LoyaltyTransactionRepository transactionRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private LoyaltyAccount getOrCreateAccount(Long userId) {
        return accountRepository.findByCustomerId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
            LoyaltyAccount account = new LoyaltyAccount();
            account.setCustomer(user);
            account.setAvailablePoints(0);
            account.setTotalPoints(0);
            account.setMembershipLevel(MembershipLevel.BRONZE);
            return accountRepository.save(account);
        });
    }

    @Override
    public LoyaltyAccountDTO getAccountByUserId(Long userId) {
        return mapToDTO(getOrCreateAccount(userId));
    }

    @Override
    @Transactional
    public LoyaltyAccountDTO addPoints(Long userId, Integer points, String description) {
        LoyaltyAccount account = getOrCreateAccount(userId);
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
        LoyaltyAccount account = getOrCreateAccount(userId);
        if (account.getAvailablePoints() < points) {
            throw new RuntimeException("Insufficient points");
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
    public List<LoyaltyTransactionDTO> getTransactions(Long accountId) {
        return transactionRepository.findByLoyaltyAccountId(accountId, Pageable.unpaged()).getContent().stream().map(tx -> {
            LoyaltyTransactionDTO dto = new LoyaltyTransactionDTO();
            dto.setId(tx.getId());
            dto.setPoints(tx.getPoints());
            dto.setDescription(tx.getDescription());
            dto.setTransactionType(tx.getTransactionType().name());
            dto.setTransactionDate(tx.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
    
    private void updateMembershipLevel(LoyaltyAccount account) {
        int lifetime = account.getTotalPoints() == null ? 0 : account.getTotalPoints();
        if (lifetime >= 10000) {
            account.setMembershipLevel(MembershipLevel.PLATINUM);
        } else if (lifetime >= 5000) {
            account.setMembershipLevel(MembershipLevel.GOLD);
        } else if (lifetime >= 1000) {
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
        return dto;
    }
}

