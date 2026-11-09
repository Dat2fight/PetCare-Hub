package com.petcarehub.dto.order;

import java.math.BigDecimal;

public record OrderQuote(BigDecimal subtotal, BigDecimal discount, int pointsApplied,
                         BigDecimal pointsDiscount, BigDecimal shippingFee, BigDecimal total,
                         int availablePoints, int maxRedeemablePoints) {}
