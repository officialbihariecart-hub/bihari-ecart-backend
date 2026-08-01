package com.bihariecart;

import com.bihariecart.dto.AddToCartRequest;
import com.bihariecart.dto.CartResponse;
import com.bihariecart.dto.UpdateCartRequest;
import com.bihariecart.entity.Category;
import com.bihariecart.entity.Product;
import com.bihariecart.entity.Role;
import com.bihariecart.entity.User;
import com.bihariecart.exception.ResourceNotFoundException;
import com.bihariecart.repository.CategoryRepository;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.UserRepository;
import com.bihariecart.service.CartService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest
@Transactional
public class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private User user1;
    private User user2;
    private Product product1;
    private Product product2;
    private Product inactiveProduct;
    private Product outOfStockProduct;

    @BeforeEach
    public void setUp() {
        // Create User 1
        user1 = new User();
        user1.setFullName("Cart User One");
        user1.setEmail("cartuser1@example.com");
        user1.setPassword("password123");
        user1.setRole(Role.ROLE_USER);
        user1 = userRepository.save(user1);

        // Create User 2 (for User Isolation tests)
        user2 = new User();
        user2.setFullName("Cart User Two");
        user2.setEmail("cartuser2@example.com");
        user2.setPassword("password123");
        user2.setRole(Role.ROLE_USER);
        user2 = userRepository.save(user2);

        // Create Category
        Category category = new Category();
        category.setName("Cart Electronics");
        category = categoryRepository.save(category);

        // Create Product 1 (Active, Stock 10, Price 500.00)
        product1 = new Product();
        product1.setName("Wireless Mouse");
        product1.setPrice(new BigDecimal("500.00"));
        product1.setStockQuantity(10);
        product1.setCategory(category);
        product1.setActive(true);
        product1 = productRepository.save(product1);

        // Create Product 2 (Active, Stock 5, Price 1500.00)
        product2 = new Product();
        product2.setName("Mechanical Keyboard");
        product2.setPrice(new BigDecimal("1500.00"));
        product2.setStockQuantity(5);
        product2.setCategory(category);
        product2.setActive(true);
        product2 = productRepository.save(product2);

        // Create Inactive Product
        inactiveProduct = new Product();
        inactiveProduct.setName("Discontinued Headset");
        inactiveProduct.setPrice(new BigDecimal("800.00"));
        inactiveProduct.setStockQuantity(10);
        inactiveProduct.setCategory(category);
        inactiveProduct.setActive(false);
        inactiveProduct = productRepository.save(inactiveProduct);

        // Create Out of Stock Product
        outOfStockProduct = new Product();
        outOfStockProduct.setName("Sold Out Monitor");
        outOfStockProduct.setPrice(new BigDecimal("12000.00"));
        outOfStockProduct.setStockQuantity(0);
        outOfStockProduct.setCategory(category);
        outOfStockProduct.setActive(true);
        outOfStockProduct = productRepository.save(outOfStockProduct);
    }

    @Test
    public void testAddToCart_Success() {
        AddToCartRequest request = new AddToCartRequest(product1.getId(), 2);
        CartResponse response = cartService.addToCart(user1.getEmail(), request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.getItems().size());
        Assertions.assertEquals(2, response.getTotalItems());
        Assertions.assertEquals(0, new BigDecimal("1000.00").compareTo(response.getTotalAmount()));
        Assertions.assertEquals("Wireless Mouse", response.getItems().get(0).getProductName());
        Assertions.assertEquals(0, new BigDecimal("500.00").compareTo(response.getItems().get(0).getPriceAtAddition()));
        Assertions.assertEquals(0, new BigDecimal("1000.00").compareTo(response.getItems().get(0).getSubTotal()));
    }

    @Test
    public void testAddToCart_ExistingProduct_QuantityIncrement() {
        AddToCartRequest request1 = new AddToCartRequest(product1.getId(), 2);
        cartService.addToCart(user1.getEmail(), request1);

        AddToCartRequest request2 = new AddToCartRequest(product1.getId(), 3);
        CartResponse response = cartService.addToCart(user1.getEmail(), request2);

        Assertions.assertEquals(1, response.getItems().size());
        Assertions.assertEquals(5, response.getTotalItems());
        Assertions.assertEquals(0, new BigDecimal("2500.00").compareTo(response.getTotalAmount()));
        Assertions.assertEquals(5, response.getItems().get(0).getQuantity());
    }

    @Test
    public void testViewCart_Success() {
        cartService.addToCart(user1.getEmail(), new AddToCartRequest(product1.getId(), 1));
        cartService.addToCart(user1.getEmail(), new AddToCartRequest(product2.getId(), 2));

        CartResponse response = cartService.getCart(user1.getEmail());

        Assertions.assertNotNull(response);
        Assertions.assertEquals(2, response.getItems().size());
        Assertions.assertEquals(3, response.getTotalItems());
        Assertions.assertEquals(0, new BigDecimal("3500.00").compareTo(response.getTotalAmount()));
    }

    @Test
    public void testEmptyCart_ReturnsZeroTotals() {
        CartResponse response = cartService.getCart(user1.getEmail());

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getItems().isEmpty());
        Assertions.assertEquals(0, response.getTotalItems());
        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalAmount()));
    }

    @Test
    public void testUpdateCartQuantity_IncreaseAndDecrease() {
        CartResponse initial = cartService.addToCart(user1.getEmail(), new AddToCartRequest(product1.getId(), 2));
        Long itemId = initial.getItems().get(0).getId();

        // Update quantity to 4
        CartResponse updated = cartService.updateCartItem(user1.getEmail(), itemId, new UpdateCartRequest(4));
        Assertions.assertEquals(4, updated.getTotalItems());
        Assertions.assertEquals(0, new BigDecimal("2000.00").compareTo(updated.getTotalAmount()));

        // Update quantity to 1
        CartResponse decreased = cartService.updateCartItem(user1.getEmail(), itemId, new UpdateCartRequest(1));
        Assertions.assertEquals(1, decreased.getTotalItems());
        Assertions.assertEquals(0, new BigDecimal("500.00").compareTo(decreased.getTotalAmount()));
    }

    @Test
    public void testUpdateCartQuantity_ZeroRemovesItem() {
        CartResponse initial = cartService.addToCart(user1.getEmail(), new AddToCartRequest(product1.getId(), 2));
        Long itemId = initial.getItems().get(0).getId();

        CartResponse updated = cartService.updateCartItem(user1.getEmail(), itemId, new UpdateCartRequest(0));
        Assertions.assertTrue(updated.getItems().isEmpty());
        Assertions.assertEquals(0, updated.getTotalItems());
        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(updated.getTotalAmount()));
    }

    @Test
    public void testRemoveCartItem_Success() {
        CartResponse initial = cartService.addToCart(user1.getEmail(), new AddToCartRequest(product1.getId(), 2));
        Long itemId = initial.getItems().get(0).getId();

        CartResponse updated = cartService.removeCartItem(user1.getEmail(), itemId);
        Assertions.assertTrue(updated.getItems().isEmpty());
    }

    @Test
    public void testClearCart_Success() {
        cartService.addToCart(user1.getEmail(), new AddToCartRequest(product1.getId(), 2));
        cartService.addToCart(user1.getEmail(), new AddToCartRequest(product2.getId(), 1));

        cartService.clearCart(user1.getEmail());

        CartResponse cart = cartService.getCart(user1.getEmail());
        Assertions.assertTrue(cart.getItems().isEmpty());
        Assertions.assertEquals(0, cart.getTotalItems());
        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(cart.getTotalAmount()));
    }

    @Test
    public void testAddToCart_InvalidProductId_ThrowsException() {
        AddToCartRequest request = new AddToCartRequest(99999L, 1);

        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addToCart(user1.getEmail(), request)
        );
    }

    @Test
    public void testUpdateCartItem_InvalidCartItemId_ThrowsException() {
        UpdateCartRequest request = new UpdateCartRequest(2);

        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.updateCartItem(user1.getEmail(), 99999L, request)
        );
    }

    @Test
    public void testAddToCart_QuantityGreaterThanStock_ThrowsException() {
        // Stock for product1 is 10, try requesting 15
        AddToCartRequest request = new AddToCartRequest(product1.getId(), 15);

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(user1.getEmail(), request)
        );
        Assertions.assertTrue(ex.getMessage().contains("exceeds available stock"));
    }

    @Test
    public void testAddToCart_ProductOutOfStock_ThrowsException() {
        AddToCartRequest request = new AddToCartRequest(outOfStockProduct.getId(), 1);

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(user1.getEmail(), request)
        );
        Assertions.assertTrue(ex.getMessage().contains("out of stock"));
    }

    @Test
    public void testAddToCart_InactiveProduct_ThrowsException() {
        AddToCartRequest request = new AddToCartRequest(inactiveProduct.getId(), 1);

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(user1.getEmail(), request)
        );
        Assertions.assertTrue(ex.getMessage().contains("Cannot add inactive"));
    }

    @Test
    public void testUserIsolation_CannotModifyOtherUserCartItem() {
        // User 1 adds item to cart
        CartResponse user1Cart = cartService.addToCart(user1.getEmail(), new AddToCartRequest(product1.getId(), 2));
        Long user1ItemId = user1Cart.getItems().get(0).getId();

        // User 2 attempts to update User 1's cart item
        IllegalArgumentException updateEx = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateCartItem(user2.getEmail(), user1ItemId, new UpdateCartRequest(5))
        );
        Assertions.assertTrue(updateEx.getMessage().contains("does not belong to the current user's cart"));

        // User 2 attempts to remove User 1's cart item
        IllegalArgumentException removeEx = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> cartService.removeCartItem(user2.getEmail(), user1ItemId)
        );
        Assertions.assertTrue(removeEx.getMessage().contains("does not belong to the current user's cart"));
    }
}
