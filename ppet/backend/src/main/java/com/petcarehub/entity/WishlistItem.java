package com.petcarehub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="wishlist_items")
@Getter @Setter
public class WishlistItem extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id")
    private User customer;
    @Column(nullable=false, length=12)
    private String type;
    @ManyToOne(fetch=FetchType.LAZY) private Product product;
    @ManyToOne(fetch=FetchType.LAZY) private Pet pet;
}
