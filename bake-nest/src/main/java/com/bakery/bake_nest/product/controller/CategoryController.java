package com.bakery.bake_nest.product.controller;

import com.bakery.bake_nest.model.Category;
import com.bakery.bake_nest.product.dto.CategoryRequest;
import com.bakery.bake_nest.product.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final ProductService products;

    public CategoryController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    public List<Category> all() {
        return products.categories();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@Valid @RequestBody CategoryRequest request) {
        return products.createCategory(request);
    }
}
