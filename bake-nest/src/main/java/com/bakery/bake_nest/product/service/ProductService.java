package com.bakery.bake_nest.product.service;

import com.bakery.bake_nest.model.Category;
import com.bakery.bake_nest.model.Product;
import com.bakery.bake_nest.product.dto.*;
import com.bakery.bake_nest.product.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ProductService {
    private final ProductManagementRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductManagementRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<Product> search(String name, Long categoryId) {
        return products.search(name, categoryId);
    }

    public Product find(Long id) {
        return products.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Product> allForAdmin() {
        return products.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Product create(ProductRequest request) {
        Product product = new Product();
        copy(product, request);
        return products.save(product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Product update(Long id, ProductRequest request) {
        Product product = find(id);
        copy(product, request);
        return product;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        // Keep the product row so earlier order items and reviews remain valid.
        find(id).setAvailable(false);
    }

    public List<Category> categories() {
        return categories.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Category createCategory(CategoryRequest request) {
        if (categories.existsByNameIgnoreCase(request.getName().trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists.");
        Category category = new Category();
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        return categories.save(category);
    }

    private void copy(Product product, ProductRequest request) {
        Category category =
                categories
                        .findById(request.getCategoryId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Category not found."));
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(category);
        product.setAvailable(request.getAvailable());
        product.setImageUrl(request.getImageUrl());
    }
}
