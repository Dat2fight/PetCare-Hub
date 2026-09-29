package com.petcarehub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pet_matching_quizzes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = {"customer", "results"})
public class PetMatchingQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "living_environment", length = 100)
    private String livingEnvironment;

    @Column(name = "house_size", length = 100)
    private String houseSize;

    @Column(name = "has_children")
    private Boolean hasChildren;

    @Column(name = "children_age_range", length = 100)
    private String childrenAgeRange;

    @Column(name = "activity_level", length = 100)
    private String activityLevel;

    @Column(name = "daily_care_hours", precision = 4, scale = 2)
    private BigDecimal dailyCareHours;

    @Column(name = "pet_experience", length = 100)
    private String petExperience;

    @Column(name = "size_preference", length = 100)
    private String sizePreference;

    @Column(name = "energy_preference", length = 100)
    private String energyPreference;

    @Column(name = "grooming_tolerance", length = 100)
    private String groomingTolerance;

    @Column(name = "noise_tolerance", length = 100)
    private String noiseTolerance;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PetMatchingResult> results = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
