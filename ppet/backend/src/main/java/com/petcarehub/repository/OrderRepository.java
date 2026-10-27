package com.petcarehub.repository;

import com.petcarehub.entity.Order;
import com.petcarehub.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.id = :id and o.customer.id = :customerId")
    Optional<Order> findOwnedLockedById(Long id, Long customerId);
    String REVENUE_FILTER = " o.paymentStatus = com.petcarehub.enums.PaymentStatus.COMPLETED and o.status <> com.petcarehub.enums.OrderStatus.CANCELLED and (o.paymentMethod is null or o.paymentMethod <> 'COD' or o.status = com.petcarehub.enums.OrderStatus.DELIVERED) and o.paidAt >= :from and o.paidAt < :to";
    interface RevenuePeriod { Integer getYear(); Integer getMonth(); java.math.BigDecimal getRevenue(); long getCount(); }
    @org.springframework.data.jpa.repository.Query("select year(o.paidAt) as year, month(o.paidAt) as month, coalesce(sum(o.totalAmount), 0) as revenue, count(o) as count from Order o where" + REVENUE_FILTER + " group by year(o.paidAt), month(o.paidAt) order by year(o.paidAt), month(o.paidAt)")
    java.util.List<RevenuePeriod> revenueByMonth(LocalDateTime from, LocalDateTime to);
    interface RevenueTotal { java.math.BigDecimal getRevenue(); long getCount(); }
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(o.totalAmount), 0) as revenue, count(o) as count from Order o where" + REVENUE_FILTER)
    RevenueTotal summarizeRevenue(LocalDateTime from, LocalDateTime to);
    @org.springframework.data.jpa.repository.Query("select count(i) > 0 from OrderItem i where i.order.customer.id = :userId and i.order.status = com.petcarehub.enums.OrderStatus.DELIVERED and ((:type = 'PRODUCT' and i.product.id = :targetId) or (:type = 'PET' and i.pet.id = :targetId))")
    boolean hasDeliveredTarget(Long userId, String type, Long targetId);
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.id = :id")
    Optional<Order> findLockedById(Long id);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    Page<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);
}
