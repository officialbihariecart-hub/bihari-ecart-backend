package com.bihariecart.service.impl;

import com.bihariecart.dto.AverageRatingResponse;
import com.bihariecart.dto.ReviewRequest;
import com.bihariecart.dto.ReviewResponse;
import com.bihariecart.entity.Product;
import com.bihariecart.entity.Review;
import com.bihariecart.entity.User;
import com.bihariecart.exception.DuplicateReviewException;
import com.bihariecart.exception.ResourceNotFoundException;
import com.bihariecart.repository.OrderItemRepository;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.ReviewRepository;
import com.bihariecart.repository.UserRepository;
import com.bihariecart.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Product Review and Rating operations in Bihari E-Cart.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public ReviewResponse createReview(String userEmail, ReviewRequest request) {
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required for creating a review");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        // Business Rule 2: User can review ONLY products they have purchased
        boolean hasPurchased = orderItemRepository.existsByOrderUserIdAndProductId(user.getId(), product.getId());
        if (!hasPurchased) {
            throw new IllegalArgumentException("User has not purchased product with id: " + product.getId());
        }

        // Business Rule 3: One user can review one product only once
        boolean alreadyReviewed = reviewRepository.existsByUserIdAndProductId(user.getId(), product.getId());
        if (alreadyReviewed) {
            throw new DuplicateReviewException("User has already reviewed product with id: " + product.getId());
        }

        Review review = Review.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .product(product)
                .user(user)
                .build();

        Review savedReview = reviewRepository.saveAndFlush(review);
        updateProductRating(product.getId());
        return mapToReviewResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewResponse updateReview(Long reviewId, String userEmail, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Business Rule 6: Owner validation
        if (!review.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You can only edit your own reviews");
        }

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        Review updatedReview = reviewRepository.saveAndFlush(review);
        updateProductRating(review.getProduct().getId());
        return mapToReviewResponse(updatedReview);
    }

    @Override
    public void deleteReview(Long reviewId, String userEmail) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Business Rule 7: Owner validation
        if (!review.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You can only delete your own reviews");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        reviewRepository.flush();
        updateProductRating(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public AverageRatingResponse getAverageRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        double averageRating = product.getAverageRating() != null ? product.getAverageRating() : 0.0;
        long totalReviews = product.getReviewCount() != null ? product.getReviewCount().longValue() : 0L;

        return AverageRatingResponse.builder()
                .productId(productId)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteReviewByAdmin(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        reviewRepository.flush();
        updateProductRating(productId);
    }

    @Override
    public void updateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Double rawAverage = reviewRepository.averageRatingByProductId(productId);
        Long totalCount = reviewRepository.countByProductId(productId);

        double averageRating = 0.0;
        int reviewCount = 0;

        if (totalCount != null && totalCount > 0 && rawAverage != null) {
            averageRating = Math.round(rawAverage * 10.0) / 10.0;
            reviewCount = totalCount.intValue();
        }

        product.setAverageRating(averageRating);
        product.setReviewCount(reviewCount);

        productRepository.save(product);
    }

    /**
     * Maps Review entity to ReviewResponse DTO.
     */
    private ReviewResponse mapToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(review.getUser() != null ? review.getUser().getFullName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
