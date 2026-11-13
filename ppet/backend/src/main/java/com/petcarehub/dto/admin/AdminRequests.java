package com.petcarehub.dto.admin;
import jakarta.validation.constraints.*;
import com.petcarehub.enums.RoleType;

public final class AdminRequests {
    private AdminRequests() {}
    public record Status(@NotNull Boolean enabled, @Size(max=500) String reason, @Size(max=72) String currentPassword) {}
    public record RoleChange(@NotNull RoleType role, @NotBlank @Size(max=500) String reason, @Size(max=72) String currentPassword) {}
    public record Reason(@NotBlank @Size(max=500) String reason, @Size(max=72) String currentPassword) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
    public record Staff(@NotBlank @Size(max=50) String username, @NotBlank @Email @Size(max=255) String email,
        @NotBlank @Size(max=100) String firstName, @NotBlank @Size(max=100) String lastName,
        @Size(max=20) String phone, @NotNull RoleType role) {}
    public record Provisioned(AdminUserDTO user, String temporaryPassword) {}
    public record Password(@NotBlank @Size(max=72) String currentPassword,
        @NotBlank @Size(min=8,max=72) @com.petcarehub.validation.BcryptPasswordLength String newPassword) {}
}
