package com.bihariecart;

import com.bihariecart.dto.AddToCartRequest;
import com.bihariecart.dto.CartResponse;
import com.bihariecart.dto.UpdateCartRequest;
import com.bihariecart.entity.Category;
import com.bihariecart.entity.Product;
import com.bihariecart.entity.Role;
import com.bihariecart.entity.User;
import com.bihariecart.repository.CategoryRepository;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.UserRepository;
import com.bihariecart.security.JwtService;
import com.bihariecart.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class CartControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private String jwtToken;
    private Product testProduct;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 1. Create Test User
        testUser = new User();
        testUser.setFullName("Controller Cart User");
        testUser.setEmail("controllercart@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRole(Role.ROLE_USER);
        testUser = userRepository.save(testUser);
        jwtToken = jwtService.generateToken(testUser.getEmail());

        // 2. Create Category & Product
        Category category = new Category();
        category.setName("Controller Category");
        categoryRepository.save(category);

        testProduct = new Product();
        testProduct.setName("Controller Phone");
        testProduct.setPrice(new BigDecimal("19999.00"));
        testProduct.setStockQuantity(10);
        testProduct.setCategory(category);
        testProduct.setActive(true);
        productRepository.save(testProduct);
    }

    @Test
    public void testAddToCart_Success_JWTAuthentication() throws Exception {
        AddToCartRequest addRequest = new AddToCartRequest(testProduct.getId(), 2);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalAmount").value(39998.0))
                .andExpect(jsonPath("$.items[0].productName").value("Controller Phone"));
    }

    @Test
    public void testGetCart_Success_JWTAuthentication() throws Exception {
        cartService.addToCart(testUser.getEmail(), new AddToCartRequest(testProduct.getId(), 1));

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalAmount").value(19999.0));
    }

    @Test
    public void testUpdateCartItem_Success_JWTAuthentication() throws Exception {
        CartResponse cart = cartService.addToCart(testUser.getEmail(), new AddToCartRequest(testProduct.getId(), 1));
        Long itemId = cart.getItems().get(0).getId();

        UpdateCartRequest updateRequest = new UpdateCartRequest(3);

        mockMvc.perform(put("/api/cart/update/" + itemId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalAmount").value(59997.0));
    }

    @Test
    public void testRemoveCartItem_Success_JWTAuthentication() throws Exception {
        CartResponse cart = cartService.addToCart(testUser.getEmail(), new AddToCartRequest(testProduct.getId(), 1));
        Long itemId = cart.getItems().get(0).getId();

        mockMvc.perform(delete("/api/cart/remove/" + itemId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    public void testClearCart_Success_JWTAuthentication() throws Exception {
        cartService.addToCart(testUser.getEmail(), new AddToCartRequest(testProduct.getId(), 2));

        mockMvc.perform(delete("/api/cart/clear")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared successfully"));
    }

    @Test
    public void testUnauthorizedAccess_NoJWTToken() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized — Please provide a valid JWT token"));
    }

    @Test
    public void testValidationError_InvalidRequestBody() throws Exception {
        // Invalid request with null quantity
        AddToCartRequest invalidRequest = new AddToCartRequest(testProduct.getId(), null);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
}
