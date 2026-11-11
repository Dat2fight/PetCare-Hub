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
    Map<String,Object> body(Product product,int qty){return Map.of("shipping",ShippingFixture.valid(), "paymentMethod","COD",
            "userId",b.getId(),"total",0.01,"items",List.of(Map.of("productId",product.getId(),"quantity",qty,"price",0.01)));}
    @BeforeEach void setup(){a=account();b=account();p=product(3);q=product(1);}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Autowired PromotionRepository promotions;
    @Autowired PromotionUsageRepository usages;
    @Autowired LoyaltyAccountRepository balances;

    @Test void quoteDoesNotConsumeBenefitsAndCancellationRestoresThem() throws Exception {
        p.setPrice(new BigDecimal("500000"));
        products.save(p);
        balances.save(LoyaltyAccount.builder().customer(a).availablePoints(1000).totalPoints(1000).build());
        String code = UUID.randomUUID().toString();
        Promotion promo = promotions.save(Promotion.builder().code(code)
                .discountType(com.petcarehub.enums.DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.TEN).maxUses(1).build());
        var payload = new HashMap<>(body(p, 1));
        var key = UUID.randomUUID().toString();
        payload.put("promotionCode", code);
        payload.put("pointsToUse", 500);
        var bytes = json.writeValueAsBytes(payload);
        mvc.perform(post("/api/v1/orders/quote").with(user(caller(a,"CUSTOMER")))
                .contentType("application/json").content(bytes)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(400000)).andExpect(jsonPath("$.shippingFee").value(0));
        assertEquals(0, promotions.findById(promo.getId()).orElseThrow().getCurrentUses());
        var response = mvc.perform(post("/api/v1/orders").header("Idempotency-Key",key).with(user(caller(a,"CUSTOMER")))
                .contentType("application/json").content(bytes)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(400000)).andReturn().getResponse();
        long id = json.readTree(response.getContentAsByteArray()).get("id").asLong();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key",key).with(user(caller(a,"CUSTOMER")))
                .contentType("application/json").content(bytes)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Idempotent-Replay","true"))
                .andExpect(jsonPath("$.id").value(id));
        assertEquals(500, balances.findByCustomerId(a.getId()).orElseThrow().getAvailablePoints());
        mvc.perform(post("/api/v1/orders/quote").with(user(caller(b,"CUSTOMER")))
                .contentType("application/json").content(bytes)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/orders/{id}/cancel", id).with(user(caller(a,"CUSTOMER")))).andExpect(status().isOk());
        assertEquals(1000, balances.findByCustomerId(a.getId()).orElseThrow().getAvailablePoints());
        assertEquals(0, promotions.findById(promo.getId()).orElseThrow().getCurrentUses());
        assertTrue(usages.findByOrderId(id).isEmpty());
    }

    @Test void malformedShippingReportsEachFieldWithoutCreatingOrder() throws Exception {
        var payload = new HashMap<>(body(p, 1));
        payload.put("shipping", Map.of("recipientName", "🐶", "phone", "0123456789", "province", " ",
                "district", "a".repeat(101), "ward", "", "addressLine", "abc", "note", "a".repeat(501)));
        long before = orders.count();
        var response = mvc.perform(post("/api/v1/orders").header("Idempotency-Key",java.util.UUID.randomUUID().toString()).with(user(caller(a,"CUSTOMER")))
                .contentType("application/json").content(json.writeValueAsBytes(payload))).andExpect(status().isBadRequest());
        for (String field : List.of("recipientName","phone","province","district","ward","addressLine","note"))
            response.andExpect(jsonPath("$.fieldErrors['shipping." + field + "']").isString());
        assertEquals(before, orders.count());
    }

    @Test void keyIsMandatoryAndScopedToTheCustomerAndPayload() throws Exception {
        String key = UUID.randomUUID().toString();
        byte[] payload = json.writeValueAsBytes(body(p,1));
        var path = "/api/v1/orders";
        mvc.perform(post(path).with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(payload))
                .andExpect(status().isBadRequest());
        for (var buyer : List.of(a,b))
            mvc.perform(post(path).header("Idempotency-Key",key).with(user(caller(buyer,"CUSTOMER")))
                    .contentType("application/json").content(payload)).andExpect(status().isOk());
        mvc.perform(post(path).header("Idempotency-Key",key).with(user(caller(a,"CUSTOMER")))
                .contentType("application/json").content(json.writeValueAsBytes(body(p,2)))).andExpect(status().isUnprocessableEntity());
        assertEquals(1,inventory.findByProductId(p.getId()).orElseThrow().getQuantity());
    }

    @Test void successfulOrderUsesServerPriceAndRemovesOnlyPurchasedCartQuantity() throws Exception {
        var cart=Cart.builder().customer(a).build();
        cart.addItem(CartItem.builder().product(p).itemType(OrderItemType.PRODUCT).quantity(3).build());
        cart.addItem(CartItem.builder().product(q).itemType(OrderItemType.PRODUCT).quantity(1).build());carts.saveAndFlush(cart);
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key",java.util.UUID.randomUUID().toString()).with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(json.writeValueAsBytes(body(p,2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.userId").value(a.getId()))
                .andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(30020)).andExpect(jsonPath("$.items[0].price").value(10));
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
            mvc.perform(post("/api/v1/orders").header("Idempotency-Key",java.util.UUID.randomUUID().toString()).with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(bad)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key",java.util.UUID.randomUUID().toString()).contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key",java.util.UUID.randomUUID().toString()).with(user(caller(a,"SALES_STAFF"))).contentType("application/json").content(json.writeValueAsBytes(body(p,1))))
                .andExpect(status().isForbidden());
    }
    @Test void stockConflictAndInactiveProductNeverCreateAnOrder() throws Exception {
        long before=orders.count();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key",java.util.UUID.randomUUID().toString()).with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(json.writeValueAsBytes(body(p,4))))
                .andExpect(status().isConflict());
        p.setActive(false);products.save(p);
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key",java.util.UUID.randomUUID().toString()).with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(json.writeValueAsBytes(body(p,1))))
                .andExpect(status().isNotFound());
        assertEquals(before,orders.count());assertEquals(3,inventory.findByProductId(p.getId()).orElseThrow().getQuantity());
    }
    @Test void secondLineFailureRollsBackStockOrderAndCart() throws Exception {
        var cart=Cart.builder().customer(a).build();cart.addItem(CartItem.builder().product(p).itemType(OrderItemType.PRODUCT).quantity(1).build());carts.saveAndFlush(cart);
        var body=new HashMap<>(body(p,1));body.put("items",List.of(Map.of("productId",p.getId(),"quantity",1),Map.of("productId",q.getId(),"quantity",2)));
        long before=orders.count();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key",java.util.UUID.randomUUID().toString()).with(user(caller(a,"CUSTOMER"))).contentType("application/json").content(json.writeValueAsBytes(body)))
                .andExpect(status().isConflict());
        assertEquals(before,orders.count());assertEquals(3,inventory.findByProductId(p.getId()).orElseThrow().getQuantity());
        new TransactionTemplate(transactions).executeWithoutResult(tx->assertEquals(1,carts.findByCustomerId(a.getId()).orElseThrow().getItems().size()));
    }
    @Test void concurrentRetriesReturnTheSameCommittedOrder() throws Exception {
        String key = UUID.randomUUID().toString();
        byte[] payload = json.writeValueAsBytes(body(q, 1));
        var pool = Executors.newFixedThreadPool(3);
        var start = new CountDownLatch(1);
        Callable<Long> checkout = () -> {
            start.await();
            var response = mvc.perform(post("/api/v1/orders").header("Idempotency-Key",key).with(user(caller(a,"CUSTOMER")))
                    .contentType("application/json").content(payload)).andExpect(status().isOk()).andReturn().getResponse();
            return json.readTree(response.getContentAsByteArray()).get("id").asLong();
        };
        long before = orders.count();
        try {
            var first = pool.submit(checkout); var second = pool.submit(checkout); var third = pool.submit(checkout);
            start.countDown();
            assertEquals(first.get(20,TimeUnit.SECONDS), second.get(20,TimeUnit.SECONDS));
            assertEquals(first.get(), third.get(20,TimeUnit.SECONDS));
            assertEquals(before + 1, orders.count());
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void staleQuoteReturns409AndLeavesStockAndCartUntouched() throws Exception {
        var payload = new HashMap<>(body(p,1));
        var quote = mvc.perform(post("/api/v1/orders/quote").with(user(caller(a,"CUSTOMER")))
                .contentType("application/json").content(json.writeValueAsBytes(payload))).andExpect(status().isOk()).andReturn();
        payload.put("quoteVersion",json.readTree(quote.getResponse().getContentAsByteArray()).get("quoteVersion").asText());
        p.setPrice(BigDecimal.valueOf(150)); products.saveAndFlush(p);
        long before = orders.count();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key",UUID.randomUUID().toString()).with(user(caller(a,"CUSTOMER")))
                .contentType("application/json").content(json.writeValueAsBytes(payload))).andExpect(status().isConflict());
        assertEquals(before, orders.count()); assertEquals(3,inventory.findByProductId(p.getId()).orElseThrow().getQuantity());
    }

    @Test void simultaneousOrdersForLastUnitCommitExactlyOnce() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        long before=orders.count();
        java.util.function.Function<User,Callable<Boolean>> buy=u->()->{
            var principal=caller(u,"CUSTOMER");SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
            var item=new OrderItemDTO();item.setProductId(q.getId());item.setQuantity(1);
            var request=new OrderCreateRequest();request.setCheckoutKey(java.util.UUID.randomUUID());request.setItems(List.of(item));request.setShipping(com.petcarehub.ShippingFixture.valid());request.setPaymentMethod("COD");
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
