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

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class StaffRegistrationTest {
    @Autowired MockMvc mvc;
    @MockBean UserRepository users;
    @MockBean RoleRepository roles;

    private String body(RoleType role) {
        return "{\"username\":\"new-account\",\"email\":\"account@example.test\",\"password\":\"" + UUID.randomUUID()
                + "\",\"firstName\":\"Test\",\"lastName\":\"Account\",\"role\":\"" + role + "\"}";
    }
    private void stubRoles() {
        when(roles.findByName(any())).thenAnswer(call -> Optional.of(Role.builder().name(call.getArgument(0)).build()));
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
    }
    @ParameterizedTest @EnumSource(RoleType.class)
    void publicRegistrationAlwaysCustomer(RoleType injected) throws Exception {
        stubRoles();
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body(injected))).andExpect(status().isOk());
        verify(users).save(argThat(u -> u.getRoles().size() == 1 && u.getRoles().iterator().next().getName() == RoleType.CUSTOMER));
    }
    @ParameterizedTest @EnumSource(value=RoleType.class, names={"CUSTOMER", "SALES_STAFF", "WAREHOUSE_STAFF", "VETERINARIAN"})
    void onlyAdminCanProvisionStaff(RoleType caller) throws Exception {
        mvc.perform(post("/api/v1/admin/staff").with(user("caller").roles(caller.name()))
                .contentType("application/json").content(body(RoleType.VETERINARIAN))).andExpect(status().isForbidden());
        verify(users, never()).save(any());
    }
    @ParameterizedTest @EnumSource(value=RoleType.class, names={"SALES_STAFF", "WAREHOUSE_STAFF", "VETERINARIAN"})
    void adminCreatesEmployeeWithoutExposingPassword(RoleType role) throws Exception {
        stubRoles();
        mvc.perform(post("/api/v1/admin/staff").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(body(role))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value(role.name())).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
    @Test void adminCannotCreateAdminThroughEmployeeEndpoint() throws Exception {
        mvc.perform(post("/api/v1/admin/staff").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(body(RoleType.ADMIN))).andExpect(status().isBadRequest());
    }
}
