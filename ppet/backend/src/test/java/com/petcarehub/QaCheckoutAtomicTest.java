package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.dto.order.*;
import com.petcarehub.entity.*;
import com.petcarehub.enums.OrderItemType;
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
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:qa-checkout;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc @ActiveProfiles("test")
class QaCheckoutAtomicTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users;
    @Autowired ProductRepository products; @Autowired ProductCategoryRepository categories; @Autowired InventoryRepository inventory;
    @Autowired OrderRepository orders; @Autowired CartRepository carts; @Autowired OrderService service;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    User a,b; Product p,q;
    User account() {String n="qa_"+UUID.randomUUID();return users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused").firstName("qa_A").lastName("qa_B").build());}
    UserDetailsImpl caller(User u,String role){return new UserDetailsImpl(u.getId(),u.getUsername(),u.getEmail(),"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role)));}
    Product product(int stock) {
        var c=categories.save(ProductCategory.builder().name("qa_food").build());
        var p=products.save(Product.builder().category(c).name("qa_food").sku("qa_"+UUID.randomUUID()).price(BigDecimal.TEN).active(true).build());
        inventory.save(Inventory.builder().product(p).quantity(stock).build());return p;
    }
    Map<String,Object> body(Product product,int qty){return Map.of("shippingAddress","qa_Người nhận, qa_000, qa_Địa chỉ", "paymentMethod","COD",
            "userId",b.getId(),"total",0.01,"items",List.of(Map.of("productId",product.getId(),"quantity",qty,"price",0.01)));}
    @BeforeEach void setup(){a=account();b=account();p=product(3);q=product(1);}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void successfulOrderUsesServerPriceAndRemovesOnlyPurchasedCartQuantity() throws Exception {
        var cart=Cart.builder().customer(a).build();
        cart.addItem(CartItem.builder().product(p).itemType(OrderItemType.PRODUCT).quantity(3).build());
        cart.addItem(CartItem.builder().product(q).itemType(OrderItemType.PRODUCT).quantity(1).build());carts.saveAndFlush(cart);
        mvc.perform(post("/api/v1/orders").with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(json.writeValueAsBytes(body(p,2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.userId").value(a.getId()))
                .andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(20)).andExpect(jsonPath("$.items[0].price").value(10));
        new TransactionTemplate(transactions).executeWithoutResult(tx->{
            var remaining=carts.findByCustomerId(a.getId()).orElseThrow().getItems();
            assertEquals(2,remaining.size());
            assertEquals(1,remaining.stream().filter(i->i.getProduct().getId().equals(p.getId())).findFirst().orElseThrow().getQuantity());
            assertEquals(1,remaining.stream().filter(i->i.getProduct().getId().equals(q.getId())).findFirst().orElseThrow().getQuantity());
        });
    }
    @Test void emptyInvalidOrUnsupportedItemsReturn400AndNonCustomerCannotOrder() throws Exception {
        for(String bad:List.of("{}","{\"items\":[]}","{\"shippingAddress\":\"qa_address\",\"items\":[{\"productId\":"+p.getId()+",\"quantity\":0}]}",
                "{\"items\":[{\"productId\":"+p.getId()+",\"quantity\":1}]}",
                "{\"shippingAddress\":\"qa_address\",\"items\":[{\"productId\":"+p.getId()+",\"petId\":1,\"quantity\":1}]}"))
            mvc.perform(post("/api/v1/orders").with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(bad)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/orders").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/orders").with(user(caller(a,"SALES_STAFF"))).contentType("application/json").content(json.writeValueAsBytes(body(p,1))))
                .andExpect(status().isForbidden());
    }
    @Test void stockConflictAndInactiveProductNeverCreateAnOrder() throws Exception {
        long before=orders.count();
        mvc.perform(post("/api/v1/orders").with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(json.writeValueAsBytes(body(p,4))))
                .andExpect(status().isConflict());
        p.setActive(false);products.save(p);
        mvc.perform(post("/api/v1/orders").with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(json.writeValueAsBytes(body(p,1))))
                .andExpect(status().isNotFound());
        assertEquals(before,orders.count());assertEquals(3,inventory.findByProductId(p.getId()).orElseThrow().getQuantity());
    }
    @Test void secondLineFailureRollsBackStockOrderAndCart() throws Exception {
        var cart=Cart.builder().customer(a).build();cart.addItem(CartItem.builder().product(p).itemType(OrderItemType.PRODUCT).quantity(1).build());carts.saveAndFlush(cart);
        var body=new HashMap<>(body(p,1));body.put("items",List.of(Map.of("productId",p.getId(),"quantity",1),Map.of("productId",q.getId(),"quantity",2)));
        long before=orders.count();
        mvc.perform(post("/api/v1/orders").with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(json.writeValueAsBytes(body)))
                .andExpect(status().isConflict());
        assertEquals(before,orders.count());assertEquals(3,inventory.findByProductId(p.getId()).orElseThrow().getQuantity());
        new TransactionTemplate(transactions).executeWithoutResult(tx->assertEquals(1,carts.findByCustomerId(a.getId()).orElseThrow().getItems().size()));
    }
    @Test void simultaneousOrdersForLastUnitCommitExactlyOnce() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        long before=orders.count();
        java.util.function.Function<User,Callable<Boolean>> buy=u->()->{
            var principal=caller(u,"CUSTOMER");SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
            var item=new OrderItemDTO();item.setProductId(q.getId());item.setQuantity(1);
            var request=new OrderCreateRequest();request.setItems(List.of(item));request.setShippingAddress("qa_address");request.setPaymentMethod("COD");
            ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));
            try {service.createOrder(request);return true;}catch(IllegalStateException conflict){return false;}finally{SecurityContextHolder.clearContext();}
        };
        try {
            var first=pool.submit(buy.apply(a));var second=pool.submit(buy.apply(b));assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();
            assertEquals(1,(first.get(20,TimeUnit.SECONDS)?1:0)+(second.get(20,TimeUnit.SECONDS)?1:0));
            assertEquals(before+1,orders.count());assertEquals(0,inventory.findByProductId(q.getId()).orElseThrow().getQuantity());
        } finally {start.countDown();pool.shutdownNow();}
    }
}
