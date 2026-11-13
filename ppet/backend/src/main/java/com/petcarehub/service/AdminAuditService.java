package com.petcarehub.service;

import com.petcarehub.entity.AdminAuditLog;
import com.petcarehub.repository.AdminAuditRepository;
import com.petcarehub.security.*;
import com.petcarehub.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.*;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class AdminAuditService {
    private final AdminAuditRepository logs;
    private final Ownership ownership;
    private final Clock clock;
    // Values are identifiers/booleans/enums only. Passwords, tokens and personal details never enter the log.
    private static final Set<String> KEYS=Set.of("enabledBefore","enabledAfter","roleBefore","roleAfter",
        "deleted","sessionsRevoked","appointmentsUnassigned","mustChangePassword","status","method",
        "statusBefore","statusAfter","quantity","staffId","reasonProvided","paymentStatus","replay");

    @Transactional(propagation=Propagation.MANDATORY)
    public void record(String action, Long targetId, Map<String,?> details) { append(action,"USER",targetId,details,"SUCCESS"); }

    @Transactional(propagation=Propagation.MANDATORY)
    public void record(String action, String targetType, Long targetId, Map<String,?> details) {
        append(action,targetType,targetId,details,"SUCCESS");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void failure(int status, String method) {
        append("ADMIN_REQUEST_REJECTED", "REQUEST", null, Map.of("status",status,"method",method),status==401 || status==403 ? "DENIED" : "FAILED");
    }

    private void append(String action, String targetType, Long targetId, Map<String,?> details, String result) {
        Map<String,Object> safe=new TreeMap<>();
        details.forEach((key,value) -> { if(KEYS.contains(key) && (value instanceof Boolean || value instanceof Number || value instanceof String)) safe.put(key,value); });
        String json;
        try { json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(safe); }
        catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Không thể ghi nhật ký"); }
        if(json.length()>2000) throw new BadRequestException("Nhật ký vượt giới hạn");
        var auth=SecurityContextHolder.getContext().getAuthentication();
        var request=RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes ? attributes.getRequest() : null;
        String correlation=request==null ? UUID.randomUUID().toString() : (String)request.getAttribute("correlationId");
        logs.save(AdminAuditLog.builder().occurredAt(LocalDateTime.now(clock)).actorUserId(ownership.userId())
            .actorRole(auth.getAuthorities().stream().map(a -> a.getAuthority()).sorted().collect(java.util.stream.Collectors.joining(",")))
            .action(action).targetType(targetType).targetId(targetId).summary(action).details(json).result(result)
            .ip(request==null ? null : request.getRemoteAddr()) // No trust in client-supplied forwarding headers.
            .correlationId(correlation==null ? UUID.randomUUID().toString() : correlation).build());
    }

    @Transactional(readOnly=true)
    @org.springframework.security.access.prepost.PreAuthorize("@perm.has('AUDIT_VIEW')")
    public Page<AdminAuditLog> list(Long actor, String action, String targetType, Long target, LocalDate from, LocalDate to, Pageable page) {
        if(from!=null && to!=null && from.isAfter(to)) throw new BadRequestException("Kỳ nhật ký không hợp lệ");
        if(to!=null && to.getYear()>9998) throw new BadRequestException("Năm phải nhỏ hơn 9999");
        return logs.findAll((root,query,cb) -> {
            var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if(actor!=null) predicates.add(cb.equal(root.get("actorUserId"),actor));
            if(action!=null && !action.isBlank()) predicates.add(cb.equal(root.get("action"),action));
            if(targetType!=null && !targetType.isBlank()) predicates.add(cb.equal(root.get("targetType"),targetType));
            if(target!=null) predicates.add(cb.equal(root.get("targetId"),target));
            if(from!=null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"),from.atStartOfDay()));
            if(to!=null) predicates.add(cb.lessThan(root.get("occurredAt"),to.plusDays(1).atStartOfDay()));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, PageRequest.of(page.getPageNumber(),Math.min(page.getPageSize(),100),Sort.by(Sort.Direction.DESC,"occurredAt","id")));
    }
}
