package com.petcarehub.entity;

import org.hibernate.Length;

import org.hibernate.annotations.Nationalized;

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

    @Nationalized
    @Column(nullable = false, length = 100)
    private String name;

    @Nationalized
    @Column(length = Length.LONG32)
    private String description;

    @Nationalized
    @Column(name = "avg_lifespan", length = 50)
    private String avgLifespan;

    @Nationalized
    @Column(name = "size_category", length = 50)
    private String sizeCategory;

    @Nationalized
    @Column(name = "energy_level", length = 50)
    private String energyLevel;

    @Nationalized
    @Column(name = "grooming_needs", length = 50)
    private String groomingNeeds;

    @Column(name = "good_with_children")
    private Boolean goodWithChildren;

    @Nationalized
    @Column(name = "noise_level", length = 50)
    private String noiseLevel;
}
