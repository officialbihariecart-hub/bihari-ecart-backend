package com.bihariecart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object representing average rating summary for a product.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload containing product average rating statistics")
public class AverageRatingResponse {

    @Schema(description = "ID of the product", example = "1")
    private Long productId;

    @Schema(description = "Average rating score rounded to 1 decimal place", example = "4.5")
    private Double averageRating;

    @Schema(description = "Total number of reviews submitted for the product", example = "12")
    private Long totalReviews;
}
