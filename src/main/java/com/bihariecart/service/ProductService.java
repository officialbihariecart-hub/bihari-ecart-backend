package com.bihariecart.service;

import com.bihariecart.dto.ProductRequest;
import com.bihariecart.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    void updateProductRating(Long productId);
}
