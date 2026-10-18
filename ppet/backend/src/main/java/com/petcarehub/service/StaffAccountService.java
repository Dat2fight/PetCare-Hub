package com.petcarehub.service;

import com.petcarehub.dto.auth.RegisterRequest;
import com.petcarehub.dto.auth.UserDTO;
import com.petcarehub.enums.RoleType;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.factory.UserFactory;
import com.petcarehub.repository.RoleRepository;
import com.petcarehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service @RequiredArgsConstructor
public class StaffAccountService {
    private final UserFactory userFactory;
    private final UserRepository users;
    private final RoleRepository roles;

    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public UserDTO createStaff(RegisterRequest request) {
        // This endpoint provisions employees only; admin promotion remains a separate admin action.
        if (request.getRole() == null || !Set.of(RoleType.SALES_STAFF, RoleType.WAREHOUSE_STAFF, RoleType.VETERINARIAN).contains(request.getRole()))
            throw new BadRequestException("Chọn vai trò nhân viên hợp lệ");
        if (users.existsByUsername(request.getUsername()) || users.existsByEmail(request.getEmail()))
            throw new BadRequestException("Tên đăng nhập hoặc email đã được sử dụng");
        var role = roles.findByName(request.getRole()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò"));
        var employee = userFactory.createUser(request);
        employee.setRoles(Set.of(role));
        employee = users.save(employee);
        UserDTO dto = new UserDTO();
        dto.setId(employee.getId()); dto.setUsername(employee.getUsername()); dto.setEmail(employee.getEmail());
        dto.setFirstName(employee.getFirstName()); dto.setLastName(employee.getLastName()); dto.setPhone(employee.getPhone());
        dto.setRoles(Set.of(role.getName()));
        return dto;
    }
}
