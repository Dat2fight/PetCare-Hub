package com.petcarehub.entity;

import org.hibernate.Length;

import org.hibernate.annotations.Nationalized;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"category"})
@Data
public class Product extends BaseEntity {

    @Nationalized
    @Column(nullable = false)
    private String name;

    @Nationalized
    @Column(length = Length.LONG32)
    private String description;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(length = 500, name = "image_url")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(precision = 10, scale = 2)
    private BigDecimal weight;

    @Nationalized
    @Column(length = 100)
    private String dimensions;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
