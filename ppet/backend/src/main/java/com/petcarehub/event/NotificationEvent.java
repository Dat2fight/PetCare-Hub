package com.petcarehub.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.petcarehub.entity.Review;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
@Data
@AllArgsConstructor
@Builder
public class NotificationEvent extends ApplicationEvent {
    private final Review review;

    public NotificationEvent(Object source, Review review) {
        super(source);
        this.review = review;
    }
}
