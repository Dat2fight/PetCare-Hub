package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="promotion_campaigns") @Getter @Setter
public class PromotionCampaign  {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=36) private String requestKey;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false) private Long promotionId;
    @Column(nullable=false) private java.time.LocalDateTime createdAt;
    @Column(nullable=false) private Integer recipientCount;
    @Column(nullable=false,length=64) private String fingerprint;
}
