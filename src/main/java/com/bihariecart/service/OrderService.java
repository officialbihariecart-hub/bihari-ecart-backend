package com.bihariecart.service;

import com.bihariecart.dto.OrderResponse;
import com.bihariecart.dto.PlaceOrderRequest;

import java.util.List;

/**
 * Service interface for Order management operations in Bihari E-Cart.
 */
public interface OrderService {

    /**
     * Places a new order for the authenticated user using items currently in their shopping cart.
     *
     * @param userEmail email of the authenticated user
     * @param request place order request containing shipping details
     * @return created OrderResponse
     */
    OrderResponse placeOrder(String userEmail, PlaceOrderRequest request);

    /**
     * Retrieves all orders placed by the logged-in user sorted newest first.
     *
     * @param userEmail email of the authenticated user
     * @param userEmail email of the authenticated user
     * @return list of OrderResponses
     */
    List<OrderResponse> getMyOrders(String userEmail);

    /**
     * Retrieves details of a specific order belonging to the logged-in user.
     *
     * @param orderId ID of the target order
     * @param userEmail email of the authenticated user
     * @return OrderResponse for the requested order
     */
    OrderResponse getOrderById(Long orderId, String userEmail);

    /**
     * Cancels a PENDING or PROCESSING order for the logged-in user.
     *
     * @param orderId ID of the order to cancel
     * @param userEmail email of the authenticated user
     * @return updated OrderResponse with CANCELLED status
     */
    OrderResponse cancelOrder(Long orderId, String userEmail);
}
