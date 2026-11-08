package com.petcarehub.service;

import com.petcarehub.dto.admin.DashboardSummaryDTO;
import com.petcarehub.dto.admin.RevenueReportDTO;
import com.petcarehub.entity.User;

import com.petcarehub.dto.admin.AdminUserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    DashboardSummaryDTO getDashboardSummary(java.time.LocalDate from, java.time.LocalDate to);
    RevenueReportDTO getRevenueReport(java.time.LocalDate from, java.time.LocalDate to);
    Page<AdminUserDTO> getAllUsers(Pageable pageable);
    User updateUserRole(Long userId, String role);
}
