package com.bihariecart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing complete cart details in API responses.
 * Never exposes raw domain entities.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long id;
    private Long userId;
    private String userEmail;
    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();
    private Integer totalItems;
    private Double totalAmount;
}
