package com.petcarehub.entity;

import org.hibernate.Length;

import org.hibernate.annotations.Nationalized;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"customer"})
@Data
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "reviewable_type", nullable = false, length = 50)
    private String reviewableType;

    @Column(name = "reviewable_id", nullable = false)
    private Long reviewableId;

    @Column(nullable = false)
    private Integer rating;
    @Nationalized
    private String moderationState;
    @Nationalized
    @Column(length=500)
    private String moderationReason;

    @Nationalized
    @Column(length = Length.LONG32)
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean approved = false;
}
