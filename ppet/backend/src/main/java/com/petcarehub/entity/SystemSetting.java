package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="system_settings") @Getter @Setter @NoArgsConstructor
public class SystemSetting {
    @Id @Column(length=50) private String settingKey;
    @Column(nullable=false,precision=18,scale=2) private java.math.BigDecimal settingValue;
}
