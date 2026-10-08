package com.petcarehub.dto.auth;

import com.petcarehub.enums.RoleType;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private RoleType role;
}
