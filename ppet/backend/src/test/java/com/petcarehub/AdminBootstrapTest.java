package com.petcarehub;

import com.petcarehub.config.AdminBootstrap;
import com.petcarehub.config.DataSeeder;
import com.petcarehub.entity.Role;
import com.petcarehub.entity.User;
import com.petcarehub.enums.RoleType;
import com.petcarehub.repository.RoleRepository;
import com.petcarehub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminBootstrapTest {
    UserRepository users = mock(UserRepository.class);
    RoleRepository roles = mock(RoleRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    MockEnvironment env = new MockEnvironment();
    AdminBootstrap bootstrap = new AdminBootstrap(users, roles, encoder, env);

    private String configured() {
        String password = UUID.randomUUID() + "Aa9!";
        env.withProperty("app.bootstrap.username", "operator").withProperty("app.bootstrap.email", "operator@example.test")
                .withProperty("app.bootstrap.password", password);
        return password;
    }
    @Test void defaultContextDoesNotEnableAdminOrDemoBootstrap() {
        new ApplicationContextRunner().withUserConfiguration(AdminBootstrap.class, DataSeeder.class)
                .run(context -> { assertFalse(context.containsBean("adminBootstrap")); assertFalse(context.containsBean("dataSeeder")); });
    }
    @Test void missingPasswordFailsBeforeWritingAnything() {
        env.withProperty("app.bootstrap.username", "operator").withProperty("app.bootstrap.email", "operator@example.test");
        assertThrows(IllegalStateException.class, () -> bootstrap.run());
        verifyNoInteractions(users, roles, encoder);
    }
    @Test void createsAdminWithConfiguredEncodedPassword() {
        String password = configured();
        when(roles.findByName(any())).thenAnswer(call -> Optional.of(Role.builder().name(call.getArgument(0)).build()));
        when(encoder.encode(password)).thenReturn("encoded-for-test");
        bootstrap.run();
        verify(encoder).encode(password);
        verify(users).save(argThat(u -> u.getPasswordHash().equals("encoded-for-test") && u.getRoles().iterator().next().getName() == RoleType.ADMIN));
    }
    @Test void rerunDoesNotResetExistingAdmin() {
        configured();
        when(users.findByUsername("operator")).thenReturn(Optional.of(User.builder().enabled(false)
                .roles(Set.of(Role.builder().name(RoleType.ADMIN).build())).build()));
        bootstrap.run();
        verify(users, never()).save(any()); verifyNoInteractions(encoder);
    }
    @Test void doesNotPromoteExistingCustomer() {
        configured();
        when(users.findByUsername("operator")).thenReturn(Optional.of(User.builder()
                .roles(Set.of(Role.builder().name(RoleType.CUSTOMER).build())).build()));
        assertThrows(IllegalStateException.class, () -> bootstrap.run());
        verify(users, never()).save(any());
    }
}
