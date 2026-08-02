package com.bihariecart.service;

import com.bihariecart.dto.WishlistCheckResponse;
import com.bihariecart.dto.WishlistCountResponse;
import com.bihariecart.dto.WishlistItemRequest;
import com.bihariecart.dto.WishlistItemResponse;

import java.util.List;

/**
 * Interface for Wishlist business logic operations.
 */
public interface WishlistService {

    WishlistItemResponse addProductToWishlist(String email, WishlistItemRequest request);

    void removeProductFromWishlist(String email, Long productId);

    List<WishlistItemResponse> getUserWishlist(String email);

    WishlistCheckResponse checkProductInWishlist(String email, Long productId);

    WishlistCountResponse getWishlistCount(String email);
}
