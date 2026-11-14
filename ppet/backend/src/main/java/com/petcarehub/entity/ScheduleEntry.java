package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@MappedSuperclass @Getter @Setter
public abstract class ScheduleEntry  {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Integer weekday;
    private LocalDate workDate;
    private Long staffId;
    private LocalTime startTime;
    private LocalTime endTime;
    @Column(nullable=false) private boolean closed;
    @org.hibernate.annotations.Nationalized @Column(length=500) private String reason;
}
