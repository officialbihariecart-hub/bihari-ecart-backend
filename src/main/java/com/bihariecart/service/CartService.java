package com.bihariecart.service;

import com.bihariecart.dto.AddToCartRequest;
import com.bihariecart.dto.CartResponse;
import com.bihariecart.dto.UpdateCartRequest;

/**
 * Service contract interface defining Shopping Cart operations in Bihari E-Cart.
 * Adheres to Interface Segregation and Single Responsibility Principles.
 */
public interface CartService {

    /**
     * Adds a product to the authenticated user's shopping cart.
     * Increments quantity if product already exists in cart.
     * 
     * @param userEmail Email of logged-in user
     * @param request AddToCartRequest containing product ID and quantity
     * @return Updated CartResponse DTO
     */
    CartResponse addToCart(String userEmail, AddToCartRequest request);

    /**
     * Retrieves the current shopping cart for the authenticated user.
     * Creates a new cart if the user does not have one yet.
     * 
     * @param userEmail Email of logged-in user
     * @return CartResponse DTO
     */
    CartResponse getCart(String userEmail);

    /**
     * Updates quantity for a specific cart item belonging to the authenticated user.
     * Removes the item if quantity is set to 0.
     * 
     * @param userEmail Email of logged-in user
     * @param itemId ID of the CartItem to update
     * @param request UpdateCartRequest containing target quantity
     * @return Updated CartResponse DTO
     */
    CartResponse updateCartItem(String userEmail, Long itemId, UpdateCartRequest request);

    /**
     * Removes a specific cart item from the authenticated user's cart.
     * 
     * @param userEmail Email of logged-in user
     * @param itemId ID of the CartItem to remove
     * @return Updated CartResponse DTO
     */
    CartResponse removeCartItem(String userEmail, Long itemId);

    /**
     * Empties all items from the authenticated user's cart.
     * 
     * @param userEmail Email of logged-in user
     */
    void clearCart(String userEmail);
}
