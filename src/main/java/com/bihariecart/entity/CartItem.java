package com.bihariecart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing an individual line item inside a Shopping Cart.
 * Maintains historical price snapshot (priceAtAddition) to handle price fluctuation safely.
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Price at addition is required")
    @Column(name = "price_at_addition", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtAddition;

    /**
     * Calculates the subtotal for this item dynamically (quantity * priceAtAddition).
     * 
     * @return dynamic line subtotal
     */
    public BigDecimal getSubTotal() {
        if (priceAtAddition == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return priceAtAddition.multiply(BigDecimal.valueOf(quantity));
    }
}
