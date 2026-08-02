package com.bihariecart;

import com.bihariecart.dto.WishlistCheckResponse;
import com.bihariecart.dto.WishlistCountResponse;
import com.bihariecart.dto.WishlistItemRequest;
import com.bihariecart.dto.WishlistItemResponse;
import com.bihariecart.entity.Category;
import com.bihariecart.entity.Product;
import com.bihariecart.entity.Role;
import com.bihariecart.entity.User;
import com.bihariecart.exception.DuplicateResourceException;
import com.bihariecart.exception.ResourceNotFoundException;
import com.bihariecart.repository.CategoryRepository;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.UserRepository;
import com.bihariecart.repository.WishlistItemRepository;
import com.bihariecart.service.WishlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@Transactional
public class WishlistServiceIntegrationTest {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private User otherUser;
    private Product product1;
    private Product product2;

    @BeforeEach
    public void setup() {
        String uniqueSuffix = java.util.UUID.randomUUID().toString();
        Category category = new Category();
        category.setName("Electronics-" + uniqueSuffix);
        categoryRepository.save(category);

        product1 = new Product();
        product1.setName("Laptop");
        product1.setPrice(new BigDecimal("1000.00"));
        product1.setStockQuantity(10);
        product1.setCategory(category);
        product1.setActive(true);
        productRepository.save(product1);

        product2 = new Product();
        product2.setName("Mouse");
        product2.setPrice(new BigDecimal("50.00"));
        product2.setStockQuantity(100);
        product2.setCategory(category);
        product2.setActive(true);
        productRepository.save(product2);

        user = new User();
        user.setEmail("user1-" + uniqueSuffix + "@example.com");
        user.setFullName("User One");
        user.setPassword("password");
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        otherUser = new User();
        otherUser.setEmail("user2-" + uniqueSuffix + "@example.com");
        otherUser.setFullName("User Two");
        otherUser.setPassword("password");
        otherUser.setRole(Role.ROLE_USER);
        userRepository.save(otherUser);
    }

    @Test
    public void testAddProductToWishlist() {
        WishlistItemRequest request = new WishlistItemRequest(product1.getId());
        WishlistItemResponse response = wishlistService.addProductToWishlist(user.getEmail(), request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(product1.getId(), response.getProductId());
        Assertions.assertEquals(1, wishlistItemRepository.countByUser(user));
    }

    @Test
    public void testDuplicateProductThrowsException() {
        WishlistItemRequest request = new WishlistItemRequest(product1.getId());
        wishlistService.addProductToWishlist(user.getEmail(), request);

        Assertions.assertThrows(DuplicateResourceException.class, () -> {
            wishlistService.addProductToWishlist(user.getEmail(), request);
        });
    }

    @Test
    public void testInvalidProductThrowsException() {
        WishlistItemRequest request = new WishlistItemRequest(9999L);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            wishlistService.addProductToWishlist(user.getEmail(), request);
        });
    }

    @Test
    public void testInactiveProductCannotBeAdded() {
        product1.setActive(false);
        productRepository.save(product1);

        WishlistItemRequest request = new WishlistItemRequest(product1.getId());
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            wishlistService.addProductToWishlist(user.getEmail(), request);
        });
    }

    @Test
    public void testViewWishlist() {
        wishlistService.addProductToWishlist(user.getEmail(), new WishlistItemRequest(product1.getId()));
        wishlistService.addProductToWishlist(user.getEmail(), new WishlistItemRequest(product2.getId()));

        List<WishlistItemResponse> wishlist = wishlistService.getUserWishlist(user.getEmail());
        Assertions.assertEquals(2, wishlist.size());
    }

    @Test
    public void testEmptyWishlist() {
        List<WishlistItemResponse> wishlist = wishlistService.getUserWishlist(user.getEmail());
        Assertions.assertTrue(wishlist.isEmpty());
    }

    @Test
    public void testRemoveProduct() {
        wishlistService.addProductToWishlist(user.getEmail(), new WishlistItemRequest(product1.getId()));
        Assertions.assertEquals(1, wishlistItemRepository.countByUser(user));

        wishlistService.removeProductFromWishlist(user.getEmail(), product1.getId());
        Assertions.assertEquals(0, wishlistItemRepository.countByUser(user));
    }

    @Test
    public void testRemoveInvalidProductGracefulError() {
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            wishlistService.removeProductFromWishlist(user.getEmail(), 9999L);
        });
    }

    @Test
    public void testWishlistCount() {
        wishlistService.addProductToWishlist(user.getEmail(), new WishlistItemRequest(product1.getId()));
        WishlistCountResponse response = wishlistService.getWishlistCount(user.getEmail());
        Assertions.assertEquals(1L, response.getCount());
    }

    @Test
    public void testWishlistCheck() {
        WishlistCheckResponse falseResponse = wishlistService.checkProductInWishlist(user.getEmail(), product1.getId());
        Assertions.assertFalse(falseResponse.getWishlisted());

        wishlistService.addProductToWishlist(user.getEmail(), new WishlistItemRequest(product1.getId()));

        WishlistCheckResponse trueResponse = wishlistService.checkProductInWishlist(user.getEmail(), product1.getId());
        Assertions.assertTrue(trueResponse.getWishlisted());
        Assertions.assertNotNull(trueResponse.getWishlistItemId());
    }

    @Test
    public void testUserIsolation() {
        wishlistService.addProductToWishlist(user.getEmail(), new WishlistItemRequest(product1.getId()));

        List<WishlistItemResponse> otherUserWishlist = wishlistService.getUserWishlist(otherUser.getEmail());
        Assertions.assertTrue(otherUserWishlist.isEmpty());

        WishlistCountResponse otherUserCount = wishlistService.getWishlistCount(otherUser.getEmail());
        Assertions.assertEquals(0L, otherUserCount.getCount());
    }

    @Test
    public void testReactivationFlowAndInactiveProductsHidden() {
        // 1. User adds product
        wishlistService.addProductToWishlist(user.getEmail(), new WishlistItemRequest(product1.getId()));
        Assertions.assertEquals(1, wishlistService.getUserWishlist(user.getEmail()).size());
        Assertions.assertEquals(1, wishlistService.getWishlistCount(user.getEmail()).getCount());

        // 2. Admin marks product inactive
        product1.setActive(false);
        productRepository.save(product1);

        // 3. Wishlist hides the product
        Assertions.assertEquals(0, wishlistService.getUserWishlist(user.getEmail()).size());
        Assertions.assertEquals(0, wishlistService.getWishlistCount(user.getEmail()).getCount());
        
        // Still exists structurally
        Assertions.assertTrue(wishlistService.checkProductInWishlist(user.getEmail(), product1.getId()).getWishlisted());

        // 4. Admin reactivates product
        product1.setActive(true);
        productRepository.save(product1);

        // 5. Wishlist shows it again
        Assertions.assertEquals(1, wishlistService.getUserWishlist(user.getEmail()).size());
        Assertions.assertEquals(1, wishlistService.getWishlistCount(user.getEmail()).getCount());
    }
}
