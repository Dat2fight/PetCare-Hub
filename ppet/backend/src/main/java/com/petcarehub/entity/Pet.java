package com.petcarehub.entity;

import org.hibernate.Length;

import org.hibernate.annotations.Nationalized;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.petcarehub.enums.AvailabilityStatus;
import com.petcarehub.enums.HealthStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"species", "breed", "owner", "reservedOrder"})
@Data
public class Pet extends BaseEntity {

    @Nationalized
    @Column(length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id")
    private Breed breed;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Nationalized
    @Column(length = 20)
    private String gender;

    @Nationalized
    @Column(length = 100)
    private String color;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    // Nullable means not assessed; never infer these from age, breed or description.
    @Column(name = "activity_level", length = 10)
    private String activityLevel;

    @Column(name = "suitable_for_apartment")
    private Boolean suitableForApartment;

    @Column(name = "good_with_children")
    private Boolean goodWithChildren;

    @Column(name = "minimum_daily_care_hours", precision = 4, scale = 2)
    private BigDecimal minimumDailyCareHours;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Nationalized
    @Column(length = Length.LONG32)
    private String description;

    @Column(length = 500, name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 50)
    @Builder.Default
    private HealthStatus healthStatus = HealthStatus.HEALTHY;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 50)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    // The reservation belongs to one order; retries must never release another order's pet.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_order_id")
    private Order reservedOrder;
}
