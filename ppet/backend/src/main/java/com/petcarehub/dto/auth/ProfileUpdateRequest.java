package com.petcarehub.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @NotBlank(message="Tên là bắt buộc") @Size(max=100, message="Tên tối đa 100 ký tự")
    private String firstName;
    @NotBlank(message="Họ là bắt buộc") @Size(max=100, message="Họ tối đa 100 ký tự")
    private String lastName;
    @NotBlank(message="Email là bắt buộc") @Email(message="Email không hợp lệ") @Size(max=255, message="Email tối đa 255 ký tự")
    private String email;
    @Size(max=20, message="Số điện thoại tối đa 20 ký tự")
    private String phone;
}
