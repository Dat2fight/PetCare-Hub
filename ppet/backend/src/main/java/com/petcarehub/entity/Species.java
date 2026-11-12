package com.petcarehub.entity;

import org.hibernate.Length;

import org.hibernate.annotations.Nationalized;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "species")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"breeds"})
@Data
public class Species {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Nationalized
    @Column(length = Length.LONG32)
    private String description;

    @OneToMany(mappedBy = "species", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Breed> breeds = new ArrayList<>();
}
