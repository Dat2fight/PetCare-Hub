package com.petcarehub.config;

import com.petcarehub.entity.Role;
import com.petcarehub.entity.User;
import com.petcarehub.enums.RoleType;
import com.petcarehub.repository.RoleRepository;
import com.petcarehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component @Order(0) @RequiredArgsConstructor
@ConditionalOnProperty(name="app.bootstrap.enabled", havingValue="true")
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;
    private final Environment environment;

    @Override @Transactional
    public void run(String... args) {
        String username = required("username");
        String email = required("email");
        String password = required("password");
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException("Bootstrap password must contain at least 12 characters and at most 72 UTF-8 bytes");
        var existing = users.findByUsername(username);
        if (existing.isPresent()) {
            // Never reset credentials, enable a disabled account, or silently promote an existing customer.
            if (existing.get().getRoles().stream().noneMatch(r -> r.getName() == RoleType.ADMIN))
                throw new IllegalStateException("Bootstrap username already belongs to a non-admin account");
            return;
        }
        if (users.existsByEmail(email)) throw new IllegalStateException("Bootstrap email is already in use");
        for (RoleType type : RoleType.values()) {
            if (roles.findByName(type).isEmpty()) roles.save(Role.builder().name(type).description(type.name()).build());
        }
        Role admin = roles.findByName(RoleType.ADMIN).orElseThrow();
        users.save(User.builder().username(username).email(email).passwordHash(passwords.encode(password))
                .firstName("System").lastName("Administrator").enabled(true).roles(Set.of(admin)).build());
    }

    private String required(String name) {
        String value = environment.getProperty("app.bootstrap." + name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing bootstrap configuration: " + name);
        return value;
    }
}
