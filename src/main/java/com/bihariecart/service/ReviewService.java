package com.bihariecart.service;

import com.bihariecart.dto.AverageRatingResponse;
import com.bihariecart.dto.ReviewRequest;
import com.bihariecart.dto.ReviewResponse;

import java.util.List;

/**
 * Service interface for Product Review and Rating operations in Bihari E-Cart.
 */
public interface ReviewService {

    /**
     * Creates a new review for a product by the authenticated user.
     * Enforces purchase verification and duplicate review checks.
     *
     * @param userEmail email of authenticated customer
     * @param request review details request DTO
     * @return review response DTO
     */
    ReviewResponse createReview(String userEmail, ReviewRequest request);

    /**
     * Retrieves all reviews for a specific product.
     *
     * @param productId ID of product
     * @return list of review response DTOs
     */
    List<ReviewResponse> getReviewsByProduct(Long productId);

    /**
     * Updates an existing review owned by the authenticated user.
     *
     * @param reviewId ID of review to update
     * @param userEmail email of authenticated customer
     * @param request updated review request DTO
     * @return updated review response DTO
     */
    ReviewResponse updateReview(Long reviewId, String userEmail, ReviewRequest request);

    /**
     * Deletes a review owned by the authenticated user.
     *
     * @param reviewId ID of review to delete
     * @param userEmail email of authenticated customer
     */
    void deleteReview(Long reviewId, String userEmail);

    /**
     * Calculates the average rating and total review count for a product.
     *
     * @param productId ID of product
     * @return average rating response DTO
     */
    AverageRatingResponse getAverageRating(Long productId);

    /**
     * Retrieves all reviews across all products (Admin operation).
     *
     * @return list of all review response DTOs
     */
    List<ReviewResponse> getAllReviews();

    /**
     * Deletes any review by review ID (Admin operation).
     *
     * @param reviewId ID of review to delete
     */
    void deleteReviewByAdmin(Long reviewId);

    /**
     * Recalculates average rating and review count for a product and updates Product table.
     *
     * @param productId ID of product
     */
    void updateProductRating(Long productId);
}
