package com.bihariecart.repository;

import com.bihariecart.entity.Product;
import com.bihariecart.entity.Review;
import com.bihariecart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Review entity.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct(Product product);

    List<Review> findByProductId(Long productId);

    @Query("SELECT r FROM Review r JOIN FETCH r.product JOIN FETCH r.user WHERE r.product.id = :productId ORDER BY r.createdAt DESC")
    List<Review> findByProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);

    List<Review> findByUser(User user);

    List<Review> findByUserId(Long userId);

    boolean existsByUserAndProduct(User user, Product product);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double averageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product")
    Double averageRating(@Param("product") Product product);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Long countByProductId(@Param("productId") Long productId);

    @Query("SELECT r FROM Review r JOIN FETCH r.product JOIN FETCH r.user ORDER BY r.createdAt DESC")
    List<Review> findAllByOrderByCreatedAtDesc();
}
