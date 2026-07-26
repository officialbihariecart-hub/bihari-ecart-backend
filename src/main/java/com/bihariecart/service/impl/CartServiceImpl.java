package com.bihariecart.service.impl;

import com.bihariecart.dto.AddToCartRequest;
import com.bihariecart.dto.CartItemResponse;
import com.bihariecart.dto.CartResponse;
import com.bihariecart.dto.UpdateCartRequest;
import com.bihariecart.entity.Cart;
import com.bihariecart.entity.CartItem;
import com.bihariecart.entity.Product;
import com.bihariecart.entity.User;
import com.bihariecart.exception.ResourceNotFoundException;
import com.bihariecart.repository.CartItemRepository;
import com.bihariecart.repository.CartRepository;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.UserRepository;
import com.bihariecart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for Cart operations in Bihari E-Cart.
 * Enforces business rules, stock validation, security context checks, and
 * transactional integrity.
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartResponse addToCart(String userEmail, AddToCartRequest request) {
        User user = getUserByEmail(userEmail);
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (Boolean.FALSE.equals(product.getActive())) {
            throw new IllegalArgumentException("Cannot add inactive or deleted product to cart");
        }

        // Stock Validation 1: Prevent adding out of stock products
        if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
            throw new IllegalArgumentException("Product '" + product.getName() + "' is out of stock");
        }

        int requestedQuantity = request.getQuantity();

        // Rule 5: If product already exists, increment quantity instead of creating new
        // row
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndProduct(cart, product);

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + requestedQuantity;

            // Stock Validation 2: Ensure combined quantity does not exceed available stock
            if (newQuantity > product.getStockQuantity()) {
                throw new IllegalArgumentException(
                        "Cannot add " + requestedQuantity + " unit(s). Total requested quantity ("
                                + newQuantity + ") exceeds available stock (" + product.getStockQuantity()
                                + ") for product '" + product.getName() + "'");
            }

            existingItem.setQuantity(newQuantity);
            // Price Decision: Preserve original priceAtAddition when item was first added.
            // Do NOT overwrite with current product price to maintain historical snapshot.
        } else {
            // Stock Validation 3: Ensure initial requested quantity does not exceed
            // available stock
            if (requestedQuantity > product.getStockQuantity()) {
                throw new IllegalArgumentException("Requested quantity (" + requestedQuantity
                        + ") exceeds available stock (" + product.getStockQuantity() + ") for product '"
                        + product.getName() + "'");
            }

            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(requestedQuantity);
            newItem.setPriceAtAddition(product.getPrice());
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String userEmail) {
        User user = getUserByEmail(userEmail);
        Cart cart = cartRepository.findByUserEmailWithItems(userEmail)
                .orElseGet(() -> getOrCreateCart(user));

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(String userEmail, Long itemId, UpdateCartRequest request) {
        Cart cart = cartRepository.findByUserEmailWithItems(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userEmail));

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        // Enforce cart item ownership
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to the current user's cart");
        }

        // Rule 8: If quantity becomes zero or less after update, remove item
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            cart.removeItem(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            Product product = cartItem.getProduct();

            // Stock Validation 4: Ensure updated quantity does not exceed available stock
            if (product != null && product.getStockQuantity() != null
                    && request.getQuantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException("Requested quantity (" + request.getQuantity()
                        + ") exceeds available stock (" + product.getStockQuantity() + ") for product '"
                        + product.getName() + "'");
            }

            cartItem.setQuantity(request.getQuantity());
        }

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(String userEmail, Long itemId) {
        Cart cart = cartRepository.findByUserEmailWithItems(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userEmail));

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        // Enforce cart item ownership
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to the current user's cart");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(String userEmail) {
        Cart cart = cartRepository.findByUserEmailWithItems(userEmail)
                .orElse(null);

        if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
            cart.clearItems();
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToCartResponse(Cart cart) {
        // Rule 9: Cart cannot expose or count inactive/deleted products
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .filter(item -> item.getProduct() != null && Boolean.TRUE.equals(item.getProduct().getActive()))
                .map(this::mapToCartItemResponse)
                .toList();

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        double totalAmount = itemResponses.stream()
                .mapToDouble(CartItemResponse::getSubTotal)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .userEmail(cart.getUser() != null ? cart.getUser().getEmail() : null)
                .items(itemResponses)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        Product product = item.getProduct();
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(product.getImageUrl())
                .quantity(item.getQuantity())
                .priceAtAddition(item.getPriceAtAddition())
                .subTotal(item.getSubTotal())
                .build();
    }
}
