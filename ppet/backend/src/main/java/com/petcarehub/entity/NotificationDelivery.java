package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="notification_deliveries",uniqueConstraints=@UniqueConstraint(columnNames={
    "batchId","userId"
}
)) @Getter @Setter @NoArgsConstructor
public class NotificationDelivery {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long batchId;
    @Column(nullable=false) private Long userId;
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=200) private String title;
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=2000) private String body;
    @Column(nullable=false,length=20) private String status="PENDING";
}
