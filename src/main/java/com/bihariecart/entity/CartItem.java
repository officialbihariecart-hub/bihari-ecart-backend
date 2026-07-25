package com.bihariecart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Column(nullable = false)
    private Double priceAtAddition;

    /**
     * Calculates the subtotal for this item dynamically (quantity * priceAtAddition).
     * 
     * @return dynamic line subtotal
     */
    public Double getSubTotal() {
        if (priceAtAddition == null || quantity == null) {
            return 0.0;
        }
        return priceAtAddition * quantity;
    }
}
