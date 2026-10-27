package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class QaRevenueWindowTest {
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired OrderRepository orders;
    @Autowired SpeciesRepository species; @Autowired PetRepository pets; @Autowired ProductRepository products;
    @Autowired ProductCategoryRepository categories;
    @Test void excessiveAndReversedWindowsAreRejected() throws Exception {
        for(String endpoint:new String[]{"dashboard","revenue-report"}) {
            mvc.perform(get("/api/v1/admin/"+endpoint).with(user("qa_admin").roles("ADMIN"))
                .param("from","2024-01-01").param("to","2026-01-01")).andExpect(status().isBadRequest());
            mvc.perform(get("/api/v1/admin/"+endpoint).with(user("qa_admin").roles("ADMIN"))
                .param("from","2026-02-01").param("to","2026-01-01")).andExpect(status().isBadRequest());
            mvc.perform(get("/api/v1/admin/"+endpoint)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1/admin/"+endpoint).with(user("qa_customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        }
    }
    @Test void codCannotCountBeforeDeliveryEvenIfMarkedPaid() {
        String n="qa_"+UUID.randomUUID();
        var customer=users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused").firstName("qa_A").lastName("qa_B").build());
        orders.saveAndFlush(Order.builder().customer(customer).orderNumber(n).status(OrderStatus.CONFIRMED).paymentMethod("COD").paidAt(LocalDateTime.now())
            .paymentStatus(PaymentStatus.COMPLETED).subtotal(BigDecimal.TEN).totalAmount(BigDecimal.TEN).build());
        assertEquals(0,orders.summarizeRevenue(LocalDateTime.now().minusDays(1),LocalDateTime.now().plusDays(1)).getRevenue().signum());
    }
    @Test void bothReportsMatchHandCalculatedPaidTotalAndEmptyPeriod() throws Exception {
        String n="qa_"+UUID.randomUUID();
        var customer=users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused").firstName("qa_A").lastName("qa_B").build());
        var date=LocalDate.of(2025,4,12);
        var mixed=save(customer, "100.25", OrderStatus.DELIVERED, PaymentStatus.COMPLETED, date.atStartOfDay());
        var kind=species.save(Species.builder().name("qa_"+UUID.randomUUID()).build());
        var pet=pets.save(Pet.builder().name("qa_pet").species(kind).price(new BigDecimal("50")).build());
        var category=categories.save(ProductCategory.builder().name("qa_category").build());
        var product=products.save(Product.builder().name("qa_product").sku("qa_"+UUID.randomUUID()).category(category).price(new BigDecimal("50.25")).build());
        mixed.addItem(OrderItem.builder().itemType(OrderItemType.PET).pet(pet).quantity(1).unitPrice(new BigDecimal("50")).subtotal(new BigDecimal("50")).build());
        mixed.addItem(OrderItem.builder().itemType(OrderItemType.PRODUCT).product(product).quantity(1).unitPrice(new BigDecimal("50.25")).subtotal(new BigDecimal("50.25")).build());
        orders.saveAndFlush(mixed);
        save(customer, "49.75", OrderStatus.DELIVERED, PaymentStatus.COMPLETED, date.atTime(23,59,59));
        save(customer, "999", OrderStatus.PENDING, PaymentStatus.PENDING, date.atTime(12,0));
        save(customer, "999", OrderStatus.CANCELLED, PaymentStatus.COMPLETED, date.atTime(12,0));
        save(customer, "999", OrderStatus.DELIVERED, PaymentStatus.REFUNDED, date.atTime(12,0));
        save(customer, "999", OrderStatus.DELIVERED, PaymentStatus.COMPLETED, date.plusDays(1).atStartOfDay());
        save(customer, "999", OrderStatus.DELIVERED, PaymentStatus.COMPLETED, null);
        for(String endpoint:new String[]{"dashboard","revenue-report"}) {
            mvc.perform(get("/api/v1/admin/"+endpoint).with(user("qa_admin").roles("ADMIN")).param("from",date.toString()).param("to",date.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRevenue").value(150));
            mvc.perform(get("/api/v1/admin/"+endpoint).with(user("qa_admin").roles("ADMIN")).param("from","2020-01-01").param("to","2020-01-02"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRevenue").value(0));
        }
        mvc.perform(get("/api/v1/admin/revenue-report").with(user("qa_admin").roles("ADMIN")).param("from",date.toString()).param("to",date.toString()))
            .andExpect(jsonPath("$.totalOrders").value(2)).andExpect(jsonPath("$.periods[0].revenue").value(150));
    }
    Order save(User customer, String amount, OrderStatus status, PaymentStatus payment, LocalDateTime paidAt) {
        return orders.saveAndFlush(Order.builder().customer(customer).orderNumber("qa_"+UUID.randomUUID()).status(status).paymentStatus(payment)
            .paymentMethod("COD").paidAt(paidAt).subtotal(new BigDecimal(amount)).totalAmount(new BigDecimal(amount)).build());
    }
}
