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
    Optional<Inventory> findByPetId(Long petId);

    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.reorderPoint AND i.reorderPoint IS NOT NULL")
    List<Inventory> findLowStockItems();

    List<Inventory> findByQuantityLessThanEqual(Integer threshold);
}
