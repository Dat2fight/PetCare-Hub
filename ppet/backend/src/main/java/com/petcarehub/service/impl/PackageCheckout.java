package com.petcarehub.service.impl;

import com.petcarehub.dto.order.*;
import com.petcarehub.entity.*;
import com.petcarehub.enums.OrderItemType;
import com.petcarehub.exception.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.Ownership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component @RequiredArgsConstructor
class PackageCheckout {
    private final CarePackageRepository plans;
    private final CarePackageSubscriptionRepository subscriptions;
    private final Ownership ownership;

    static boolean requested(QuoteRequest request) {
        return request.getItems()!=null && request.getItems().stream().anyMatch(i -> i!=null && i.getCarePackageId()!=null);
    }

    OrderItem prepare(QuoteRequest request, boolean lock) {
        if (request.getItems().size()!=1 || request.getPointsToUse()!=0 ||
                (request.getPromotionCode()!=null && !request.getPromotionCode().isBlank()))
            throw new BadRequestException("Gói chăm sóc cần đặt riêng và thanh toán toàn bộ bằng tiền mặt");
        var line=request.getItems().get(0);
        if (!line.isItemReferenceValid()) throw new BadRequestException("Gói chăm sóc hoặc thú cưng không hợp lệ");
        var pet=ownership.ownedPet(line.getBeneficiaryPetId());
        var plan=(lock ? plans.findLockedById(line.getCarePackageId()) : plans.findById(line.getCarePackageId()))
                .filter(p -> Boolean.TRUE.equals(p.getActive())).orElseThrow(()->new ResourceNotFoundException("Gói đã ngừng bán"));
        return OrderItem.builder().itemType(OrderItemType.CARE_PACKAGE).beneficiaryPet(pet).carePackage(plan)
                .packageName(plan.getName()).packageMonths(plan.getDurationMonths()).packageGrooming(plan.getIncludesGrooming())
                .packageCheckup(plan.getIncludesCheckup()).packageVaccination(plan.getIncludesVaccination())
                .quantity(1).unitPrice(plan.getPrice()).subtotal(plan.getPrice()).build();
    }

    OrderQuote quote(OrderItem line) {
        var zero=BigDecimal.ZERO;
        return new OrderQuote(line.getSubtotal(),zero,0,zero,zero,line.getSubtotal(),0,0);
    }

    void activate(Order order, OrderItem line) {
        var start=order.getPaidAt().toLocalDate();
        subscriptions.save(CarePackageSubscription.builder().customer(order.getCustomer()).pet(line.getBeneficiaryPet())
                .carePackage(line.getCarePackage()).orderItem(line).startDate(start)
                .endDate(start.plusMonths(line.getPackageMonths()).minusDays(1)).autoRenew(false).build());
    }
}
