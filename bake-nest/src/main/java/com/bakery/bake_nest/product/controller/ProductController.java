package com.bakery.bake_nest.product.controller;

import com.bakery.bake_nest.model.Product;
import com.bakery.bake_nest.product.dto.ProductRequest;
import com.bakery.bake_nest.product.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    public List<Product> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId) {
        return products.search(name, categoryId);
    }

    @GetMapping("/{id}")
    public Product find(@PathVariable Long id) {
        return products.find(id);
    }

    @GetMapping("/all")
    public List<Product> all() {
        return products.allForAdmin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) {
        return products.create(request);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return products.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        products.delete(id);
    }
}
