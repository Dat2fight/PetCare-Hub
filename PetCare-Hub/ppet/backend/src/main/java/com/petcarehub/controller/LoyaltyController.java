package com.petcarehub.controller;

import com.petcarehub.dto.loyalty.LoyaltyAccountDTO;
import com.petcarehub.dto.loyalty.LoyaltyTransactionDTO;
import com.petcarehub.service.LoyaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/account/{userId}")
    public ResponseEntity<LoyaltyAccountDTO> getAccount(@PathVariable Long userId) {
        return ResponseEntity.ok(loyaltyService.getAccountByUserId(userId));
    }

    @PostMapping("/account/{userId}/add")
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
        return ResponseEntity.ok(loyaltyService.spendPoints(userId, points, description));
    }

    @GetMapping("/account/{accountId}/transactions")
    public ResponseEntity<List<LoyaltyTransactionDTO>> getTransactions(@PathVariable Long accountId) {
        return ResponseEntity.ok(loyaltyService.getTransactions(accountId));
    }
}
