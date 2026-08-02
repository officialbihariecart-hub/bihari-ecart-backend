package com.bihariecart.controller;

import com.bihariecart.dto.WishlistCheckResponse;
import com.bihariecart.dto.WishlistCountResponse;
import com.bihariecart.dto.WishlistItemRequest;
import com.bihariecart.dto.WishlistItemResponse;
import com.bihariecart.entity.User;
import com.bihariecart.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for User Wishlist operations.
 * Base URL: /api/wishlist
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist Management", description = "Endpoints for managing user wishlists")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping
    @Operation(summary = "Add product to wishlist", description = "Adds a specific product to the authenticated user's wishlist.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<WishlistItemResponse> addProductToWishlist(
            @Valid @RequestBody WishlistItemRequest request,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        WishlistItemResponse response = wishlistService.addProductToWishlist(email, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove product from wishlist", description = "Removes a product from the user's wishlist.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> removeProductFromWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        wishlistService.removeProductFromWishlist(email, productId);
        return ResponseEntity.ok(Map.of("message", "Product removed from wishlist"));
    }

    @GetMapping
    @Operation(summary = "View user wishlist", description = "Returns all active products in the user's wishlist.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<WishlistItemResponse>> getUserWishlist(
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        List<WishlistItemResponse> responses = wishlistService.getUserWishlist(email);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{productId}/exists")
    @Operation(summary = "Check if product is wishlisted", description = "Checks whether a given product is currently in the user's wishlist.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<WishlistCheckResponse> checkProductInWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        WishlistCheckResponse response = wishlistService.checkProductInWishlist(email, productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    @Operation(summary = "Get wishlist count", description = "Returns the total number of active products in the user's wishlist.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<WishlistCountResponse> getWishlistCount(
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        WishlistCountResponse response = wishlistService.getWishlistCount(email);
        return ResponseEntity.ok(response);
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
