package com.petcarehub.dto.order;

import java.math.BigDecimal;

public record OrderQuote(BigDecimal subtotal, BigDecimal discount, int pointsApplied,
                         BigDecimal pointsDiscount, BigDecimal shippingFee, BigDecimal total,
                         int availablePoints, int maxRedeemablePoints, java.util.List<Line> lines) {
    public record Line(String type, Long id, int quantity, BigDecimal price) {}
    public OrderQuote(BigDecimal subtotal, BigDecimal discount, int pointsApplied, BigDecimal pointsDiscount,
                      BigDecimal shippingFee, BigDecimal total, int availablePoints, int maxRedeemablePoints) {
        this(subtotal,discount,pointsApplied,pointsDiscount,shippingFee,total,availablePoints,maxRedeemablePoints,java.util.List.of());
    }

    public OrderQuote withLines(java.util.List<Line> items) {
        return new OrderQuote(subtotal, discount, pointsApplied, pointsDiscount, shippingFee, total,
                availablePoints, maxRedeemablePoints, java.util.List.copyOf(items));
    }

    public String getQuoteVersion() {
        var prices = lines.stream().map(i -> i.type()+"/"+i.id()+"/"+i.quantity()+"/"+i.price().stripTrailingZeros().toPlainString()).sorted().toList();
        String amounts = java.util.stream.Stream.of(subtotal,discount,pointsDiscount,shippingFee,total)
                .map(n -> n.stripTrailingZeros().toPlainString()).collect(java.util.stream.Collectors.joining("/"));
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest((amounts + "/" + pointsApplied + "/" + prices).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
