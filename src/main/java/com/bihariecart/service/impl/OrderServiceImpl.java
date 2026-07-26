package com.bihariecart.service.impl;

import com.bihariecart.dto.OrderItemResponse;
import com.bihariecart.dto.OrderResponse;
import com.bihariecart.dto.PlaceOrderRequest;
import com.bihariecart.entity.Cart;
import com.bihariecart.entity.CartItem;
import com.bihariecart.entity.Order;
import com.bihariecart.entity.OrderItem;
import com.bihariecart.entity.OrderStatus;
import com.bihariecart.entity.PaymentStatus;
import com.bihariecart.entity.Product;
import com.bihariecart.entity.User;
import com.bihariecart.exception.ResourceNotFoundException;
import com.bihariecart.repository.CartItemRepository;
import com.bihariecart.repository.CartRepository;
import com.bihariecart.repository.OrderRepository;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.UserRepository;
import com.bihariecart.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service Implementation for Order management operations in Bihari E-Cart.
 * Ensures transactional integrity, accurate BigDecimal financial calculations,
 * stock reservation/restoration, and security isolation.
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponse placeOrder(String userEmail, PlaceOrderRequest request) {
        User user = getUserByEmail(userEmail);

        // 1. Fetch user's cart with items and products
        Cart cart = cartRepository.findByUserEmailWithItems(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userEmail));

        // 2. Validate non-empty cart
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with an empty shopping cart");
        }

        // 3. Validate product stock and deduct quantities
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product == null || Boolean.FALSE.equals(product.getActive())) {
                throw new IllegalArgumentException("Product '" + (product != null ? product.getName() : "Unknown")
                        + "' is no longer active or available for purchase");
            }
            if (product.getStockQuantity() == null || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product '" + product.getName()
                        + "'. Available: " + (product.getStockQuantity() != null ? product.getStockQuantity() : 0)
                        + ", requested: " + cartItem.getQuantity());
            }

            // Deduct stock quantity
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // 4. Generate unique order number (Format: ORD-YYYYMMDD-XXXXXX)
        String orderNumber = generateUniqueOrderNumber();

        // 5. Create Order entity
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        // 6. Copy CartItems to OrderItems with BigDecimal unit price snapshot
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice());

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(unitPrice)
                    .build();

            order.addOrderItem(orderItem);
        }

        // 7. Calculate total amount strictly using BigDecimal arithmetic
        order.calculateTotalAmount();

        // 8. Save Order (cascades saving of OrderItems)
        Order savedOrder = orderRepository.save(order);

        // 9. Clear user's cart
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();

        // 10. Return mapped response
        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Order> orders = orderRepository.findByUserIdWithItemsOrderByCreatedAtDesc(user.getId());
        return orders.stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId + " for user: " + userEmail));
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId + " for user: " + userEmail));

        // Allow cancellation only when status is PENDING or PROCESSING
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalArgumentException("Cannot cancel order with status '" + order.getStatus()
                    + "'. Only PENDING or PROCESSING orders can be cancelled.");
        }

        // Update status to CANCELLED
        order.setStatus(OrderStatus.CANCELLED);

        // Restore product stock quantity for every order item
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null && product.getStockQuantity() != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        Order updatedOrder = orderRepository.save(order);
        return mapToOrderResponse(updatedOrder);
    }

    /**
     * Generates a unique order number in the format: ORD-YYYYMMDD-XXXXXX
     */
    private String generateUniqueOrderNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber;
        do {
            String randomCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            orderNumber = "ORD-" + datePrefix + "-" + randomCode;
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .shippingAddress(order.getShippingAddress())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        Product product = item.getProduct();
        return OrderItemResponse.builder()
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(item.getSubTotal())
                .build();
    }
}
