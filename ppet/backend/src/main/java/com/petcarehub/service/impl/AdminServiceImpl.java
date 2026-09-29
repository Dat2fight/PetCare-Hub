package com.petcarehub.service.impl;

import com.petcarehub.dto.admin.DashboardSummaryDTO;
import com.petcarehub.dto.admin.RevenueReportDTO;
import com.petcarehub.entity.Order;
import com.petcarehub.entity.User;
import com.petcarehub.repository.OrderRepository;
import com.petcarehub.repository.PetRepository;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PetRepository petRepository;

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();
        long totalPets = petRepository.count();

        List<Order> allOrders = orderRepository.findAll();
        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getTotalAmount) // Assuming Order has getTotalAmount returning BigDecimal
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardSummaryDTO.builder()
                .totalUsers(totalUsers)
                .totalOrders(totalOrders)
                .totalPets(totalPets)
                .totalRevenue(totalRevenue)
                .build();
    }

    @Override
    public RevenueReportDTO getRevenueReport() {
        List<Order> allOrders = orderRepository.findAll();
        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RevenueReportDTO.builder()
                .reportDate(LocalDate.now())
                .totalOrders(allOrders.size())
                .totalRevenue(totalRevenue)
                .build();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Assuming user has setRole method taking String or Enum.
        // We will just set it as String or parse to Enum if needed.
        // For POC, assuming setRole(String) exists.
        com.petcarehub.entity.Role roleEntity = new com.petcarehub.entity.Role(); roleEntity.setName(com.petcarehub.enums.RoleType.valueOf(role)); user.getRoles().clear(); user.getRoles().add(roleEntity);
        return userRepository.save(user);
    }
}

