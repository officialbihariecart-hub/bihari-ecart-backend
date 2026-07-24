package com.bihariecart.repository;

import com.bihariecart.entity.Category;
import com.bihariecart.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByName(String name);

    boolean existsByName(String name);

    List<Product> findByCategory(Category category);

    List<Product> findByActive(Boolean active);

    List<Product> findByBrand(String brand);
}
