package com.bihariecart;

import com.bihariecart.dto.WishlistItemRequest;
import com.bihariecart.entity.Category;
import com.bihariecart.entity.Product;
import com.bihariecart.entity.Role;
import com.bihariecart.entity.User;
import com.bihariecart.repository.CategoryRepository;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.UserRepository;
import com.bihariecart.repository.WishlistItemRepository;
import com.bihariecart.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class WishlistControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JwtService jwtService;

    private User user;
    private Product product;
    private String jwtToken;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String uniqueSuffix = java.util.UUID.randomUUID().toString();
        Category category = new Category();
        category.setName("Home Appliances-" + uniqueSuffix);
        categoryRepository.save(category);

        product = new Product();
        product.setName("Vacuum Cleaner");
        product.setPrice(new BigDecimal("150.00"));
        product.setStockQuantity(50);
        product.setCategory(category);
        product.setActive(true);
        productRepository.save(product);

        user = new User();
        user.setEmail("testuser-" + uniqueSuffix + "@example.com");
        user.setFullName("Test User");
        user.setPassword("password");
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        jwtToken = "Bearer " + jwtService.generateToken(user.getEmail());
    }

    @Test
    public void testAddProductToWishlist() throws Exception {
        WishlistItemRequest request = new WishlistItemRequest(product.getId());

        mockMvc.perform(post("/api/wishlist")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(product.getId()))
                .andExpect(jsonPath("$.productName").value(product.getName()));
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        WishlistItemRequest request = new WishlistItemRequest(product.getId());

        mockMvc.perform(post("/api/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testViewWishlist() throws Exception {
        // Add to wishlist first
        mockMvc.perform(post("/api/wishlist")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WishlistItemRequest(product.getId()))))
                .andExpect(status().isCreated());

        // View it
        mockMvc.perform(get("/api/wishlist")
                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(product.getId()));
    }

    @Test
    public void testRemoveProductFromWishlist() throws Exception {
        // Add
        mockMvc.perform(post("/api/wishlist")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WishlistItemRequest(product.getId()))))
                .andExpect(status().isCreated());

        // Remove
        mockMvc.perform(delete("/api/wishlist/" + product.getId())
                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product removed from wishlist"));

        // Verify count
        mockMvc.perform(get("/api/wishlist/count")
                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    public void testCheckProductExistsInWishlist() throws Exception {
        // Check before adding
        mockMvc.perform(get("/api/wishlist/" + product.getId() + "/exists")
                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wishlisted").value(false));

        // Add
        mockMvc.perform(post("/api/wishlist")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WishlistItemRequest(product.getId()))))
                .andExpect(status().isCreated());

        // Check after adding
        mockMvc.perform(get("/api/wishlist/" + product.getId() + "/exists")
                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wishlisted").value(true))
                .andExpect(jsonPath("$.productId").value(product.getId()))
                .andExpect(jsonPath("$.wishlistItemId").isNotEmpty());
    }

    @Test
    public void testGetWishlistCount() throws Exception {
        mockMvc.perform(post("/api/wishlist")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WishlistItemRequest(product.getId()))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/wishlist/count")
                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }
}
