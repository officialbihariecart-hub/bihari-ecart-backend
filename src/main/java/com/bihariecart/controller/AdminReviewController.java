package com.bihariecart.controller;

import com.bihariecart.dto.ReviewResponse;
import com.bihariecart.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Admin Product Review operations.
 * Base URL: /api/admin/reviews
 * Restricted to users with ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "Admin Review Management", description = "Admin endpoints for viewing and managing all product reviews")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "View all reviews", description = "Admin endpoint to retrieve every product review submitted across the platform.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All reviews retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        List<ReviewResponse> responses = reviewService.getAllReviews();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete any review", description = "Admin endpoint to delete any product review by ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review deleted successfully by admin"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    public ResponseEntity<Map<String, String>> deleteReviewByAdmin(@PathVariable Long reviewId) {
        reviewService.deleteReviewByAdmin(reviewId);
        return ResponseEntity.ok(Map.of("message", "Review deleted successfully by admin with id: " + reviewId));
    }
}
