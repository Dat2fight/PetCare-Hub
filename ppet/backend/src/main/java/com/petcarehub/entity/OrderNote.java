package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="order_notes") @Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderNote {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long orderId;
    @Column(nullable=false) private Long actorId;
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=2000) private String body;
    @Column(nullable=false) private java.time.LocalDateTime createdAt;
}
