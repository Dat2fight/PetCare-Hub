package com.petcarehub.security;

import com.petcarehub.enums.RoleType;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import java.util.*;

/** The only role-to-permission mapping for the portal. Legacy resource URLs retain their guards. */
@Component("perm")
public class PermissionService {
    private static final Map<RoleType, Set<Permission>> GRANTS = Map.of(
        RoleType.ADMIN, Collections.unmodifiableSet(EnumSet.allOf(Permission.class)),
        RoleType.SALES_STAFF, Set.of(Permission.PORTAL_ACCESS, Permission.ORDER_MANAGE,
            Permission.CATALOG_MANAGE, Permission.CATALOG_READ, Permission.INVENTORY_MANAGE,
            Permission.PROMOTION_MANAGE, Permission.SALE_PET_MANAGE, Permission.REVIEW_MANAGE,
            Permission.CONTENT_MANAGE, Permission.PACKAGE_MANAGE, Permission.ASSIGNED_APPOINTMENT_MANAGE),
        RoleType.WAREHOUSE_STAFF, Set.of(Permission.PORTAL_ACCESS, Permission.CATALOG_READ, Permission.INVENTORY_MANAGE),
        RoleType.VETERINARIAN, Set.of(Permission.PORTAL_ACCESS, Permission.ASSIGNED_APPOINTMENT_MANAGE, Permission.ASSIGNED_MEDICAL_MANAGE),
        RoleType.CUSTOMER, Set.of());
    public Set<Permission> permissions(Authentication authentication) {
        Set<Permission> result = EnumSet.noneOf(Permission.class);
        if (authentication == null || !authentication.isAuthenticated()) return result;
        if (authentication.getPrincipal() instanceof UserDetailsImpl p && (!p.isEnabled() || p.isMustChangePassword())) return result;
        authentication.getAuthorities().forEach(a -> {
            try { result.addAll(GRANTS.get(RoleType.valueOf(a.getAuthority().replaceFirst("^ROLE_", "")))); }
            catch (IllegalArgumentException ignored) { /* No permissions for unknown authorities. */ }
        });
        return result;
    }
    public boolean has(String permission) {
        return permissions(SecurityContextHolder.getContext().getAuthentication()).contains(Permission.valueOf(permission));
    }
    public AuthorizationManager<RequestAuthorizationContext> access(Permission permission) {
        return (authentication, context) -> new AuthorizationDecision(permissions(authentication.get()).contains(permission));
    }
    public AuthorizationManager<RequestAuthorizationContext> accessAny(Permission... values) {
        return (authentication,context) -> {var grants=permissions(authentication.get());return new AuthorizationDecision(Arrays.stream(values).anyMatch(grants::contains));};
    }
}
