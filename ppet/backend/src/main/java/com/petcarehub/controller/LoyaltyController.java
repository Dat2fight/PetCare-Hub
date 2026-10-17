package com.petcarehub.controller;

import com.petcarehub.dto.loyalty.LoyaltyAccountDTO;
import com.petcarehub.dto.loyalty.LoyaltyTransactionDTO;
import com.petcarehub.service.LoyaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final com.petcarehub.security.Ownership ownership;
    private final com.petcarehub.repository.LoyaltyAccountRepository accounts;

    @GetMapping("/account/{userId}")
    public ResponseEntity<LoyaltyAccountDTO> getAccount(@PathVariable Long userId) {
        ownership.requireUser(userId);
        return ResponseEntity.ok(loyaltyService.getAccountByUserId(userId));
    }

    @PostMapping("/account/{userId}/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoyaltyAccountDTO> addPoints(
            @PathVariable Long userId,
            @RequestParam Integer points,
            @RequestParam String description) {
        return ResponseEntity.ok(loyaltyService.addPoints(userId, points, description));
    }

    @PostMapping("/account/{userId}/spend")
    public ResponseEntity<LoyaltyAccountDTO> spendPoints(
            @PathVariable Long userId,
            @RequestParam Integer points,
            @RequestParam String description) {
        ownership.requireUser(userId);
        return ResponseEntity.ok(loyaltyService.spendPoints(userId, points, description));
    }

    @GetMapping("/account/{accountId}/transactions")
    public ResponseEntity<List<LoyaltyTransactionDTO>> getTransactions(@PathVariable Long accountId, org.springframework.data.domain.Pageable pageable) {
        ownership.requireUser(accounts.findById(accountId).orElseThrow(() -> new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy tài khoản điểm")).getCustomer().getId());
        return ResponseEntity.ok(loyaltyService.getTransactions(accountId, pageable));
    }
}
