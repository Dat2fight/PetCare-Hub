package com.petcarehub.service.impl;

import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoyaltyPromotionTest {
    @Test void tierUsesLifetimePointsAndRejectsNegativeSpend() {
        var accounts=mock(LoyaltyAccountRepository.class); var transactions=mock(LoyaltyTransactionRepository.class);
        var users=mock(UserRepository.class);
        var service=new LoyaltyServiceImpl(accounts,transactions,users);
        User user=new User();user.setId(1L);
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));
        var account=LoyaltyAccount.builder().customer(user).availablePoints(900).totalPoints(900).build();
        when(accounts.findByCustomerId(1L)).thenReturn(Optional.of(account)); when(accounts.save(account)).thenReturn(account);
        var dto=service.addPoints(1L,100,"Test"); assertEquals("SILVER",dto.getTier()); assertEquals(4000,dto.getPointsToNextTier());
        assertThrows(com.petcarehub.exception.BadRequestException.class,()->service.spendPoints(1L,-1,"Test"));
        assertEquals("SILVER",service.spendPoints(1L,50,"Test").getTier());
    }
    @Test void promotionChecksDatesUsageAndActive() {
        var repository=mock(PromotionRepository.class);var service=new PromotionServiceImpl(repository);
        var promotion=Promotion.builder().active(true).startDate(LocalDateTime.now().minusDays(1)).endDate(LocalDateTime.now().plusDays(1)).currentUses(0).maxUses(1).build();
        when(repository.findByCode("TEST")).thenReturn(Optional.of(promotion));assertTrue(service.validatePromotion("TEST"));
        promotion.setCurrentUses(1);assertFalse(service.validatePromotion("TEST"));promotion.setCurrentUses(0);
        promotion.setEndDate(LocalDateTime.now().minusDays(1));assertFalse(service.validatePromotion("TEST"));
        promotion.setEndDate(LocalDateTime.now().plusDays(1));promotion.setActive(false);assertFalse(service.validatePromotion("TEST"));
    }
}
