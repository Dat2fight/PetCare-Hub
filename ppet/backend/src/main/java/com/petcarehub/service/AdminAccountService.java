package com.petcarehub.service;

import com.petcarehub.dto.admin.*;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.Ownership;
import com.petcarehub.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @PreAuthorize("@perm.has('USER_MANAGE')")
public class AdminAccountService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final AppointmentRepository appointments;
    private final NotificationRepository notifications;
    private final AdminAuditService audit;
    private final Ownership ownership;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final jakarta.persistence.EntityManager em;

    @Transactional(readOnly=true)
    public Page<AdminUserDTO> list(String keyword, RoleType role, String status, Pageable pageable) {
        if(keyword!=null && keyword.length()>100) throw new BadRequestException("Từ khóa tối đa 100 ký tự");
        if(status!=null && !Set.of("ACTIVE","LOCKED","DELETED","ALL").contains(status)) throw new BadRequestException("Trạng thái không hợp lệ");
        Sort sort=pageable.getSort().isSorted()?pageable.getSort():Sort.by("id");
        for(var order:sort) if(!Set.of("id","username","email","firstName","lastName","enabled").contains(order.getProperty())) throw new BadRequestException("Cột sắp xếp không hợp lệ");
        if(sort.getOrderFor("id")==null) sort=sort.and(Sort.by("id"));
        return users.findAll((root,query,cb) -> {
            var filters=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if(!"ALL".equals(status)) filters.add("DELETED".equals(status)?cb.isNotNull(root.get("deletedAt")):cb.isNull(root.get("deletedAt")));
            if("ACTIVE".equals(status)||"LOCKED".equals(status)) filters.add(cb.equal(root.get("enabled"),"ACTIVE".equals(status)));
            if(role!=null) filters.add(cb.equal(root.join("roles").get("name"),role));
            if(keyword!=null && !keyword.isBlank()) {
                String pattern="%"+keyword.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_").replace("[","![")+"%";
                filters.add(cb.or(cb.like(cb.lower(root.get("username")),pattern,'!'),cb.like(cb.lower(root.get("email")),pattern,'!'),
                    cb.like(cb.lower(cb.concat(cb.concat(root.get("lastName")," "),root.get("firstName"))),pattern,'!')));
            }
            query.distinct(true);
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        },PageRequest.of(pageable.getPageNumber(),Math.min(pageable.getPageSize(),100),sort)).map(AdminUserDTO::from);
    }

    // A stable role row serializes governance before locking users: two administrators cannot remove each other concurrently.
    public void lockGovernance() {
        roles.lockByName(RoleType.ADMIN).orElseThrow(() -> new IllegalStateException("Thiếu vai trò ADMIN"));
        var actor=users.findById(ownership.userId()).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Tài khoản không tồn tại"));
        em.refresh(actor);
        var principal=(com.petcarehub.security.UserDetailsImpl)org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!admin(actor)||!Boolean.TRUE.equals(actor.getEnabled())||actor.getDeletedAt()!=null||actor.getTokenVersion()!=principal.getTokenVersion())
            throw new org.springframework.security.access.AccessDeniedException("Phiên quản trị không còn hợp lệ");
    }
    private User target(Long id) { return users.findLockedById(id).filter(u -> u.getDeletedAt()==null).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản")); }
    private boolean admin(User user) { return user.getRoles().stream().anyMatch(r -> r.getName()==RoleType.ADMIN); }
    private boolean employee(User user) { return user.getRoles().stream().anyMatch(r -> r.getName()!=RoleType.CUSTOMER); }
    private void other(User user) { if(ownership.userId().equals(user.getId())) throw new IllegalStateException("Không được thay đổi quyền, khóa hoặc xóa chính mình"); }
    private void preserveLastAdmin(User user) {
        if(admin(user) && Boolean.TRUE.equals(user.getEnabled()) && users.countActiveAdmins()<=1) throw new IllegalStateException("Phải giữ ít nhất một quản trị viên đang hoạt động");
    }
    private void reauthenticate(String password) {
        var actor=users.findById(ownership.userId()).filter(u -> Boolean.TRUE.equals(u.getEnabled()) && u.getDeletedAt()==null)
            .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Tài khoản không hoạt động"));
        if(password==null || !passwords.matches(password,actor.getPasswordHash())) throw new org.springframework.security.access.AccessDeniedException("Mật khẩu xác nhận không đúng");
    }
    private void reason(String reason) { if(reason==null || reason.isBlank()) throw new BadRequestException("Cần nhập lý do"); }

    @Transactional
    public AdminUserDTO status(Long id, AdminRequests.Status request) {
        lockGovernance(); User user=target(id); other(user);
        if(!request.enabled()) { reason(request.reason()); preserveLastAdmin(user); if(admin(user)) reauthenticate(request.currentPassword()); }
        boolean before=Boolean.TRUE.equals(user.getEnabled());
        user.setEnabled(request.enabled()); user.setTokenVersion(user.getTokenVersion()+1);
        audit.record("USER_STATUS_CHANGED",id,Map.of("enabledBefore",before,"enabledAfter",request.enabled()));
        return AdminUserDTO.from(user);
    }

    @Transactional
    public AdminUserDTO role(Long id, AdminRequests.RoleChange request) {
        lockGovernance(); User user=target(id); other(user);
        Set<RoleType> current=user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
        if(Set.of(RoleType.SALES_STAFF,RoleType.WAREHOUSE_STAFF,RoleType.VETERINARIAN).contains(request.role()) && !current.contains(request.role()))
            throw new BadRequestException("Cấp vai trò nhân viên qua chức năng tạo tài khoản nhân viên");
        if(admin(user)||request.role()==RoleType.ADMIN) reauthenticate(request.currentPassword());
        if(admin(user)&&request.role()!=RoleType.ADMIN) preserveLastAdmin(user);
        String before=current.stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(","));
        user.setRoles(new HashSet<>(Set.of(roles.findByName(request.role()).orElseThrow(() -> new BadRequestException("Vai trò không tồn tại")))));
        user.setTokenVersion(user.getTokenVersion()+1);
        int detached=current.equals(Set.of(request.role()))?0:unassign(user);
        audit.record("USER_ROLE_CHANGED",id,Map.of("roleBefore",before,"roleAfter",request.role().name(),"appointmentsUnassigned",detached));
        return AdminUserDTO.from(user);
    }

    @Transactional
    public AdminUserDTO remove(Long id, AdminRequests.Reason request) {
        lockGovernance(); User user=target(id); other(user); preserveLastAdmin(user);
        if(employee(user)) reauthenticate(request.currentPassword());
        user.setDeletedAt(LocalDateTime.now(clock)); user.setEnabled(false); user.setTokenVersion(user.getTokenVersion()+1);
        int detached=unassign(user);
        audit.record("USER_DELETED",id,Map.of("deleted",true,"appointmentsUnassigned",detached));
        return AdminUserDTO.from(user);
    }

    private int unassign(User user) {
        var now=LocalDateTime.now(clock);
        var pending=appointments.futureAssigned(user.getId(),now.toLocalDate(),now.toLocalTime());
        for(var appointment:pending) {
            appointment.setStaff(null);
            notifications.save(Notification.builder().user(appointment.getCustomer()).type(NotificationType.APPOINTMENT_REMINDER)
                .title("Lịch hẹn cần phân công lại").message("Lịch hẹn #"+appointment.getId()+" đang được sắp xếp lại nhân viên. Cửa hàng sẽ cập nhật cho bạn.").build());
        }
        return pending.size();
    }

    @Transactional
    public void revoke(Long id, AdminRequests.Reason request) {
        lockGovernance(); var user=target(id); user.setTokenVersion(user.getTokenVersion()+1);
        audit.record("USER_SESSIONS_REVOKED",id,Map.of("sessionsRevoked",true));
    }

    public record Slice(List<Map<String,Object>> content, long totalElements, int number, int size) {}
    public record Overview(AdminUserDTO user, Slice orders, Slice appointments, Slice pets, Map<String,Object> loyalty) {}
    @Transactional
    public Overview overview(Long id, int ordersPage, int appointmentsPage, int petsPage, int size) {
        var user=users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if(size<1 || size>100 || ordersPage<0 || appointmentsPage<0 || petsPage<0 || Math.max(ordersPage,Math.max(appointmentsPage,petsPage))>1000000) throw new BadRequestException("Phân trang không hợp lệ");
        // Scalar projections prevent traversing lazy customer/order-item graphs and leaking internal fields.
        var orders=slice("Order o","o.customer.id",id,"o.id,o.orderNumber,o.status,o.totalAmount,o.createdAt",List.of("id","number","status","total","createdAt"),ordersPage,size);
        var appointments=slice("Appointment o","o.customer.id",id,"o.id,o.appointmentDate,o.startTime,o.status",List.of("id","date","time","status"),appointmentsPage,size);
        var pets=slice("Pet o","o.owner.id",id,"o.id,o.name,o.dateOfBirth",List.of("id","name","dateOfBirth"),petsPage,size);
        var points=em.createQuery("select l.availablePoints,l.membershipLevel from LoyaltyAccount l where l.customer.id=:id",Object[].class).setParameter("id",id).getResultList();
        Map<String,Object> loyalty=points.isEmpty()?Map.of("availablePoints",0,"membershipLevel","BRONZE"):Map.of("availablePoints",points.get(0)[0],"membershipLevel",points.get(0)[1]);
        audit.record("USER_OVERVIEW_READ",id,Map.of());
        return new Overview(AdminUserDTO.from(user),orders,appointments,pets,loyalty);
    }
    private Slice slice(String entity,String owner,Long id,String columns,List<String> names,int page,int size) {
        // All JPQL fragments are internal constants; user input is bound only as parameters.
        var rows=em.createQuery("select "+columns+" from "+entity+" where "+owner+"=:id order by o.id desc",Object[].class)
            .setParameter("id",id).setFirstResult(page*size).setMaxResults(size).getResultList();
        var content=rows.stream().map(row -> { Map<String,Object> item=new LinkedHashMap<>(); for(int i=0;i<names.size();i++) item.put(names.get(i),row[i]); return item; }).toList();
        long count=em.createQuery("select count(o) from "+entity+" where "+owner+"=:id",Long.class).setParameter("id",id).getSingleResult();
        return new Slice(content,count,page,size);
    }
}
