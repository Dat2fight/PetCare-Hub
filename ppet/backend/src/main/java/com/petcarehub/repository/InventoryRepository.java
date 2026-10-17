package com.petcarehub.repository;

import com.petcarehub.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.product.id = :productId")
    Optional<Inventory> findLockedByProductId(@org.springframework.data.repository.query.Param("productId") Long productId);
    List<Inventory> findByProductIdIn(java.util.Collection<Long> productIds);
    Optional<Inventory> findByPetId(Long petId);

    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.reorderPoint AND i.reorderPoint IS NOT NULL")
    List<Inventory> findLowStockItems();

    List<Inventory> findByQuantityLessThanEqual(Integer threshold);
}
