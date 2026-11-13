package com.petcarehub.controller;

import com.petcarehub.dto.admin.*;
import com.petcarehub.entity.AdminAuditLog;
import com.petcarehub.enums.RoleType;
import com.petcarehub.service.*;
import com.petcarehub.security.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final StaffAccountService staff;
    private final AdminAccountService accounts;
    private final AdminAuditService audit;
    private final PermissionService permissions;
    @GetMapping("/me/permissions") @PreAuthorize("@perm.has('PORTAL_ACCESS')")
    public java.util.Map<String,?> permissions(org.springframework.security.core.Authentication authentication) {
        return java.util.Map.of("permissions",permissions.permissions(authentication));
    }
    @PostMapping("/staff") @PreAuthorize("@perm.has('USER_MANAGE')")
    public ResponseEntity<AdminRequests.Provisioned> createStaff(@Valid @RequestBody AdminRequests.Staff request) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(staff.createStaff(request));
    }
    @GetMapping("/dashboard") @PreAuthorize("@perm.has('REPORT_VIEW')")
    public DashboardSummaryDTO dashboard(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) { return adminService.getDashboardSummary(from,to); }
    @GetMapping("/revenue-report") @PreAuthorize("@perm.has('REPORT_VIEW')")
    public RevenueReportDTO report(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) { return adminService.getRevenueReport(from,to); }
    @GetMapping("/users") @PreAuthorize("@perm.has('USER_MANAGE')")
    public Page<AdminUserDTO> users(@RequestParam(required=false) String keyword,@RequestParam(required=false) RoleType role,
            @RequestParam(required=false) String status,@PageableDefault(size=20,sort="id") Pageable pageable) {
        return accounts.list(keyword,role,status,pageable);
    }
    @PatchMapping("/users/{userId}/role") @PreAuthorize("@perm.has('USER_MANAGE')")
    public AdminUserDTO role(@PathVariable Long userId,@Valid @RequestBody AdminRequests.RoleChange request) { return accounts.role(userId,request); }
    @PatchMapping("/users/{userId}/status") @PreAuthorize("@perm.has('USER_MANAGE')")
    public AdminUserDTO status(@PathVariable Long userId,@Valid @RequestBody AdminRequests.Status request) { return accounts.status(userId,request); }
    @DeleteMapping("/users/{userId}") @PreAuthorize("@perm.has('USER_MANAGE')")
    public AdminUserDTO remove(@PathVariable Long userId,@Valid @RequestBody AdminRequests.Reason request) { return accounts.remove(userId,request); }
    @PostMapping("/users/{userId}/revoke-sessions") @PreAuthorize("@perm.has('USER_MANAGE')") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long userId,@Valid @RequestBody AdminRequests.Reason request) { accounts.revoke(userId,request); }
    @GetMapping("/users/{userId}/overview") @PreAuthorize("@perm.has('USER_MANAGE')")
    public AdminAccountService.Overview overview(@PathVariable Long userId,@RequestParam(defaultValue="0") int ordersPage,
            @RequestParam(defaultValue="0") int appointmentsPage,@RequestParam(defaultValue="0") int petsPage,@RequestParam(defaultValue="10") int size) {
        return accounts.overview(userId,ordersPage,appointmentsPage,petsPage,size);
    }
    @GetMapping("/audit-logs") @PreAuthorize("@perm.has('AUDIT_VIEW')")
    public Page<AdminAuditLog> logs(@RequestParam(required=false) Long actor,@RequestParam(required=false) String action,
            @RequestParam(required=false) String targetType,@RequestParam(required=false) Long target,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@PageableDefault(size=20) Pageable pageable) {
        return audit.list(actor,action,targetType,target,from,to,pageable);
    }
}
