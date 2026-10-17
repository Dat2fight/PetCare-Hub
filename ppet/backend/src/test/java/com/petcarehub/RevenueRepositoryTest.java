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
            em.persist(order);
        }
        em.persist(Order.builder().customer(customer).orderNumber("CANCELLED").status(OrderStatus.CANCELLED).paymentStatus(PaymentStatus.COMPLETED).subtotal(BigDecimal.TEN).totalAmount(BigDecimal.TEN).build());
        em.flush();
        var total = orders.summarizeRevenue(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        assertEquals(0, BigDecimal.TEN.compareTo(total.getRevenue())); assertEquals(1, total.getCount());
        assertEquals(0, orders.summarizeRevenue(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)).getCount());
    }
}
