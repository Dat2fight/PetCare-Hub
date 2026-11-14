package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="promotion_audience",uniqueConstraints=@UniqueConstraint(columnNames= {
    "promotion_id","user_id"
}
)) @Getter @Setter @NoArgsConstructor public class PromotionAudience  {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long promotionId;
    @Column(nullable=false) private Long userId;
}
