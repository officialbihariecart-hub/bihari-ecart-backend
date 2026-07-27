package com.bihariecart.controller;

import com.bihariecart.dto.OrderResponse;
import com.bihariecart.dto.UpdateOrderStatusRequest;
import com.bihariecart.dto.UpdatePaymentStatusRequest;
import com.bihariecart.entity.OrderStatus;
import com.bihariecart.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Admin Order Management in Bihari E-Cart.
 * Requires ADMIN role for all requests via Spring Security JWT.
 * Base URL: /api/admin/orders
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Order Management", description = "Admin endpoints for viewing, filtering, and updating all user orders")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders (Admin)", description = "Retrieves all orders placed across all users in the system, sorted newest first.")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> responses = orderService.getAllOrdersForAdmin();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter orders by status (Admin)", description = "Retrieves all orders matching a specific OrderStatus (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED).")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
        List<OrderResponse> responses = orderService.getOrdersByStatusForAdmin(status);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status (Admin)", description = "Updates order status with state transition validation rules.")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse response = orderService.updateOrderStatusForAdmin(orderId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/payment-status")
    @Operation(summary = "Update payment status (Admin)", description = "Updates order payment status (PENDING, COMPLETED, FAILED, REFUNDED).")
    public ResponseEntity<OrderResponse> updatePaymentStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        OrderResponse response = orderService.updatePaymentStatusForAdmin(orderId, request);
        return ResponseEntity.ok(response);
    }
}
