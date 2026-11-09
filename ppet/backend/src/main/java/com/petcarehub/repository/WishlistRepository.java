package com.petcarehub.repository;

import com.petcarehub.entity.WishlistItem;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface WishlistRepository extends JpaRepository<WishlistItem,Long> {
    @EntityGraph(attributePaths={"product","pet"})
    Page<WishlistItem> findByCustomerIdOrderByIdDesc(Long userId, Pageable page);
    Optional<WishlistItem> findByIdAndCustomerId(Long id, Long userId);
    @Query("select w from WishlistItem w where w.customer.id=:userId and w.type=:type and coalesce(w.product.id,w.pet.id) in :ids")
    List<WishlistItem> matches(Long userId, String type, Collection<Long> ids);
}
