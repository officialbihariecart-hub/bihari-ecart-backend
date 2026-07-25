package com.bihariecart.controller;

import com.bihariecart.dto.AddToCartRequest;
import com.bihariecart.dto.CartResponse;
import com.bihariecart.dto.UpdateCartRequest;
import com.bihariecart.entity.User;
import com.bihariecart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Shopping Cart management in Bihari E-Cart.
 * Requires JWT Authentication for all requests.
 * Uses SecurityContext Holder via Authentication injection.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart Management", description = "Endpoints for managing user shopping cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    @Operation(summary = "Add product to cart", description = "Adds a product to the authenticated user's cart or increments quantity if item already exists.")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        CartResponse response = cartService.addToCart(email, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Get current user cart", description = "Retrieves active shopping cart for the logged-in user.")
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        CartResponse response = cartService.getCart(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{itemId}")
    @Operation(summary = "Update cart item quantity", description = "Updates quantity of a line item in cart. If quantity is set to 0, item is removed.")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartRequest request,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        CartResponse response = cartService.updateCartItem(email, itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{itemId}")
    @Operation(summary = "Remove item from cart", description = "Removes a specific line item from the user's cart.")
    public ResponseEntity<CartResponse> removeCartItem(
            @PathVariable Long itemId,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        CartResponse response = cartService.removeCartItem(email, itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear user shopping cart", description = "Removes all items from the user's active shopping cart.")
    public ResponseEntity<Map<String, String>> clearCart(Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        cartService.clearCart(email);
        return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
    }

    /**
     * Extracts authenticated user email securely from Spring Security Context.
     */
    private String getAuthenticatedUserEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getEmail();
        }
        return authentication.getName();
    }
}
