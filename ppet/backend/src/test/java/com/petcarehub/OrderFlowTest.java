package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.dto.order.*;
import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.OrderService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:order-flow;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class OrderFlowTest {
    @Autowired UserRepository users; @Autowired ProductRepository products; @Autowired ProductCategoryRepository categories;
    @Autowired InventoryRepository inventory; @Autowired OrderRepository orders; @Autowired OrderService service;
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    User owner, other; Product product; OrderCreateRequest request;
    User account() { String n=UUID.randomUUID().toString(); return users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused").firstName("A").lastName("B").build()); }
    UserDetailsImpl caller(User u, String role) { return new UserDetailsImpl(u.getId(),u.getUsername(),u.getEmail(),"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role))); }
    void authenticate(User u,String role) { var p=caller(u,role); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,p.getAuthorities())); }
    @BeforeEach void setup() {
        owner=account(); other=account(); authenticate(owner,"CUSTOMER");
        var category=categories.save(ProductCategory.builder().name("Flow").build());
        product=products.save(Product.builder().category(category).name("Food").sku(UUID.randomUUID().toString()).price(BigDecimal.TEN).active(true).build());
        inventory.save(Inventory.builder().product(product).quantity(5).build());
        var item=new OrderItemDTO(); item.setProductId(product.getId()); item.setQuantity(2); item.setPrice(0.01);
        request=new OrderCreateRequest(); request.setUserId(other.getId()); request.setItems(List.of(item)); request.setShippingAddress("Recipient, phone, address"); request.setPaymentMethod("COD");
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void persistsServerPriceAndProtectedDetail() throws Exception {
        var order=service.createOrder(request);
        assertEquals(owner.getId(),order.getUserId()); assertEquals(20.0,order.getTotalPrice());
        assertEquals(3,inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
        mvc.perform(get("/api/v1/orders/{id}",order.getId()).with(user(caller(owner,"CUSTOMER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].price").value(10))
                .andExpect(jsonPath("$.shippingAddress").value(request.getShippingAddress())).andExpect(jsonPath("$.paymentStatus").value("PENDING"));
        mvc.perform(get("/api/v1/orders/{id}",order.getId()).with(user(caller(other,"CUSTOMER")))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/orders/{id}/cancel",order.getId()).with(user(caller(other,"CUSTOMER")))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/orders/{id}",order.getId()).with(user(caller(other,"SALES_STAFF")))).andExpect(status().isOk());
    }
    @Test void transitionsAndPaidRevenueRemainConsistent() throws Exception {
        var order=service.createOrder(request);
        mvc.perform(post("/api/v1/orders/{id}/confirm",order.getId()).with(user(caller(owner,"CUSTOMER")))).andExpect(status().isForbidden());
        authenticate(other,"SALES_STAFF");
        assertThrows(com.petcarehub.exception.InvalidStateTransitionException.class,()->service.shipOrder(order.getId()));
        service.confirmOrder(order.getId()); service.processOrder(order.getId()); service.shipOrder(order.getId());
        var delivered=service.deliverOrder(order.getId()); assertEquals("DELIVERED",delivered.getStatus()); assertEquals("COMPLETED",delivered.getPaymentStatus());
        assertTrue(orders.summarizeRevenue(LocalDateTime.now().minusDays(1),LocalDateTime.now().plusDays(1)).getRevenue().compareTo(BigDecimal.valueOf(20))>=0);
        authenticate(owner,"CUSTOMER"); assertThrows(com.petcarehub.exception.InvalidStateTransitionException.class,()->service.cancelOrder(order.getId()));
    }
    @Test void cancellationRestoresStockOnlyOnce() {
        var order=service.createOrder(request); service.cancelOrder(order.getId());
        assertEquals(5,inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
        assertThrows(com.petcarehub.exception.InvalidStateTransitionException.class,()->service.cancelOrder(order.getId()));
        assertEquals(5,inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
    }
    @Test void insufficientSecondLineRollsBackAllStockAndOrder() {
        var second=new OrderItemDTO(); second.setProductId(product.getId()); second.setQuantity(4);
        request.setItems(List.of(request.getItems().get(0),second)); long before=orders.count();
        assertThrows(IllegalStateException.class,()->service.createOrder(request));
        assertEquals(before,orders.count()); assertEquals(5,inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
    }
    @Test void checkoutResponseCanBeLoadedByItsRealId() throws Exception {
        var response=mvc.perform(post("/api/v1/orders").with(user(caller(owner,"CUSTOMER")))
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn();
        long id=json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/v1/orders/{id}",id).with(user(caller(owner,"CUSTOMER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.shippingAddress").value(request.getShippingAddress()))
                .andExpect(jsonPath("$.items[0].productName").value("Food"));
        long before=orders.count(); request.setShippingAddress(" ");
        mvc.perform(post("/api/v1/orders").with(user(caller(owner,"CUSTOMER")))
                .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
        assertEquals(before,orders.count());
    }
    @Test void missingOrderIs404AndUnsupportedPaymentIs400() throws Exception {
        mvc.perform(get("/api/v1/orders/9223372036854775807").with(user(caller(owner,"CUSTOMER")))).andExpect(status().isNotFound());
        request.setPaymentMethod("CARD");
        mvc.perform(post("/api/v1/orders").with(user(caller(owner,"CUSTOMER"))).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
        assertEquals(5,inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
    }
}
