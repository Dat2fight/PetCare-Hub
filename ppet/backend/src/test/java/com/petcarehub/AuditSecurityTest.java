package com.petcarehub;

import com.petcarehub.config.JwtProperties;
import com.petcarehub.entity.User;
import com.petcarehub.enums.RoleType;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.security.JwtTokenProvider;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.PetService;
import com.petcarehub.service.ServiceManagementService;
import com.petcarehub.service.TreatmentService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider tokens;
    @Autowired JwtProperties properties;
    @MockBean UserRepository users;
    @MockBean PetService pets;
    @MockBean com.petcarehub.repository.NotificationRepository notifications;
    @MockBean ServiceManagementService services;
    @MockBean TreatmentService treatments;

    @Test void anonymousPersonalEndpointsReturn401() throws Exception {
        for (String path : List.of("/api/v1/auth/me", "/api/v1/pets/my-pets")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test void validJwtReturnsPersonalDataAndUsesConfiguredExpiry() throws Exception {
        User account = User.builder().username("audit").email("audit@example.test").passwordHash("unused").build();
        account.setId(1L);
        when(users.findByUsername("audit")).thenReturn(Optional.of(account));
        var principal = UserDetailsImpl.build(account);
        String jwt = tokens.generateToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(jwt).getPayload();
        assertEquals(properties.getExpirationMs(), claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
        for (String path : List.of("/api/v1/auth/me", "/api/v1/pets/my-pets")) {
            mvc.perform(get(path).header("Authorization", "Bearer " + jwt)).andExpect(status().isOk());
        }
    }

    @ParameterizedTest @EnumSource(RoleType.class)
    void roleMatrix(RoleType role) throws Exception {
        int commerce = role == RoleType.ADMIN || role == RoleType.SALES_STAFF ? 204 : 403;
        int medical = role == RoleType.ADMIN || role == RoleType.VETERINARIAN ? 204 : 403;
        mvc.perform(delete("/api/v1/pets/1").with(user("audit").roles(role.name()))).andExpect(status().is(commerce));
        mvc.perform(delete("/api/v1/services/1").with(user("audit").roles(role.name()))).andExpect(status().is(commerce));
        mvc.perform(delete("/api/v1/treatments/1").with(user("audit").roles(role.name()))).andExpect(status().is(medical));
    }

    @Test void invalidRegistrationReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType("application/json")
                .content("{\"email\":\"invalid\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email")));
    }

    @Test void customerCannotAccessAdminOrAnotherUsersNotifications() throws Exception {
        var principal = new UserDetailsImpl(1L, "a", "a@test", "unused", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER")));
        mvc.perform(get("/api/v1/admin/dashboard").with(user(principal))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/notifications/2").with(user(principal))).andExpect(status().isForbidden());
        var owner = new User(); owner.setId(2L);
        var notice = com.petcarehub.entity.Notification.builder().user(owner).build();
        when(notifications.findById(10L)).thenReturn(Optional.of(notice));
        mvc.perform(put("/api/v1/notifications/10/read").with(user(principal))).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/users/me").with(user(principal)).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
