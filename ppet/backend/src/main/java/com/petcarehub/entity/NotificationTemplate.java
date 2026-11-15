package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="notification_templates") @Getter @Setter @NoArgsConstructor
public class NotificationTemplate {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=60) private String code;
    @Column(nullable=false,length=20) private String purpose;
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=200) private String title;
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=2000) private String body;
    @Column(nullable=false) private boolean active=true;
}
