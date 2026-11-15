package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="content_revisions") @Getter @Setter @NoArgsConstructor
public class ContentRevision {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long contentId;
    @org.hibernate.annotations.Nationalized @Column(nullable=false,length=org.hibernate.Length.LONG32) private String snapshot;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false) private java.time.LocalDateTime createdAt;
}
