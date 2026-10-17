package com.petcarehub.service.impl;

import com.petcarehub.dto.admin.DashboardSummaryDTO;
import com.petcarehub.dto.admin.RevenueReportDTO;
import com.petcarehub.entity.Order;
import com.petcarehub.entity.Role;
import com.petcarehub.entity.User;
import com.petcarehub.enums.RoleType;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.OrderRepository;
import com.petcarehub.repository.PetRepository;
import com.petcarehub.repository.RoleRepository;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PetRepository petRepository;
    private final RoleRepository roleRepository;

    @Override
    public DashboardSummaryDTO getDashboardSummary(java.time.LocalDate from, java.time.LocalDate to) {
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();
        long totalPets = petRepository.count();

        BigDecimal totalRevenue = revenue(from, to).getRevenue();

        return DashboardSummaryDTO.builder()
                .totalUsers(totalUsers)
                .totalOrders(totalOrders)
                .totalPets(totalPets)
                .totalRevenue(totalRevenue)
                .build();
    }

    @Override
    public RevenueReportDTO getRevenueReport(java.time.LocalDate from, java.time.LocalDate to) {
        var summary = revenue(from, to);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;

        return RevenueReportDTO.builder()
                .reportDate(LocalDate.now())
                .periods(orderRepository.revenueByMonth(start.atStartOfDay(), end.plusDays(1).atStartOfDay()))
                .totalOrders(summary.getCount())
                .totalRevenue(summary.getRevenue())
                .build();
    }

    private OrderRepository.RevenueTotal revenue(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        if (start.isAfter(end)) throw new BadRequestException("Ngày bắt đầu không được sau ngày kết thúc");
        // NEEDS DECISION: no paidAt/partial refund fields; use createdAt and exclude fully refunded payments.
        return orderRepository.summarizeRevenue(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với id: " + userId));

        if (role == null || role.isBlank()) {
            throw new BadRequestException("Tên role không được để trống");
        }

        RoleType roleType;
        try {
            String roleName = role.trim().toUpperCase();
            if (roleName.startsWith("ROLE_")) {
                roleName = roleName.substring(5);
            }
            roleType = RoleType.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Role không hợp lệ: " + role);
        }

        Role roleEntity = roleRepository.findByName(roleType)
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại: " + role));

        user.getRoles().clear();
        user.getRoles().add(roleEntity);
        return userRepository.save(user);
    }
}

