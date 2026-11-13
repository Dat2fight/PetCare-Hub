package com.petcarehub;

import com.petcarehub.entity.Role;
import com.petcarehub.entity.User;
import com.petcarehub.enums.RoleType;
import com.petcarehub.repository.RoleRepository;
import com.petcarehub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @org.springframework.transaction.annotation.Transactional
class StaffRegistrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;

    private String body(RoleType role) {
        return "{\"username\":\"new-account\",\"email\":\"account@example.test\",\"password\":\"" + UUID.randomUUID()
                + "\",\"firstName\":\"Test\",\"lastName\":\"Account\",\"role\":\"" + role + "\"}";
    }
    private void stubRoles() {
        for(var type:RoleType.values()) if(roles.findByName(type).isEmpty()) roles.save(Role.builder().name(type).build());
    }
    private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        stubRoles();
        var actor=users.save(User.builder().username("staff_test_admin").email("staff_test_admin@example.test")
            .firstName("Test").lastName("Admin").passwordHash("unused").roles(java.util.Set.of(roles.findByName(RoleType.ADMIN).orElseThrow())).build());
        return user(com.petcarehub.security.UserDetailsImpl.build(actor));
    }
    @ParameterizedTest @EnumSource(RoleType.class)
    void publicRegistrationAlwaysCustomer(RoleType injected) throws Exception {
        stubRoles();
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body(injected))).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of(RoleType.CUSTOMER), users.findByUsername("new-account").orElseThrow().getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
    }
    @ParameterizedTest @EnumSource(value=RoleType.class, names={"CUSTOMER", "SALES_STAFF", "WAREHOUSE_STAFF", "VETERINARIAN"})
    void onlyAdminCanProvisionStaff(RoleType caller) throws Exception {
        mvc.perform(post("/api/v1/admin/staff").with(user("caller").roles(caller.name()))
                .contentType("application/json").content(body(RoleType.VETERINARIAN))).andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertFalse(users.existsByUsername("new-account"));
    }
    @ParameterizedTest @EnumSource(value=RoleType.class, names={"SALES_STAFF", "WAREHOUSE_STAFF", "VETERINARIAN"})
    void adminCreatesEmployeeWithoutExposingPassword(RoleType role) throws Exception {
        stubRoles();
        mvc.perform(post("/api/v1/admin/staff").with(admin())
                .contentType("application/json").content(body(role))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.roles[0]").value(role.name())).andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty()).andExpect(jsonPath("$.user.mustChangePassword").value(true));
    }
    @Test void adminCannotCreateAdminThroughEmployeeEndpoint() throws Exception {
        mvc.perform(post("/api/v1/admin/staff").with(admin())
                .contentType("application/json").content(body(RoleType.ADMIN))).andExpect(status().isBadRequest());
    }
}
