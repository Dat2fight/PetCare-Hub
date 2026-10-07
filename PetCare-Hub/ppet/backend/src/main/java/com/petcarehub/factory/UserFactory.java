package com.petcarehub.factory;

import com.petcarehub.dto.auth.RegisterRequest;
import com.petcarehub.entity.Role;
import com.petcarehub.enums.RoleType;
import com.petcarehub.entity.User;
import com.petcarehub.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class UserFactory {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());

        Set<Role> roles = new HashSet<>();
        RoleType requestRole = request.getRole();

        if (requestRole == null || (requestRole != RoleType.CUSTOMER && requestRole != RoleType.SALES_STAFF)) {
            requestRole = RoleType.CUSTOMER;
        }

        Role userRole = roleRepository.findByName(requestRole)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);
        user.setRoles(roles);

        return user;
    }
}

