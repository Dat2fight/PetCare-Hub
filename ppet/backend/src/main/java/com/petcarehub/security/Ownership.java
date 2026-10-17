package com.petcarehub.security;

import com.petcarehub.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

@Component
@RequiredArgsConstructor
public class Ownership {
    private final PetRepository pets;
    public Long userId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl principal))
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Bạn cần đăng nhập");
        return principal.getId();
    }
    public void requireUser(Long id) {
        if (!userId().equals(id) && !hasRole("ADMIN")) throw new AccessDeniedException("Bạn không có quyền truy cập dữ liệu này");
    }
    public boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    public void requirePet(Long id) {
        var pet = pets.findById(id).orElseThrow(() -> new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy thú cưng"));
        if (hasRole("ADMIN") || hasRole("VETERINARIAN")) return;
        if (pet.getOwner() == null || !pet.getOwner().getId().equals(userId())) throw new AccessDeniedException("Bạn không có quyền truy cập hồ sơ này");
    }
}
