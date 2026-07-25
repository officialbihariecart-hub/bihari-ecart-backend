package com.bihariecart.repository;

import com.bihariecart.entity.Cart;
import com.bihariecart.entity.CartItem;
import com.bihariecart.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for CartItem entity.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    void deleteByCart(Cart cart);

    void deleteByCartId(Long cartId);
}
