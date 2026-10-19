package com.petcarehub;

import com.petcarehub.dto.cart.*;
import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.CartService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:cart-mutation;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc @ActiveProfiles("test")
class CartMutationTest {
    @Autowired UserRepository users; @Autowired ProductRepository products; @Autowired ProductCategoryRepository categories;
    @Autowired InventoryRepository inventory; @Autowired CartService carts; @Autowired MockMvc mvc;
    User owner,other; Product product; long itemId;
    User account() { String n=UUID.randomUUID().toString(); return users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused").firstName("A").lastName("B").build()); }
    AddToCartRequest add(int quantity) { var r=new AddToCartRequest();r.setProductId(product.getId());r.setQuantity(quantity);return r; }
    @BeforeEach void setup() {
        owner=account();other=account(); var category=categories.save(ProductCategory.builder().name("Cart").build());
        product=products.save(Product.builder().category(category).name("Cart product").price(BigDecimal.TEN).sku(UUID.randomUUID().toString()).active(true).build());
        inventory.save(Inventory.builder().product(product).quantity(10).build());
        itemId=carts.addItemToCart(owner.getId(),add(2)).getItems().get(0).getId();
    }
    @Test void updateRemoveAndClearPersistForCurrentCustomer() throws Exception {
        var principal=UserDetailsImpl.build(owner);
        mvc.perform(put("/api/v1/cart/items/{id}",itemId).with(user(principal)).contentType("application/json").content("{\"quantity\":4}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(4));
        assertEquals(4,carts.getCurrentUserCart(owner.getId()).getItems().get(0).getQuantity());
        mvc.perform(delete("/api/v1/cart/items/{id}",itemId).with(user(principal))).andExpect(status().isNoContent());
        assertTrue(carts.getCurrentUserCart(owner.getId()).getItems().isEmpty());
        carts.addItemToCart(owner.getId(),add(1)); carts.addItemToCart(other.getId(),add(1));
        mvc.perform(delete("/api/v1/cart").with(user(principal))).andExpect(status().isNoContent());
        assertTrue(carts.getCurrentUserCart(owner.getId()).getItems().isEmpty());
        assertEquals(1,carts.getCurrentUserCart(other.getId()).getItems().size());
    }
    @Test void foreignItemCannotBeUpdatedOrRemovedAndQueryIdCannotSelectOwner() throws Exception {
        var principal=UserDetailsImpl.build(other);
        mvc.perform(put("/api/v1/cart/items/{id}",itemId).with(user(principal)).contentType("application/json").content("{\"quantity\":3}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/cart/items/{id}",itemId).with(user(principal))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/cart?userId="+owner.getId()).with(user(principal))).andExpect(jsonPath("$.items").isEmpty());
        assertEquals(2,carts.getCurrentUserCart(owner.getId()).getItems().get(0).getQuantity());
    }
    @Test void invalidQuantityAndStockDoNotMutate() throws Exception {
        for(String value:new String[]{"null","0","-1","11"})
            mvc.perform(put("/api/v1/cart/items/{id}",itemId).with(user(UserDetailsImpl.build(owner))).contentType("application/json").content("{\"quantity\":"+value+"}"))
                    .andExpect(status().isBadRequest());
        assertEquals(2,carts.getCurrentUserCart(owner.getId()).getItems().get(0).getQuantity());
    }
    @Test void cartPageReceivesCurrentServerPriceAndPersistedQuantity() throws Exception {
        product.setPrice(new BigDecimal("12.50"));products.save(product);
        mvc.perform(put("/api/v1/cart/items/{id}",itemId).with(user(UserDetailsImpl.build(owner)))
                .contentType("application/json").content("{\"quantity\":3}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/cart").with(user(UserDetailsImpl.build(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].price").value(12.5))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].productName").value("Cart product"));
    }
    @Test void updateAndAddUseSameSerializationBoundary() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        var request=new UpdateCartItemRequest();request.setQuantity(4);
        try {
            var a=pool.submit(()->{start.await();return carts.updateCartItemQuantity(owner.getId(),itemId,request);});
            var b=pool.submit(()->{start.await();return carts.addItemToCart(owner.getId(),add(1));});
            start.countDown();a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);
            var result=carts.getCurrentUserCart(owner.getId());assertEquals(1,result.getItems().size());
            assertTrue(result.getItems().get(0).getQuantity()==4 || result.getItems().get(0).getQuantity()==5);
        } finally {pool.shutdownNow();}
    }
}
