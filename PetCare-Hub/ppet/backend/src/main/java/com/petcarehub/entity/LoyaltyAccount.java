package com.petcarehub.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.petcarehub.enums.MembershipLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loyalty_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"customer"})
@Data
public class LoyaltyAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private User customer;

    @Column(name = "total_points", nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @Column(name = "available_points", nullable = false)
    @Builder.Default
    private Integer availablePoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_level", nullable = false, length = 50)
    @Builder.Default
    private MembershipLevel membershipLevel = MembershipLevel.BRONZE;
}
