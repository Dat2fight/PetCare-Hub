package com.petcarehub.dto.auth;

import com.petcarehub.enums.RoleType;
import lombok.Data;
import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Set<RoleType> roles;
}
