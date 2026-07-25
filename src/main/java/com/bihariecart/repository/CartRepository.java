package com.bihariecart.repository;

import com.bihariecart.entity.Cart;
import com.bihariecart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Cart entity.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);

    Optional<Cart> findByUserId(Long userId);

    /**
     * Fetch Cart with items and associated products eagerly in a single JOIN query.
     * Prevents N+1 select performance issues.
     */
    @Query("SELECT DISTINCT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

    /**
     * Fetch Cart by User Email with items and products eagerly loaded.
     */
    @Query("SELECT DISTINCT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product WHERE c.user.email = :email")
    Optional<Cart> findByUserEmailWithItems(@Param("email") String email);
}
