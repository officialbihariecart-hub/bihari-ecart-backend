package com.bihariecart.dto;

import com.bihariecart.entity.OrderStatus;
import com.bihariecart.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing order details in API responses.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();
}
