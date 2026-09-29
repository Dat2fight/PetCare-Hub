package com.petcarehub.event;

import com.petcarehub.entity.Review;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {
    private final Review review;

    public NotificationEvent(Object source, Review review) {
        super(source);
        this.review = review;
    }
}
