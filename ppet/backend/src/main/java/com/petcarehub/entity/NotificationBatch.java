package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="notification_batches") @Getter @Setter @NoArgsConstructor
public class NotificationBatch {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=36) private String requestKey;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false,length=64) private String fingerprint;
    @Column(nullable=false,length=20) private String status="QUEUED";
    @Column(nullable=false) private int total;
    @Column(nullable=false) private int sent;
    @Column(nullable=false) private int skipped;
    @Column(nullable=false) private int attempts;
    @Column(nullable=false) private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime finishedAt;
    @Column(length=40) private String errorCode;
}
