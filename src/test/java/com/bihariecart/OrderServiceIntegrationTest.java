package com.bihariecart;

import com.bihariecart.dto.AddToCartRequest;
import com.bihariecart.dto.OrderResponse;
import com.bihariecart.dto.PlaceOrderRequest;
import com.bihariecart.entity.Category;
import com.bihariecart.entity.OrderStatus;
import com.bihariecart.entity.PaymentStatus;
import com.bihariecart.entity.Product;
import com.bihariecart.entity.User;
import com.bihariecart.repository.CategoryRepository;
import com.bihariecart.repository.OrderRepository;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.UserRepository;
import com.bihariecart.service.CartService;
import com.bihariecart.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest
@Transactional
public class OrderServiceIntegrationTest {

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
    private OrderRepository orderRepository;

    @Test
    public void testAddToCartAndPlaceOrderSuccess() {
        // 1. Create User
        User user = new User();
        user.setFullName("Test User");
        user.setEmail("testorder@example.com");
        user.setPassword("password123");
        userRepository.save(user);

        // 2. Create Category
        Category category = new Category();
        category.setName("Electronics Test");
        categoryRepository.save(category);

        // 3. Create Product
        Product product = new Product();
        product.setName("Samsung Galaxy S24");
        product.setPrice(79999.0);
        product.setStockQuantity(10);
        product.setCategory(category);
        product.setActive(true);
        productRepository.save(product);

        // 4. Add product to cart
        AddToCartRequest addRequest = new AddToCartRequest();
        addRequest.setProductId(product.getId());
        addRequest.setQuantity(2);
        cartService.addToCart(user.getEmail(), addRequest);

        // 5. Place Order
        PlaceOrderRequest orderRequest = new PlaceOrderRequest();
        orderRequest.setShippingAddress("123 Test Street, Bihar");
        OrderResponse response = orderService.placeOrder(user.getEmail(), orderRequest);

        // 6. Assertions
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getOrderId());
        Assertions.assertTrue(response.getOrderNumber().startsWith("ORD-"));
        Assertions.assertEquals(OrderStatus.PENDING, response.getStatus());
        Assertions.assertEquals(PaymentStatus.PENDING, response.getStatus().name().equals("PENDING") ? PaymentStatus.PENDING : PaymentStatus.FAILED);
        Assertions.assertEquals(0, new BigDecimal("159998.00").compareTo(response.getTotalAmount()));
        Assertions.assertEquals(1, response.getItems().size());
        Assertions.assertEquals("Samsung Galaxy S24", response.getItems().get(0).getProductName());
        Assertions.assertEquals(2, response.getItems().get(0).getQuantity());

        // 7. Stock quantity should be reduced from 10 to 8
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(8, updatedProduct.getStockQuantity());
    }
}
