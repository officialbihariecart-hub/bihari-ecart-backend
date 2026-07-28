package com.bihariecart.service.impl;

import com.bihariecart.dto.ProductRequest;
import com.bihariecart.dto.ProductResponse;
import com.bihariecart.entity.Category;
import com.bihariecart.entity.Product;
import com.bihariecart.exception.DuplicateResourceException;
import com.bihariecart.exception.ResourceNotFoundException;
import com.bihariecart.repository.CategoryRepository;
import com.bihariecart.repository.ProductRepository;
import com.bihariecart.repository.ReviewRepository;
import com.bihariecart.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, ReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        String trimmedName = request.getName().trim();
        if (productRepository.existsByName(trimmedName)) {
            throw new DuplicateResourceException("Product with name '" + trimmedName + "' already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Product product = new Product();
        product.setName(trimmedName);
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setBrand(request.getBrand());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        String trimmedName = request.getName().trim();
        productRepository.findByName(trimmedName).ifPresent(existingProduct -> {
            if (!existingProduct.getId().equals(id)) {
                throw new DuplicateResourceException("Product with name '" + trimmedName + "' already exists");
            }
        });

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        product.setName(trimmedName);
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setBrand(request.getBrand());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    @Override
    @Transactional
    public void updateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Double rawAverage = reviewRepository.averageRatingByProductId(productId);
        Long totalCount = reviewRepository.countByProductId(productId);

        double averageRating = 0.0;
        int reviewCount = 0;

        if (totalCount != null && totalCount > 0 && rawAverage != null) {
            averageRating = Math.round(rawAverage * 10.0) / 10.0;
            reviewCount = totalCount.intValue();
        }

        product.setAverageRating(averageRating);
        product.setReviewCount(reviewCount);

        productRepository.save(product);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .brand(product.getBrand())
                .active(product.getActive())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .averageRating(product.getAverageRating() != null ? product.getAverageRating() : 0.0)
                .reviewCount(product.getReviewCount() != null ? product.getReviewCount() : 0)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
