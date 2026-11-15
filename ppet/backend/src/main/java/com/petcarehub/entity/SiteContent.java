package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="site_content") @Getter @Setter @NoArgsConstructor
public class SiteContent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=100) private String slug;
    @Column(nullable=false,length=20) private String kind;
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=200) private String title;
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=org.hibernate.Length.LONG32) private String body;
    @Column(nullable=false,length=2000) private String configuration="{}";
    @org.hibernate.annotations.Nationalized @Column(length=org.hibernate.Length.LONG32) private String publishedSnapshot;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
