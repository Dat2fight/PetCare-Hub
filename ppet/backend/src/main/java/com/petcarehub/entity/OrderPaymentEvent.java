package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="order_payment_events",uniqueConstraints=@UniqueConstraint(columnNames={"order_id","event_type"}))
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderPaymentEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long orderId;
    @Column(nullable=false,length=40) private String eventType;
    @Column(nullable=false,precision=18,scale=2) private java.math.BigDecimal amount;
    @org.hibernate.annotations.Nationalized @Column(length=100) private String reference;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false) private java.time.LocalDateTime occurredAt;
}
