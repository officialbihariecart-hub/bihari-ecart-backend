package com.bihariecart.controller;

import com.bihariecart.dto.AverageRatingResponse;
import com.bihariecart.dto.ReviewRequest;
import com.bihariecart.dto.ReviewResponse;
import com.bihariecart.entity.User;
import com.bihariecart.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * REST Controller for Customer and Public Product Review operations.
 * Base URL: /api/reviews
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Product Review Management", description = "Customer & Public endpoints for product reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Submit a product review", description = "Allows an authenticated user to submit a review for a product they have purchased.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Review submitted successfully",
                content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid rating or user has not purchased product"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
        @ApiResponse(responseCode = "404", description = "Product or User not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate review - Product already reviewed by user")
    })
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        ReviewResponse response = reviewService.createReview(email, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get reviews by product ID", description = "Public endpoint to retrieve all reviews for a specific product.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reviews fetched successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<List<ReviewResponse>> getReviewsByProduct(@PathVariable Long productId) {
        List<ReviewResponse> responses = reviewService.getReviewsByProduct(productId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update own review", description = "Allows an authenticated user to update their previously submitted review.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review updated successfully",
                content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload or rating"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Cannot edit review owned by another user"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        ReviewResponse response = reviewService.updateReview(reviewId, email, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete own review", description = "Allows an authenticated user to delete their own review.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Cannot delete review owned by another user"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<Map<String, String>> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        reviewService.deleteReview(reviewId, email);
        return ResponseEntity.ok(Map.of("message", "Review deleted successfully with id: " + reviewId));
    }

    @GetMapping("/product/{productId}/average")
    @Operation(summary = "Get product average rating", description = "Public endpoint to retrieve average rating and total review count for a product.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Average rating retrieved successfully",
                content = @Content(schema = @Schema(implementation = AverageRatingResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<AverageRatingResponse> getAverageRating(@PathVariable Long productId) {
        AverageRatingResponse response = reviewService.getAverageRating(productId);
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
