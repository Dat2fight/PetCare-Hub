package com.petcarehub.controller;

import com.petcarehub.dto.auth.ProfileUpdateRequest;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.security.Ownership;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository users;
    private final Ownership ownership;
    @PutMapping("/me")
    @Transactional
    public void update(@Valid @RequestBody ProfileUpdateRequest request) {
        var user = users.findById(ownership.userId()).orElseThrow(() -> new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy tài khoản"));
        user.setFirstName(request.getFirstName()); user.setLastName(request.getLastName());
        user.setEmail(request.getEmail()); user.setPhone(request.getPhone()); users.save(user);
    }
}
