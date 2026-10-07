package com.petcarehub.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "breeds")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"species"})
@Data
public class Breed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "avg_lifespan", length = 50)
    private String avgLifespan;

    @Column(name = "size_category", length = 50)
    private String sizeCategory;

    @Column(name = "energy_level", length = 50)
    private String energyLevel;

    @Column(name = "grooming_needs", length = 50)
    private String groomingNeeds;

    @Column(name = "good_with_children")
    private Boolean goodWithChildren;

    @Column(name = "noise_level", length = 50)
    private String noiseLevel;
}
