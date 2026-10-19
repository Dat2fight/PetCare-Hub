package com.petcarehub.repository;

import com.petcarehub.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomerId(Long customerId);
    // Scalar lookup does not preload mutable cart/items before acquiring the customer lock.
    @org.springframework.data.jpa.repository.Query("select i.product.id from CartItem i where i.cart.customer.id = :userId and i.id = :itemId")
    Optional<Long> findProductIdForItem(Long userId, Long itemId);
}
