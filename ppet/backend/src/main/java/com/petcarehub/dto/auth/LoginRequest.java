package com.petcarehub.dto.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    @jakarta.validation.constraints.NotBlank(message="Mật khẩu không được trống")
    @com.petcarehub.validation.BcryptPasswordLength
    private String password;
}
