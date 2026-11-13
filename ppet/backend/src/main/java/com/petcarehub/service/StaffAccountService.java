package com.petcarehub.service;

import com.petcarehub.dto.admin.*;
import com.petcarehub.entity.User;
import com.petcarehub.enums.RoleType;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class StaffAccountService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;
    private final AdminAuditService audit;
    private final AdminAccountService accounts;
    private final java.security.SecureRandom random=new java.security.SecureRandom();

    @Transactional @PreAuthorize("@perm.has('USER_MANAGE')")
    public AdminRequests.Provisioned createStaff(AdminRequests.Staff request) {
        accounts.lockGovernance();
        if(!Set.of(RoleType.SALES_STAFF,RoleType.WAREHOUSE_STAFF,RoleType.VETERINARIAN).contains(request.role())) throw new BadRequestException("Chọn vai trò nhân viên hợp lệ");
        if(users.existsByUsername(request.username().strip())||users.existsByEmail(request.email().strip())) throw new BadRequestException("Tên đăng nhập hoặc email đã được sử dụng");
        byte[] secret=new byte[24]; random.nextBytes(secret);
        String temporary=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        var employee=users.saveAndFlush(User.builder().username(request.username().strip()).email(request.email().strip())
            .firstName(request.firstName().strip()).lastName(request.lastName().strip()).phone(request.phone())
            .passwordHash(passwords.encode(temporary)).mustChangePassword(true)
            .roles(new HashSet<>(Set.of(roles.findByName(request.role()).orElseThrow(() -> new BadRequestException("Vai trò không tồn tại"))))).build());
        audit.record("STAFF_CREATED",employee.getId(),Map.of("roleAfter",request.role().name(),"mustChangePassword",true));
        return new AdminRequests.Provisioned(AdminUserDTO.from(employee),temporary);
    }
}
