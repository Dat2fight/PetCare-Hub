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
    @Test void soldWishlistPetStaysUnavailableWithoutDisclosingPrivateProfile() throws Exception {
        var customer=account(); var newOwner=account();
        var type=species.save(Species.builder().name("Wishlist species").build());
        for(String name:java.util.List.of("Mun","Miu")) {
            var pet=pets.save(Pet.builder().name(name).species(type).price(java.math.BigDecimal.TEN).build());
            var body="{\"type\":\"PET\",\"targetId\":"+pet.getId()+"}";
            mvc.perform(post("/api/v1/wishlist").with(customer(customer)).contentType("application/json").content(body)).andExpect(status().isOk());
            mvc.perform(get("/api/v1/wishlist/status").with(customer(customer)).param("type","PET").param("ids",pet.getId().toString()))
                    .andExpect(jsonPath("$[0].targetId").value(pet.getId()));
            pet.setOwner(newOwner);pet.setName("Private name");pets.saveAndFlush(pet);
            mvc.perform(post("/api/v1/wishlist").with(customer(newOwner)).contentType("application/json").content(body)).andExpect(status().isNotFound());
        }
        var first=mvc.perform(get("/api/v1/wishlist?size=1").with(customer(customer)))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content[0].available").value(false))
                .andExpect(jsonPath("$.content[0].name").value("Thú cưng đã ngừng bán")).andReturn().getResponse().getContentAsString();
        var next=mvc.perform(get("/api/v1/wishlist?size=1&page=1").with(customer(customer))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        org.junit.jupiter.api.Assertions.assertNotEquals(mapper.readTree(first).at("/content/0/id"),mapper.readTree(next).at("/content/0/id"));
    }
    @Autowired SpeciesRepository species;
    @Autowired PetRepository pets;
    @Autowired CarePackageRepository plans;
    @Autowired CarePackageSubscriptionRepository subscriptions;
    @Test void cashActivatesPackageOnceAndOnlyForOwner() throws Exception {
        var owner=account(); var other=account();
        var pet=pets.save(Pet.builder().name("Miu").owner(owner).price(java.math.BigDecimal.ZERO)
                .species(species.save(Species.builder().name("Package species").build())).build());
        var plan=plans.save(CarePackage.builder().name("Khám định kỳ").price(java.math.BigDecimal.TEN)
                .durationMonths(3).includesCheckup(true).build());
        var key=java.util.UUID.randomUUID();
        String orders="/api/v1/orders", mine="/api/v1/care-packages/subscriptions/me";
        String body="{\"checkoutKey\":\""+key+"\",\"items\":[{\"quantity\":1,\"carePackageId\":"+plan.getId()+",\"beneficiaryPetId\":"+pet.getId()+"}]}";
        mvc.perform(post(orders).with(customer(other)).contentType("application/json").content(body)).andExpect(status().isNotFound());
        mvc.perform(post(orders+"/quote").with(customer(owner)).contentType("application/json").content(body)).andExpect(jsonPath("$.shippingFee").value(0));
        String result=mvc.perform(post(orders).with(customer(owner)).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPrice").value(10)).andReturn().getResponse().getContentAsString();
        long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result).get("id").asLong();
        var cancelled=mvc.perform(post(orders).with(customer(owner)).contentType("application/json")
                .content(body.replace(key.toString(),java.util.UUID.randomUUID().toString()))).andReturn().getResponse().getContentAsString();
        long cancelledId=new com.fasterxml.jackson.databind.ObjectMapper().readTree(cancelled).get("id").asLong();
        mvc.perform(post(orders+"/"+cancelledId+"/cancel").with(customer(owner))).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post(orders+"/"+cancelledId+"/deliver").with(user("staff").roles("ADMIN"))).andExpect(status().isConflict());
        plan.setName("Tên mới"); plan.setActive(false); plans.saveAndFlush(plan);
        mvc.perform(post(orders).with(customer(owner)).contentType("application/json").content(body)).andExpect(jsonPath("$.id").value(id));
        mvc.perform(post(orders).with(customer(owner)).contentType("application/json").content(body.replace(key.toString(),java.util.UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        mvc.perform(get(mine).with(customer(owner))).andExpect(jsonPath("$.length()").value(0));
        for(String action:java.util.List.of("confirm","process","ship","deliver","deliver"))
            mvc.perform(post(orders+"/"+id+"/"+action).with(user("staff").roles("SALES_STAFF"))).andExpect(status().isOk());
        mvc.perform(get(mine).with(customer(owner))).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Khám định kỳ")).andExpect(jsonPath("$[0].includesCheckup").value(true))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        mvc.perform(get(mine).with(customer(other))).andExpect(jsonPath("$.length()").value(0));
        var sub=subscriptions.findByCustomerId(owner.getId()).get(0);
        org.junit.jupiter.api.Assertions.assertEquals(sub.getStartDate().plusMonths(3).minusDays(1),sub.getEndDate());
        sub.setStartDate(java.time.LocalDate.of(2020,1,1));sub.setEndDate(java.time.LocalDate.of(2020,3,31)); subscriptions.saveAndFlush(sub);
        mvc.perform(get(mine).with(customer(owner))).andExpect(jsonPath("$[0].status").value("EXPIRED"));
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
