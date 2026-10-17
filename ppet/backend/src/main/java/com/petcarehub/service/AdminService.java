package com.petcarehub.service;

import com.petcarehub.dto.admin.DashboardSummaryDTO;
import com.petcarehub.dto.admin.RevenueReportDTO;
import com.petcarehub.entity.User;

import java.util.List;

public interface AdminService {
    DashboardSummaryDTO getDashboardSummary(java.time.LocalDate from, java.time.LocalDate to);
    RevenueReportDTO getRevenueReport(java.time.LocalDate from, java.time.LocalDate to);
    List<User> getAllUsers();
    User updateUserRole(Long userId, String role);
}
