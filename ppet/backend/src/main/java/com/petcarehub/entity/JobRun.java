package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="job_runs") @Getter @Setter @NoArgsConstructor
public class JobRun {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=40) private String name;
    @Column(nullable=false) private java.time.LocalDateTime startedAt;
    private java.time.LocalDateTime finishedAt;
    @Column(nullable=false) private int processed;
    @Column(nullable=false,length=20) private String status="RUNNING";
    @Column(length=40) private String errorCode;
}
