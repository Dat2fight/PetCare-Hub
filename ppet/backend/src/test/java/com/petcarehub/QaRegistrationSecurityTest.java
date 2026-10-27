package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.entity.*;
import com.petcarehub.enums.RoleType;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class QaRegistrationSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @BeforeEach void seedRoles() {
        for (var type : RoleType.values())
            if (roles.findByName(type).isEmpty()) roles.save(Role.builder().name(type).build());
    }
    @ParameterizedTest @ValueSource(strings={"SALES_STAFF","ADMIN","VETERINARIAN","qa_unknown"})
    void injectedRoleIsIgnoredAndRealLoginHasOnlyCustomerAuthority(String injected) throws Exception {
        String name="qa_"+UUID.randomUUID();
        String password=UUID.randomUUID().toString();
        var body=new HashMap<String,Object>(Map.of("username",name,"email",name+"@example.test",
                "password",password,"firstName","qa_First","lastName","qa_Last","role",injected));
        body.put("enabled",false); body.put("roles",List.of("ADMIN")); body.put("points",999999); body.put("tier","PLATINUM");
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(json.writeValueAsBytes(body)))
                .andExpect(status().isOk());
        User saved=users.findByUsername(name).orElseThrow();
        assertEquals(Set.of(RoleType.CUSTOMER), saved.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
        assertTrue(saved.getEnabled());
        var login=mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("username",name,"password",password))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"))
                .andReturn().getResponse().getContentAsString();
        String token=json.readTree(login).path("token").asText();
        assertFalse(token.isBlank());
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/users/{id}/role",saved.getId()).header("Authorization","Bearer "+token)
                .param("role","ADMIN")).andExpect(status().isForbidden());
    }
    @Test void existingAdminCannotBeDemotedThroughGenericRoleEndpoint() throws Exception {
        User admin=users.save(User.builder().username("qa_admin").email("qa_admin@example.test").passwordHash("unused")
                .firstName("qa_A").lastName("qa_B").roles(new HashSet<>(Set.of(roles.findByName(RoleType.ADMIN).orElseThrow()))).build());
        mvc.perform(patch("/api/v1/admin/users/{id}/role",admin.getId()).with(user(UserDetailsImpl.build(admin)))
                .param("role","CUSTOMER")).andExpect(status().isForbidden());
        assertTrue(users.findById(admin.getId()).orElseThrow().getRoles().stream().anyMatch(r->r.getName()==RoleType.ADMIN));
    }
}
