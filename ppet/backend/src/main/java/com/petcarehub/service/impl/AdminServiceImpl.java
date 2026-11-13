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
    @org.springframework.security.access.prepost.PreAuthorize("@perm.has('REPORT_VIEW')")
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
    @org.springframework.security.access.prepost.PreAuthorize("@perm.has('REPORT_VIEW')")
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

}
