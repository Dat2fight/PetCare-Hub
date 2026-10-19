package com.petcarehub.dto.auth;

import com.petcarehub.enums.RoleType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message="Tên đăng nhập không được trống") @Size(max=50, message="Tên đăng nhập tối đa 50 ký tự")
    private String username;
    @NotBlank(message="Email không được trống") @Email(message="Email không hợp lệ") @Size(max=255, message="Email tối đa 255 ký tự")
    private String email;
    @NotBlank(message="Mật khẩu không được trống") @Size(min=8, max=72, message="Mật khẩu phải từ 8 đến 72 ký tự")
    @com.petcarehub.validation.BcryptPasswordLength
    private String password;
    @NotBlank(message="Tên không được trống") @Size(max=100, message="Tên tối đa 100 ký tự")
    private String firstName;
    @NotBlank(message="Họ không được trống") @Size(max=100, message="Họ tối đa 100 ký tự")
    private String lastName;
    @Size(max=20, message="Số điện thoại tối đa 20 ký tự")
    private String phone;
    private RoleType role;
}
