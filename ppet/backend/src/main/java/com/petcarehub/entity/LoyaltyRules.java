package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="loyalty_rules") @Getter @Setter
public class LoyaltyRules  {
    @Id private Integer id=1;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal earnEvery=new BigDecimal("10000");
    @Column(nullable=false,precision=18,scale=2) private BigDecimal pointValue=new BigDecimal("100");
    @Column(nullable=false) private int silver=1000;
    @Column(nullable=false) private int gold=5000;
    @Column(nullable=false) private int platinum=10000;
}
