package com.petcarehub.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="admin_catalog_state") @Getter @Setter @NoArgsConstructor
public class AdminCatalogState  {
    @Id private Integer id;
}
