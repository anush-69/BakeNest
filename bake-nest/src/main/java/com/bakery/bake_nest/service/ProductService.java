package com.bakery.bake_nest.service;

import com.bakery.bake_nest.model.Product;
import com.bakery.bake_nest.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repo;

    // Read: Get all products to show on the menu
    public List<Product> getAllProducts() {
        return repo.findAll();
    }

    // Orders show only products that are available.
    public List<Product> getAvailableProducts() {
        return repo.findByAvailableTrueOrderByNameAsc();
    }

    // Create / Update: Save product to MySQL database
    public void saveProduct(Product product) {
        repo.save(product);
    }

    // Read: Find one product by ID (useful for editing)
    public Product getProductById(Long id) {
        return repo.findById(id).orElse(null);
    }

    // Delete: Remove a product by ID
    public void deleteProduct(Long id) {
        repo.deleteById(id);
    }
}