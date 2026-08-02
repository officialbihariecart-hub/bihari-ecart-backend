package com.bihariecart.repository;

import com.bihariecart.entity.Product;
import com.bihariecart.entity.User;
import com.bihariecart.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for WishlistItem entity.
 */
@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    boolean existsByUserAndProduct(User user, Product product);

    Optional<WishlistItem> findByUserAndProduct(User user, Product product);

    void deleteByUserAndProduct(User user, Product product);

    @Query("SELECT COUNT(w) FROM WishlistItem w JOIN w.product p WHERE w.user = :user AND p.active = true")
    long countByUser(@Param("user") User user);

    @Query("SELECT w FROM WishlistItem w JOIN FETCH w.product p WHERE w.user = :user AND p.active = true ORDER BY w.createdAt DESC")
    List<WishlistItem> findActiveWishlistItemsByUser(@Param("user") User user);
}
