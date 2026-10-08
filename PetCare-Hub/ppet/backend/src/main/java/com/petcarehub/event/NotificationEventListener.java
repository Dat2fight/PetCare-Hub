package com.petcarehub.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.petcarehub.entity.Notification;
import com.petcarehub.enums.NotificationType;
import com.petcarehub.repository.NotificationRepository;
import com.petcarehub.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor

public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNotificationEvent(NotificationEvent event) {
        // Here we can determine the target user based on reviewableType and reviewableId
        // For simplicity, we assume we notify a specific user or admin
        // Normally you would fetch the owner of the targetId
        
        Notification notification = Notification.builder()
                .user(event.getReview().getCustomer()) // Just as an example, should be target user
                .type(NotificationType.SYSTEM) // Or REVIEW_CREATED etc.
                .title("New Review Submitted")
                .message("A new review was submitted for " + event.getReview().getReviewableType())
                .read(false)
                .build();
                
        notification = notificationRepository.save(notification);
        
        webSocketNotificationService.sendNotification(notification.getUser().getId(), notification);
    }
}

