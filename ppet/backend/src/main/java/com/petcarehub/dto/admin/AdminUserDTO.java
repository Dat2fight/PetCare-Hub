package com.petcarehub.dto.admin;

import com.petcarehub.entity.User;
import com.petcarehub.enums.RoleType;
import java.util.Set;
import java.util.stream.Collectors;

public record AdminUserDTO(Long id, String username, String email, String firstName,
        String lastName, Boolean enabled, Set<RoleType> roles) {
    public static AdminUserDTO from(User user) {
        return new AdminUserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getEnabled(), user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()));
    }
}
