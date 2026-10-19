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
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetailsImpl principal) || !principal.isEnabled())
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Bạn cần đăng nhập");
        return principal.getId();
    }
    public void requireUser(Long id) { authorizedUserId(id); }
    public Long authorizedUserId(Long requestedId) {
        Long currentId = userId();
        if (currentId.equals(requestedId)) return currentId;
        if (requestedId != null && hasRole("ADMIN")) return requestedId;
        throw new AccessDeniedException("Bạn không có quyền truy cập dữ liệu này");
    }
    public boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    // Booking and customer editing require actual ownership, without a clinical/admin override.
    public com.petcarehub.entity.Pet ownedPet(Long id) {
        Long currentId = userId();
        var pet = pets.findById(id).orElseThrow(() -> new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy thú cưng"));
        if (pet.getOwner() == null || !currentId.equals(pet.getOwner().getId()))
            throw new AccessDeniedException("Bạn chỉ được thao tác với thú cưng của mình");
        return pet;
    }
    public void requirePet(Long id) {
        var pet = pets.findById(id).orElseThrow(() -> new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy thú cưng"));
        if (hasRole("ADMIN") || hasRole("VETERINARIAN")) return;
        if (pet.getOwner() == null || !pet.getOwner().getId().equals(userId())) throw new AccessDeniedException("Bạn không có quyền truy cập hồ sơ này");
    }
}
