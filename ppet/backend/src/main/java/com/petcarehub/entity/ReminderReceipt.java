package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="reminder_receipts",uniqueConstraints=@UniqueConstraint(columnNames={
    "kind","sourceId","sourceDate","sourceTime"
}
)) @Getter @Setter @NoArgsConstructor
public class ReminderReceipt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=20) private String kind;
    @Column(nullable=false) private Long sourceId;
    @Column(nullable=false) private java.time.LocalDate sourceDate;
    @Column(nullable=false) private java.time.LocalTime sourceTime;
    @Column(nullable=false) private java.time.LocalDateTime createdAt;
}
