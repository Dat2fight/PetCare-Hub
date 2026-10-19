package com.petcarehub;

import com.petcarehub.entity.User;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.security.JwtTokenProvider;
import com.petcarehub.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class EnabledAccountTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider tokens;
    @Autowired PasswordEncoder passwords;
    @MockBean UserRepository users;

    @Test void principalReflectsEnabledIncludingNull() {
        User account = User.builder().username("enabled-check").build();
        assertTrue(UserDetailsImpl.build(account).isEnabled());
        account.setEnabled(false);
        assertFalse(UserDetailsImpl.build(account).isEnabled());
        account.setEnabled(null);
        assertFalse(UserDetailsImpl.build(account).isEnabled());
    }

    @Test void tokenIssuedBeforeDisablingIsRejected() throws Exception {
        User account = User.builder().username("disabled-customer").build(); account.setId(11L);
        var principal = UserDetailsImpl.build(account);
        String token = tokens.generateToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        account.setEnabled(false);
        when(users.findByUsername(account.getUsername())).thenReturn(Optional.of(account));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test void disabledAccountCannotLoginWithCorrectPassword() throws Exception {
        String password = UUID.randomUUID().toString();
        User account = User.builder().username("disabled-login").enabled(false).passwordHash(passwords.encode(password)).build();
        when(users.findByUsername(account.getUsername())).thenReturn(Optional.of(account));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"username\":\"disabled-login\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
