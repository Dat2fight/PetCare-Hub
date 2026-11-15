package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
@Entity @Table(name="return_requests") @Getter @Setter @NoArgsConstructor
public class ReturnRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long orderId;
    @Column(nullable=false) private Long itemId;
    @Column(nullable=false) private Long customerId;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false,length=20) private String status="REQUESTED";
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=500) private String reason;
    @org.hibernate.annotations.Nationalized @Column(length=500) private String decisionReason;
    @Column(nullable=false,length=36) private String requestKey;
    @Column(nullable=false,length=64) private String fingerprint;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal maximumRefund;
    @Column(precision=18,scale=2) private BigDecimal refundAmount;
    @org.hibernate.annotations.Nationalized @Column(length=100) private String refundReference;
    @Column(nullable=false) private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long handledBy;
}
