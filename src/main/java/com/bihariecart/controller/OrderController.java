package com.bihariecart.controller;

import com.bihariecart.dto.OrderResponse;
import com.bihariecart.dto.PlaceOrderRequest;
import com.bihariecart.entity.User;
import com.bihariecart.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Order Management in Bihari E-Cart.
 * Secured using JWT Authentication for all endpoints.
 * Base URL: /api/orders
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for placing, retrieving, and cancelling orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place")
    @Operation(summary = "Place a new order", description = "Converts items in current user's shopping cart into a placed order.")
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        OrderResponse response = orderService.placeOrder(email, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get current user orders", description = "Retrieves all orders placed by the logged-in user sorted newest first.")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        List<OrderResponse> responses = orderService.getMyOrders(email);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves details of a specific order belonging to the logged-in user.")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        OrderResponse response = orderService.getOrderById(orderId, email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels a pending or processing order for the logged-in user.")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        String email = getAuthenticatedUserEmail(authentication);
        OrderResponse response = orderService.cancelOrder(orderId, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Extracts authenticated user email securely from Spring Security Context.
     */
    private String getAuthenticatedUserEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getEmail();
        }
        return authentication.getName();
    }
}
