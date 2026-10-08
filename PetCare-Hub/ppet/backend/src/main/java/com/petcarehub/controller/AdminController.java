package com.petcarehub.controller;

import com.petcarehub.dto.admin.DashboardSummaryDTO;
import com.petcarehub.dto.admin.RevenueReportDTO;
import com.petcarehub.entity.User;
import com.petcarehub.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(adminService.getDashboardSummary());
    }

    @GetMapping("/revenue-report")
    public ResponseEntity<RevenueReportDTO> getRevenueReport() {
        return ResponseEntity.ok(adminService.getRevenueReport());
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<User> updateUserRole(@PathVariable Long userId, @RequestParam String role) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, role));
    }
}
