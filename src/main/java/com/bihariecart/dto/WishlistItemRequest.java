package com.bihariecart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for adding a product to the wishlist.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for adding a product to wishlist")
public class WishlistItemRequest {

    @NotNull(message = "Product ID is required")
    @Schema(description = "ID of the product to wishlist", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;
}
