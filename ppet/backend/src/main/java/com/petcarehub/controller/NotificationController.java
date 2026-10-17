package com.petcarehub.controller;

import com.petcarehub.dto.notification.NotificationDTO;
import com.petcarehub.entity.Notification;
import com.petcarehub.repository.NotificationRepository;
import com.petcarehub.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;

    @GetMapping
    public Page<NotificationDTO> list(@AuthenticationPrincipal UserDetailsImpl user, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable).map(this::dto);
    }
    @GetMapping("/{userId}")
    public Page<NotificationDTO> legacyList(@PathVariable Long userId, @AuthenticationPrincipal UserDetailsImpl user, Pageable pageable) {
        if (!user.getId().equals(userId)) throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xem thông báo này");
        return list(user, pageable);
    }
    @GetMapping("/unread-count")
    public java.util.Map<String, Long> count(@AuthenticationPrincipal UserDetailsImpl user) {
        return java.util.Map.of("count", notificationRepository.countByUserIdAndReadFalse(user.getId()));
    }
    @PutMapping("/{id}/read")
    @Transactional
    public void read(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl user) {
        Notification n = notificationRepository.findById(id).orElseThrow(() -> new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy thông báo"));
        if (!n.getUser().getId().equals(user.getId())) throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền sửa thông báo này");
        n.setRead(true); notificationRepository.save(n);
    }
    @PutMapping("/read-all")
    @Transactional
    public void readAll(@AuthenticationPrincipal UserDetailsImpl user) { notificationRepository.markAllAsRead(user.getId()); }

    private NotificationDTO dto(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId()); dto.setUserId(n.getUser().getId()); dto.setMessage(n.getMessage());
        dto.setRead(Boolean.TRUE.equals(n.getRead())); dto.setCreatedAt(n.getCreatedAt()); return dto;
    }
}
