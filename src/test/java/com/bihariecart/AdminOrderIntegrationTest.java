package com.bihariecart;

import com.bihariecart.entity.Category;
import com.bihariecart.entity.Order;
import com.bihariecart.entity.OrderItem;
import com.bihariecart.entity.OrderStatus;
import com.bihariecart.entity.PaymentStatus;
import com.bihariecart.entity.Product;
import com.bihariecart.entity.Role;
import com.bihariecart.entity.User;
import com.bihariecart.repository.CategoryRepository;
import com.bihariecart.repository.OrderRepository;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.UserRepository;
import com.bihariecart.security.JwtService;
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
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class AdminOrderIntegrationTest {

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
        private OrderRepository orderRepository;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private User adminUser;
        private User normalUser;
        private String adminToken;
        private String userToken;
        private Order testOrder;

        @BeforeEach
        public void setup() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();

                // 1. Create Admin User
                adminUser = new User();
                adminUser.setFullName("Admin User");
                adminUser.setEmail("admin@bihariecart.com");
                adminUser.setPassword(passwordEncoder.encode("admin123"));
                adminUser.setRole(Role.ROLE_ADMIN);
                adminUser = userRepository.save(adminUser);
                adminToken = jwtService.generateToken(adminUser.getEmail());

                // 2. Create Normal User
                normalUser = new User();
                normalUser.setFullName("Normal User");
                normalUser.setEmail("normaluser@bihariecart.com");
                normalUser.setPassword(passwordEncoder.encode("user123"));
                normalUser.setRole(Role.ROLE_USER);
                normalUser = userRepository.save(normalUser);
                userToken = jwtService.generateToken(normalUser.getEmail());

                // 3. Create Category & Product
                Category category = new Category();
                category.setName("Admin Electronics");
                categoryRepository.save(category);

                Product product = new Product();
                product.setName("Admin Test Phone");
                product.setPrice(new BigDecimal("49999.00"));
                product.setStockQuantity(20);
                product.setCategory(category);
                product.setActive(true);
                productRepository.save(product);

                // 4. Create Order
                testOrder = Order.builder()
                                .orderNumber("ORD-ADMIN-0001")
                                .user(normalUser)
                                .shippingAddress("123 Admin Test St")
                                .status(OrderStatus.PENDING)
                                .paymentStatus(PaymentStatus.PENDING)
                                .totalAmount(new BigDecimal("49999.00"))
                                .build();

                OrderItem item = OrderItem.builder()
                                .product(product)
                                .quantity(1)
                                .price(new BigDecimal("49999.00"))
                                .build();
                testOrder.addOrderItem(item);

                testOrder = orderRepository.save(testOrder);
        }

        @Test
        public void testGetAllOrders_Success_AdminRole() throws Exception {
                mockMvc.perform(get("/api/admin/orders")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].orderNumber").value("ORD-ADMIN-0001"))
                                .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        public void testGetOrdersByStatus_Success_AdminRole() throws Exception {
                mockMvc.perform(get("/api/admin/orders/status/PENDING")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        public void testUpdateOrderStatus_Success_AdminRole() throws Exception {
                Map<String, String> requestBody = Map.of("status", "PROCESSING");

                mockMvc.perform(put("/api/admin/orders/" + testOrder.getId() + "/status")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("PROCESSING"));
        }

        @Test
        public void testUpdatePaymentStatus_Success_AdminRole() throws Exception {
                Map<String, String> requestBody = Map.of("paymentStatus", "COMPLETED");

                mockMvc.perform(put("/api/admin/orders/" + testOrder.getId() + "/payment-status")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
        }

        @Test
        public void testAdminEndpoint_Unauthorized_NoToken() throws Exception {
                mockMvc.perform(get("/api/admin/orders"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        public void testAdminEndpoint_Forbidden_NormalUserRole() throws Exception {
                mockMvc.perform(get("/api/admin/orders")
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.error")
                                                .value("Forbidden — Access denied. Admin privileges required."));
        }

        @Test
        public void testUpdateOrderStatus_InvalidOrderId_NotFound() throws Exception {
                Map<String, String> requestBody = Map.of("status", "PROCESSING");

                mockMvc.perform(put("/api/admin/orders/99999/status")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Order not found with id: 99999"));
        }

        @Test
        public void testUpdateOrderStatus_InvalidCancelledTransition_BadRequest() throws Exception {
                // First cancel order
                testOrder.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(testOrder);

                // Try updating cancelled order to PROCESSING
                Map<String, String> requestBody = Map.of("status", "PROCESSING");

                mockMvc.perform(put("/api/admin/orders/" + testOrder.getId() + "/status")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Cannot update status of a CANCELLED order"));
        }
}
