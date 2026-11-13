package com.petcarehub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="admin_audit_log") @org.hibernate.annotations.Immutable
@Getter @Builder @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor
public class AdminAuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,updatable=false) private LocalDateTime occurredAt;
    @Column(nullable=false,updatable=false) private Long actorUserId;
    @Column(nullable=false,updatable=false) private String actorRole;
    @Column(nullable=false,length=80,updatable=false) private String action;
    @Column(nullable=false,length=80,updatable=false) private String targetType;
    @Column(updatable=false) private Long targetId;
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=500,updatable=false) private String summary;
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=2000,updatable=false) private String details;
    @Column(length=64,updatable=false) private String ip;
    @Column(nullable=false,length=36,updatable=false) private String correlationId;
    @Column(nullable=false,length=20,updatable=false) private String result;
}
