package com.petcarehub.dto.auth;

import com.petcarehub.enums.RoleType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Only accepted by the ADMIN staff provisioning endpoint. */
@Getter @Setter
public class StaffCreateRequest extends RegisterRequest {
    @NotNull(message="Vai trò nhân viên là bắt buộc")
    private RoleType role;
}
