package com.bihariecart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for checking if a product is in the user's wishlist.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload representing whether a product is wishlisted")
public class WishlistCheckResponse {

    @Schema(description = "True if the product is in the user's wishlist", example = "true")
    private Boolean wishlisted;

    @Schema(description = "Unique ID of the wishlist item if wishlisted, null otherwise", example = "10")
    private Long wishlistItemId;

    @Schema(description = "ID of the product", example = "1")
    private Long productId;

    @Schema(description = "Timestamp when it was added to the wishlist, null otherwise", example = "2026-08-02T10:00:00")
    private LocalDateTime addedAt;
}
