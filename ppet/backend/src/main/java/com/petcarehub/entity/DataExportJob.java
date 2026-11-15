package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="data_export_jobs") @Getter @Setter @NoArgsConstructor
public class DataExportJob {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false,length=36) private String requestKey;
    @Column(nullable=false,length=64) private String fingerprint;
    @Column(nullable=false,length=20) private String kind;
    @Column(nullable=false,length=20) private String status="QUEUED";
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=1000) private String criteria;
    @org.hibernate.annotations.Nationalized @Column(length=org.hibernate.Length.LONG32) private String payload;
    private Integer rowCount;
    @Column(length=40) private String errorCode;
    @Column(nullable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
}
