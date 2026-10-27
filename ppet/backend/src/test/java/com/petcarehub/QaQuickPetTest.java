package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="app.pets.max-personal-pets=2") @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class QaQuickPetTest {
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired SpeciesRepository species; @Autowired PetRepository pets;
    User owner; Species dog;
    UserDetailsImpl caller(String role) { return new UserDetailsImpl(owner.getId(),owner.getUsername(),owner.getEmail(),"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role))); }
    @BeforeEach void setup() {
        String n="qa_"+UUID.randomUUID();
        owner=users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused").firstName("qa_A").lastName("qa_B").build());
        dog=species.findByName("Dog").orElseGet(()->species.save(Species.builder().name("Dog").build()));
    }
    String body(int age) { return "{\"name\":\"qa_pet\",\"speciesId\":"+dog.getId()+",\"age\":"+age+",\"ownerId\":999,\"price\":999,\"availabilityStatus\":\"AVAILABLE\"}"; }
    @Test void quickCreateIgnoresPrivilegedFieldsAndEnforcesCap() throws Exception {
        for(int i=0;i<2;i++) mvc.perform(post("/api/v1/pets/my-pets").with(user(caller("CUSTOMER"))).contentType("application/json").content(body(2)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(owner.getId())).andExpect(jsonPath("$.age").value(2))
                .andExpect(jsonPath("$.availabilityStatus").value("UNAVAILABLE"));
        mvc.perform(post("/api/v1/pets/my-pets").with(user(caller("CUSTOMER"))).contentType("application/json").content(body(2)))
                .andExpect(status().isConflict());
        var owned=pets.findByOwnerId(owner.getId());assertEquals(2,owned.size());
        for(var pet:owned) {assertEquals(0,pet.getPrice().signum());mvc.perform(get("/api/v1/pets/"+pet.getId())).andExpect(status().isNotFound());}
    }
    @Test void rejectsInvalidAgeAndStaff() throws Exception {
        for(int age:new int[]{-1,31}) mvc.perform(post("/api/v1/pets/my-pets").with(user(caller("CUSTOMER"))).contentType("application/json").content(body(age))).andExpect(status().isBadRequest());
        for(var role:RoleType.values()) if(role!=RoleType.CUSTOMER)
            mvc.perform(post("/api/v1/pets/my-pets").with(user(caller(role.name()))).contentType("application/json").content(body(2))).andExpect(status().isForbidden());
        assertTrue(pets.findByOwnerId(owner.getId()).isEmpty());
    }
}
