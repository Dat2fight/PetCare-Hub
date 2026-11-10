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
import com.petcarehub.dto.admin.AdminUserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PetRepository petRepository;
    private final RoleRepository roleRepository;
    private final java.time.Clock clock;

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public DashboardSummaryDTO getDashboardSummary(java.time.LocalDate from, java.time.LocalDate to) {
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();
        long totalPets = petRepository.count();

        var period = period(from, to);
        BigDecimal totalRevenue = orderRepository.summarizeRevenue(period.start(), period.end()).getRevenue();

        return DashboardSummaryDTO.builder()
                .totalUsers(totalUsers)
                .totalOrders(totalOrders)
                .totalPets(totalPets)
                .totalRevenue(totalRevenue)
                .build();
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public RevenueReportDTO getRevenueReport(java.time.LocalDate from, java.time.LocalDate to) {
        var period = period(from, to);
        var summary = orderRepository.summarizeRevenue(period.start(), period.end());

        return RevenueReportDTO.builder()
                .reportDate(LocalDate.now(clock))
                .periods(orderRepository.revenueByMonth(period.start(), period.end()))
                .totalOrders(summary.getCount())
                .totalRevenue(summary.getRevenue())
                .build();
    }

    private record Period(java.time.LocalDateTime start, java.time.LocalDateTime end) {}
    private Period period(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        if (start.isAfter(end)) throw new BadRequestException("Ngày bắt đầu không được sau ngày kết thúc");
        if (java.time.temporal.ChronoUnit.DAYS.between(start, end) >= 366 || start.getYear() < 1 || end.getYear() > 9998)
            throw new BadRequestException("Kỳ báo cáo tối đa 366 ngày, năm từ 1 đến 9998");
        return new Period(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Page<AdminUserDTO> getAllUsers(Pageable pageable) {
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("id");
        for (Sort.Order order : sort) {
            if (!java.util.Set.of("id", "username", "email", "firstName", "lastName", "enabled").contains(order.getProperty()))
                throw new BadRequestException("Trường sắp xếp người dùng không hợp lệ");
        }
        if (sort.getOrderFor("id") == null) sort = sort.and(Sort.by("id"));
        Pageable bounded = pageable.isUnpaged() ? PageRequest.of(0, 20, sort)
                : PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), sort);
        return userRepository.findAll(bounded).map(AdminUserDTO::from);
    }

    @Override
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
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

        // NEEDS DECISION: admin removal needs a dedicated, serialized governance flow.
        // Until then, no generic role change may remove an existing administrator.
        if (roleType != RoleType.ADMIN && user.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ADMIN))
            throw new org.springframework.security.access.AccessDeniedException("Không thể hạ quyền quản trị viên qua chức năng này");

        Role roleEntity = roleRepository.findByName(roleType)
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại: " + role));

        user.getRoles().clear();
        user.getRoles().add(roleEntity);
        return userRepository.save(user);
    }
}

