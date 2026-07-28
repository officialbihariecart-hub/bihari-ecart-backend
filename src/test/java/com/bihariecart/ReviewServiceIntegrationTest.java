package com.bihariecart;

import com.bihariecart.dto.*;
import com.bihariecart.entity.*;
import com.bihariecart.exception.DuplicateReviewException;
import com.bihariecart.exception.ResourceNotFoundException;
import com.bihariecart.repository.*;
import com.bihariecart.service.CartService;
import com.bihariecart.service.OrderService;
import com.bihariecart.service.ReviewService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Transactional
public class ReviewServiceIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private User buyer;
    private User nonBuyer;
    private User admin;
    private Product product;

    @BeforeEach
    public void setUp() {
        // Create Buyer
        buyer = new User();
        buyer.setFullName("Buyer User");
        buyer.setEmail("buyer@example.com");
        buyer.setPassword("password123");
        buyer.setRole(Role.ROLE_USER);
        userRepository.save(buyer);

        // Create Non-Buyer
        nonBuyer = new User();
        nonBuyer.setFullName("Non Buyer User");
        nonBuyer.setEmail("nonbuyer@example.com");
        nonBuyer.setPassword("password123");
        nonBuyer.setRole(Role.ROLE_USER);
        userRepository.save(nonBuyer);

        // Create Admin
        admin = new User();
        admin.setFullName("Admin User");
        admin.setEmail("admin@example.com");
        admin.setPassword("password123");
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);

        // Create Category
        Category category = new Category();
        category.setName("Electronics Sprint 6");
        categoryRepository.save(category);

        // Create Product
        product = new Product();
        product.setName("Product Review Test Phone");
        product.setPrice(49999.0);
        product.setStockQuantity(20);
        product.setCategory(category);
        product.setActive(true);
        productRepository.save(product);

        // Buyer purchases the product
        AddToCartRequest cartRequest = new AddToCartRequest();
        cartRequest.setProductId(product.getId());
        cartRequest.setQuantity(1);
        cartService.addToCart(buyer.getEmail(), cartRequest);

        PlaceOrderRequest orderRequest = new PlaceOrderRequest();
        orderRequest.setShippingAddress("Patna, Bihar");
        orderService.placeOrder(buyer.getEmail(), orderRequest);
    }

    @Test
    public void testCreateReviewSuccess() {
        ReviewRequest request = ReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .comment("Superb smartphone!")
                .build();

        ReviewResponse response = reviewService.createReview(buyer.getEmail(), request);

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getReviewId());
        Assertions.assertEquals(product.getId(), response.getProductId());
        Assertions.assertEquals("Product Review Test Phone", response.getProductName());
        Assertions.assertEquals(buyer.getId(), response.getUserId());
        Assertions.assertEquals("Buyer User", response.getUserName());
        Assertions.assertEquals(5, response.getRating());
        Assertions.assertEquals("Superb smartphone!", response.getComment());
    }

    @Test
    public void testCreateReviewWithoutPurchaseFails() {
        ReviewRequest request = ReviewRequest.builder()
                .productId(product.getId())
                .rating(4)
                .comment("Have not bought it yet")
                .build();

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(nonBuyer.getEmail(), request)
        );

        Assertions.assertTrue(exception.getMessage().contains("has not purchased product"));
    }

    @Test
    public void testDuplicateReviewFails() {
        ReviewRequest request = ReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .comment("First review")
                .build();

        reviewService.createReview(buyer.getEmail(), request);

        ReviewRequest duplicateRequest = ReviewRequest.builder()
                .productId(product.getId())
                .rating(4)
                .comment("Second review attempt")
                .build();

        DuplicateReviewException exception = Assertions.assertThrows(
                DuplicateReviewException.class,
                () -> reviewService.createReview(buyer.getEmail(), duplicateRequest)
        );

        Assertions.assertTrue(exception.getMessage().contains("already reviewed product"));
    }

    @Test
    public void testUpdateReviewSuccessAndNonOwnerForbidden() {
        ReviewRequest createReq = ReviewRequest.builder()
                .productId(product.getId())
                .rating(3)
                .comment("Average phone")
                .build();

        ReviewResponse initial = reviewService.createReview(buyer.getEmail(), createReq);

        ReviewRequest updateReq = ReviewRequest.builder()
                .rating(4)
                .comment("Updated: Better than expected")
                .build();

        ReviewResponse updated = reviewService.updateReview(initial.getReviewId(), buyer.getEmail(), updateReq);
        Assertions.assertEquals(4, updated.getRating());
        Assertions.assertEquals("Updated: Better than expected", updated.getComment());

        // Attempt update by another user
        Assertions.assertThrows(
                AccessDeniedException.class,
                () -> reviewService.updateReview(initial.getReviewId(), nonBuyer.getEmail(), updateReq)
        );
    }

    @Test
    public void testDeleteReviewSuccessAndNonOwnerForbidden() {
        ReviewRequest createReq = ReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .comment("Will delete this")
                .build();

        ReviewResponse initial = reviewService.createReview(buyer.getEmail(), createReq);

        // Non-owner cannot delete
        Assertions.assertThrows(
                AccessDeniedException.class,
                () -> reviewService.deleteReview(initial.getReviewId(), nonBuyer.getEmail())
        );

        // Owner can delete
        reviewService.deleteReview(initial.getReviewId(), buyer.getEmail());

        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.getReviewsByProduct(product.getId()).stream()
                        .filter(r -> r.getReviewId().equals(initial.getReviewId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Review deleted"))
        );
    }

    @Test
    public void testGetAverageRating() {
        ReviewRequest req = ReviewRequest.builder()
                .productId(product.getId())
                .rating(4)
                .comment("Good purchase")
                .build();

        reviewService.createReview(buyer.getEmail(), req);

        AverageRatingResponse avgResponse = reviewService.getAverageRating(product.getId());

        Assertions.assertNotNull(avgResponse);
        Assertions.assertEquals(product.getId(), avgResponse.getProductId());
        Assertions.assertEquals(4.0, avgResponse.getAverageRating());
        Assertions.assertEquals(1L, avgResponse.getTotalReviews());
    }

    @Test
    public void testAdminViewAllAndDeleteAnyReview() {
        ReviewRequest req = ReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .comment("Admin test review")
                .build();

        ReviewResponse review = reviewService.createReview(buyer.getEmail(), req);

        List<ReviewResponse> allReviews = reviewService.getAllReviews();
        Assertions.assertTrue(allReviews.stream().anyMatch(r -> r.getReviewId().equals(review.getReviewId())));

        reviewService.deleteReviewByAdmin(review.getReviewId());

        List<ReviewResponse> remaining = reviewService.getAllReviews();
        Assertions.assertFalse(remaining.stream().anyMatch(r -> r.getReviewId().equals(review.getReviewId())));
    }

    @Test
    public void testProductRatingAutoUpdateLifecycleAndEdgeCase() {
        // Initially no reviews exist
        Product initialProduct = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(0.0, initialProduct.getAverageRating());
        Assertions.assertEquals(0, initialProduct.getReviewCount());

        // 1. Create Review (Rating 5)
        ReviewRequest createReq = ReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .comment("Outstanding product!")
                .build();
        ReviewResponse created = reviewService.createReview(buyer.getEmail(), createReq);

        Product productAfterCreate = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(5.0, productAfterCreate.getAverageRating());
        Assertions.assertEquals(1, productAfterCreate.getReviewCount());

        // 2. Update Review (Rating changed from 5 to 3)
        ReviewRequest updateReq = ReviewRequest.builder()
                .rating(3)
                .comment("Changed my mind, average")
                .build();
        reviewService.updateReview(created.getReviewId(), buyer.getEmail(), updateReq);

        Product productAfterUpdate = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(3.0, productAfterUpdate.getAverageRating());
        Assertions.assertEquals(1, productAfterUpdate.getReviewCount());

        // 3. Delete Review
        reviewService.deleteReview(created.getReviewId(), buyer.getEmail());

        Product productAfterDelete = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(0.0, productAfterDelete.getAverageRating());
        Assertions.assertEquals(0, productAfterDelete.getReviewCount());
    }
}
