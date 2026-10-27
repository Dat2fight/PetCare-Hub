package com.petcarehub;

import com.petcarehub.dto.order.*;
import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CheckoutPersistenceTest {
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired ProductCategoryRepository categories;
    @Autowired InventoryRepository inventory;
    @Autowired OrderRepository orders;
    @Autowired OrderService service;
    @Test void persistsServerPriceReservesAndRestoresStock() {
        User customer = users.save(User.builder().username("checkout").email("checkout@example.test").passwordHash("unused").firstName("A").lastName("B").build());
        var principal = new UserDetailsImpl(customer.getId(),customer.getUsername(),customer.getEmail(),"unused",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER")));
        var principal = new UserDetailsImpl(customer.getId(), customer.getUsername(), customer.getEmail(), "unused", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            var category = categories.save(ProductCategory.builder().name("Food").build());
            Product product = products.save(Product.builder().category(category).name("Food").sku("CHECKOUT").price(BigDecimal.TEN).active(true).build());
            inventory.save(Inventory.builder().product(product).quantity(3).build());
            OrderItemDTO item = new OrderItemDTO(); item.setProductId(product.getId()); item.setQuantity(2); item.setPrice(0.01);
            OrderCreateRequest request = new OrderCreateRequest(); request.setItems(List.of(item)); request.setShippingAddress("Địa chỉ thử nghiệm");
            OrderDTO result = service.createOrder(request);
            assertNotNull(result.getId()); assertEquals(customer.getId(), result.getUserId()); assertEquals(20.0, result.getTotalPrice());
            assertEquals(1, inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
            assertEquals(1, orders.findById(result.getId()).orElseThrow().getItems().size());
            service.cancelOrder(result.getId());
            assertEquals(3, inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
            assertThrows(com.petcarehub.exception.InvalidStateTransitionException.class, () -> service.cancelOrder(result.getId()));
        } finally { SecurityContextHolder.clearContext(); }
    }
}
