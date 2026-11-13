package com.petcarehub.service;
import com.petcarehub.dto.admin.AdminRequests;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.security.Ownership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service @RequiredArgsConstructor
public class PasswordChangeService {
    private final UserRepository users;
    private final Ownership ownership;
    private final PasswordEncoder passwords;
    private final AdminAuditService audit;
    @Transactional
    public void change(AdminRequests.Password request) {
        var user=users.findLockedById(ownership.userId()).filter(u -> Boolean.TRUE.equals(u.getEnabled()) && u.getDeletedAt()==null)
            .orElseThrow(() -> new org.springframework.security.authentication.InsufficientAuthenticationException("Vui lòng đăng nhập"));
        if(!passwords.matches(request.currentPassword(),user.getPasswordHash())) throw new org.springframework.security.access.AccessDeniedException("Mật khẩu hiện tại không đúng");
        if(passwords.matches(request.newPassword(),user.getPasswordHash())) throw new com.petcarehub.exception.BadRequestException("Mật khẩu mới phải khác mật khẩu hiện tại");
        user.setPasswordHash(passwords.encode(request.newPassword())); user.setMustChangePassword(false); user.setTokenVersion(user.getTokenVersion()+1);
        audit.record("USER_PASSWORD_CHANGED",user.getId(),java.util.Map.of("sessionsRevoked",true,"mustChangePassword",false));
    }
}
