package com.petcarehub.service.impl;

import com.petcarehub.dto.order.OrderQuote;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutPricing {
    private static final BigDecimal POINT_VALUE = BigDecimal.valueOf(100);
    private final PromotionRepository promotions;
    private final PromotionUsageRepository usages;
    private final LoyaltyAccountRepository accounts;
    private final LoyaltyTransactionRepository ledger;
    private final UserRepository users;
    private final Clock clock;
    @Value("${app.checkout.shipping-fee:30000}") private BigDecimal shippingFee;
    @Value("${app.checkout.free-shipping-from:500000}") private BigDecimal freeShippingFrom;
    @Value("${app.checkout.promotion-per-customer:1}") private int perCustomer;

    OrderQuote calculate(Long userId, BigDecimal subtotal, BigDecimal products, String code, int points, boolean reserve) {
        Promotion promo = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (code != null && !code.isBlank()) {
            code = code.trim();
            promo = (reserve ? promotions.lockByCode(code) : promotions.findByCode(code))
                    .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại"));
            var now = LocalDateTime.now(clock);
            if (!Boolean.TRUE.equals(promo.getActive()) || promo.getStartDate() != null && now.isBefore(promo.getStartDate()) || promo.getEndDate() != null && now.isAfter(promo.getEndDate()))
                throw new BadRequestException("Mã giảm giá ngoài thời hạn sử dụng");
            if (products.signum() == 0 || promo.getMinOrderAmount() != null && products.compareTo(promo.getMinOrderAmount()) < 0)
                throw new BadRequestException("Tiền sản phẩm chưa đủ để áp mã");
            int limit = promo.getMaxUsesPerCustomer() == null ? perCustomer : promo.getMaxUsesPerCustomer();
            if (promo.getMaxUses() != null && promo.getCurrentUses() >= promo.getMaxUses()
                    || usages.countByPromotionIdAndCustomerId(promo.getId(), userId) >= limit)
                throw new BadRequestException("Mã giảm giá đã hết lượt");
            discount = promo.getDiscountType() == DiscountType.PERCENTAGE
                    ? products.multiply(promo.getDiscountValue()).movePointLeft(2) : promo.getDiscountValue();
            discount = discount.min(products).setScale(0, RoundingMode.HALF_UP);
        }
        int balance = accounts.findByCustomerId(userId).map(LoyaltyAccount::getAvailablePoints).orElse(0);
        int max = products.subtract(discount).max(BigDecimal.ZERO).divide(POINT_VALUE.multiply(BigDecimal.valueOf(2)), 0, RoundingMode.DOWN)
                .min(BigDecimal.valueOf(balance)).intValueExact();
        if (points < 0 || points > max) throw new BadRequestException("Số điểm vượt số dư hoặc giới hạn 50% tiền sản phẩm");
        BigDecimal redeemed = POINT_VALUE.multiply(BigDecimal.valueOf(points));
        BigDecimal ship = products.signum() > 0 && subtotal.compareTo(freeShippingFrom) < 0 ? shippingFee : BigDecimal.ZERO;
        return new OrderQuote(subtotal, discount, points, redeemed, ship, subtotal.subtract(discount).subtract(redeemed).add(ship), balance, max);
    }

    void reserve(Order order) {
        if (order.getPromotionCode() != null) {
            var promo = promotions.lockByCode(order.getPromotionCode()).orElseThrow();
            promo.setCurrentUses(Math.addExact(promo.getCurrentUses(), 1));
            usages.save(PromotionUsage.builder().order(order).promotion(promo).customer(order.getCustomer()).build());
        }
        if (order.getPointsUsed() > 0) post(order, order.getPointsUsed(), LoyaltyTransactionType.REDEEM);
    }

    void release(Order order) {
        users.findLockedById(order.getCustomer().getId()).orElseThrow();
        usages.findByOrderId(order.getId()).ifPresent(usage -> {
            var promo = promotions.lockById(usage.getPromotion().getId()).orElseThrow();
            promo.setCurrentUses(promo.getCurrentUses() - 1);
            usages.delete(usage);
        });
        if (order.getPointsUsed() != null && order.getPointsUsed() > 0) post(order, order.getPointsUsed(), LoyaltyTransactionType.ADJUST);
    }

    void earn(Order order) {
        if (order.getPointsUsed() == null) return; // Historical orders predate this reward policy.
        var eligible = order.getItems().stream().filter(i -> i.getProduct() != null)
                .map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int points = eligible.subtract(order.getDiscountAmount()).subtract(order.getPointsDiscount())
                .divideToIntegralValue(BigDecimal.valueOf(10000)).intValueExact();
        if (points > 0) post(order, points, LoyaltyTransactionType.EARN);
    }

    private void post(Order order, int points, LoyaltyTransactionType type) {
        var user = users.findLockedById(order.getCustomer().getId()).orElseThrow();
        var account = accounts.findByCustomerId(user.getId()).orElseGet(() ->
                accounts.save(LoyaltyAccount.builder().customer(user).build()));
        int delta = type == LoyaltyTransactionType.REDEEM ? -points : points;
        account.setAvailablePoints(Math.addExact(account.getAvailablePoints(), delta));
        if (type == LoyaltyTransactionType.EARN) {
            account.setTotalPoints(Math.addExact(account.getTotalPoints(), points));
            LoyaltyServiceImpl.updateMembershipLevel(account);
        }
        ledger.save(LoyaltyTransaction.builder().loyaltyAccount(account).transactionType(type)
                .points(points).sourceType(LoyaltySourceType.PURCHASE).sourceId(order.getId())
                .description("Đơn " + order.getOrderNumber()).build());
    }
}
