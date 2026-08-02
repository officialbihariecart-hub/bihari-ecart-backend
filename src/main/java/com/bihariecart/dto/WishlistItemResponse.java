package com.bihariecart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a product in the user's wishlist.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload representing a wishlisted item")
public class WishlistItemResponse {

    @Schema(description = "Unique ID of the wishlist item record", example = "10")
    private Long id;

    @Schema(description = "ID of the wishlisted product", example = "1")
    private Long productId;

    @Schema(description = "Name of the wishlisted product", example = "Samsung Galaxy S24")
    private String productName;

    @Schema(description = "Current price of the product", example = "999.99")
    private BigDecimal productPrice;

    @Schema(description = "URL of the product image", example = "http://example.com/image.jpg")
    private String productImageUrl;

    @Schema(description = "Whether the product is currently in stock", example = "true")
    private Boolean inStock;

    @Schema(description = "Timestamp when the product was added to wishlist", example = "2026-08-02T10:00:00")
    private LocalDateTime addedAt;
}
