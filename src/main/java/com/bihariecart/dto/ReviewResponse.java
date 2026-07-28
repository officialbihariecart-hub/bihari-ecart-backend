package com.bihariecart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing detailed review details in API responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload representing a product review")
public class ReviewResponse {

    @Schema(description = "Unique ID of the review", example = "10")
    private Long reviewId;

    @Schema(description = "ID of the reviewed product", example = "1")
    private Long productId;

    @Schema(description = "Name of the reviewed product", example = "Samsung Galaxy S24")
    private String productName;

    @Schema(description = "ID of the reviewing user", example = "5")
    private Long userId;

    @Schema(description = "Full name of the reviewing user", example = "John Doe")
    private String userName;

    @Schema(description = "Rating score between 1 and 5", example = "5")
    private Integer rating;

    @Schema(description = "Optional review text or comments", example = "Outstanding product, high quality build!")
    private String comment;

    @Schema(description = "Timestamp when the review was created", example = "2026-07-28T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the review was last updated", example = "2026-07-28T10:00:00")
    private LocalDateTime updatedAt;
}
