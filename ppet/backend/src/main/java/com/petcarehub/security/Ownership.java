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
    public void requireSelfUser(Long id) {
        if (!userId().equals(id)) throw new AccessDeniedException("Bạn không có quyền truy cập dữ liệu này");
    }
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
        return pets.findByIdAndOwnerId(id, currentId)
                .orElseThrow(() -> new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy thú cưng"));
    }
    public void requirePet(Long id) {
        Long currentId = userId();
        var pet = hasRole("ADMIN") || hasRole("VETERINARIAN") ? pets.findById(id) : pets.findByIdAndOwnerId(id, currentId);
        pet.orElseThrow(() -> new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy thú cưng"));
    }
}
