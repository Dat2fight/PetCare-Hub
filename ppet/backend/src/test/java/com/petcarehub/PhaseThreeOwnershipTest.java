package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class PhaseThreeOwnershipTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ReviewRepository reviews;
    private User account() {
        var n=java.util.UUID.randomUUID().toString();
        return users.save(User.builder().username(n).email(n+"@example.test").passwordHash("test").firstName("QA").lastName("Test").build());
    }
    private org.springframework.test.web.servlet.request.RequestPostProcessor customer(User u) {
        return user(new UserDetailsImpl(u.getId(),u.getUsername(),u.getEmail(),"",java.util.List.of(
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }
    @Autowired ProductRepository products;
    @Autowired ProductCategoryRepository categories;
    @Test void wishlistIsolation() throws Exception {
        var a=account(); var b=account();
        var category=categories.save(ProductCategory.builder().name("Wishlist").build());
        var p=products.save(Product.builder().name("Hạt").price(java.math.BigDecimal.TEN).category(category).sku("wish").build());
        String url="/api/v1/wishlist", body="{\"type\":\"PRODUCT\",\"targetId\":"+p.getId()+"}";
        var first=mvc.perform(post(url).with(customer(a)).contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(first).get("id").asLong();
        mvc.perform(post(url).with(customer(a)).contentType("application/json").content(body)).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get(url).with(customer(b))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(delete(url+"/"+id).with(customer(b))).andExpect(status().isNotFound());
        p.setActive(false); products.save(p);
        mvc.perform(get(url+"?size=1").with(customer(a))).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].available").value(false));
    }
    @Test void foreignReview() throws Exception {
        var a=account(); var b=account();
        var r=reviews.save(Review.builder().customer(a).reviewableType("PRODUCT").reviewableId(1L).rating(4).comment("QA").approved(true).build());
        String url="/api/v1/reviews/me/"+r.getId();
        String change="{\"rating\":5,\"comment\":\"<b>test</b>\"}";
        mvc.perform(put(url).with(customer(b)).contentType("application/json").content(change)).andExpect(status().isNotFound());
        mvc.perform(delete(url).with(customer(b))).andExpect(status().isNotFound());
        mvc.perform(put(url).with(customer(a)).contentType("application/json").content(change)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(get("/api/v1/reviews/"+r.getId())).andExpect(status().isNotFound());
    }
}
