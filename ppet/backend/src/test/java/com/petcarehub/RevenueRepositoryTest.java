package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RevenueRepositoryTest {
    @Autowired TestEntityManager em;
    @Autowired OrderRepository orders;
    @Test void sumsOnlyPaidNonCancelledOrdersInRange() {
        User customer = User.builder().username("revenue").email("revenue@example.test").passwordHash("unused").firstName("A").lastName("B").build(); em.persist(customer);
        int i = 0;
        for (PaymentStatus payment : PaymentStatus.values()) {
            Order order = Order.builder().customer(customer).orderNumber("REV-" + i++).status(OrderStatus.DELIVERED).paymentStatus(payment).subtotal(BigDecimal.TEN).totalAmount(BigDecimal.TEN).build();
            order.setPaidAt(LocalDateTime.now()); em.persist(order);
        }
        em.persist(Order.builder().customer(customer).orderNumber("CANCELLED").status(OrderStatus.CANCELLED).paymentStatus(PaymentStatus.COMPLETED).subtotal(BigDecimal.TEN).totalAmount(BigDecimal.TEN).build());
        Order previous=Order.builder().customer(customer).orderNumber("PREVIOUS").status(OrderStatus.DELIVERED).paymentStatus(PaymentStatus.COMPLETED).subtotal(BigDecimal.ONE).totalAmount(BigDecimal.ONE).build();
        previous.setPaidAt(LocalDateTime.now().minusMonths(2)); em.persist(previous); em.flush();
        var earlier=LocalDateTime.now().minusMonths(2);
        em.getEntityManager().createNativeQuery("update orders set created_at=:date where id=:id").setParameter("date",earlier).setParameter("id",previous.getId()).executeUpdate();
        var periods=orders.revenueByMonth(earlier.minusDays(1),LocalDateTime.now().plusDays(1));
        assertEquals(2,periods.size()); assertEquals(0, BigDecimal.ONE.compareTo(periods.get(0).getRevenue()));
        assertEquals(0, BigDecimal.TEN.compareTo(periods.get(1).getRevenue()));
        em.flush();
        var total = orders.summarizeRevenue(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        assertEquals(0, BigDecimal.TEN.compareTo(total.getRevenue())); assertEquals(1, total.getCount());
        assertEquals(0, orders.summarizeRevenue(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)).getCount());
    }
}
