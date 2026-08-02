package com.bihariecart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for returning the user's wishlist count.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload representing the count of active wishlist items")
public class WishlistCountResponse {

    @Schema(description = "Total number of active products in the wishlist", example = "7")
    private Long count;
}
