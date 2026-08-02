package com.bihariecart.service.impl;

import com.bihariecart.dto.WishlistCheckResponse;
import com.bihariecart.dto.WishlistCountResponse;
import com.bihariecart.dto.WishlistItemRequest;
import com.bihariecart.dto.WishlistItemResponse;
import com.bihariecart.entity.Product;
import com.bihariecart.entity.User;
import com.bihariecart.entity.WishlistItem;
import com.bihariecart.exception.DuplicateResourceException;
import com.bihariecart.exception.ResourceNotFoundException;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.UserRepository;
import com.bihariecart.repository.WishlistItemRepository;
import com.bihariecart.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public WishlistItemResponse addProductToWishlist(String email, WishlistItemRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.getActive()) {
            throw new IllegalArgumentException("Cannot add an inactive product to wishlist");
        }

        if (wishlistItemRepository.existsByUserAndProduct(user, product)) {
            throw new DuplicateResourceException("Product is already in your wishlist");
        }

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .build();

        WishlistItem savedItem = wishlistItemRepository.save(item);
        return mapToResponse(savedItem);
    }

    @Override
    @Transactional
    public void removeProductFromWishlist(String email, Long productId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        wishlistItemRepository.deleteByUserAndProduct(user, product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getUserWishlist(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Note: findActiveWishlistItemsByUser filters out inactive products via JOIN
        return wishlistItemRepository.findActiveWishlistItemsByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistCheckResponse checkProductInWishlist(String email, Long productId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Optional<WishlistItem> itemOpt = wishlistItemRepository.findByUserAndProduct(user, product);

        if (itemOpt.isPresent()) {
            WishlistItem item = itemOpt.get();
            // If it exists but is inactive, we still return true since it's structurally wishlisted,
            // but normally we might want to return false if it's inactive. Let's strictly return true
            // if the relationship exists, to allow the user to delete it if desired.
            return WishlistCheckResponse.builder()
                    .wishlisted(true)
                    .wishlistItemId(item.getId())
                    .productId(product.getId())
                    .addedAt(item.getCreatedAt())
                    .build();
        }

        return WishlistCheckResponse.builder()
                .wishlisted(false)
                .wishlistItemId(null)
                .productId(productId)
                .addedAt(null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistCountResponse getWishlistCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Returns count of only ACTIVE products in the wishlist
        long count = wishlistItemRepository.countByUser(user);
        return WishlistCountResponse.builder().count(count).build();
    }

    private WishlistItemResponse mapToResponse(WishlistItem item) {
        return WishlistItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productPrice(item.getProduct().getPrice())
                .productImageUrl(item.getProduct().getImageUrl())
                .inStock(item.getProduct().getStockQuantity() > 0)
                .addedAt(item.getCreatedAt())
                .build();
    }
}
