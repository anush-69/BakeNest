package com.bakery.bake_nest.controller;

import com.bakery.bake_nest.model.Product;
import com.bakery.bake_nest.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService service;

    // Folder where images will be saved automatically
    private static final String UPLOAD_DIRECTORY = "src/main/resources/static/images/";

    // 1. Show list of all products (Read)
    @GetMapping
    public String viewProducts(Model model) {
        model.addAttribute("products", service.getAllProducts());
        return "product-list";
    }

    // 2. Add a new product (Create)
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        return "add-product";
    }

    // 3. Save the product
    @PostMapping("/save")
    public String saveProduct(@ModelAttribute Product product) {
        service.saveProduct(product);
        return "redirect:/products";
    }

    // 4. Delete a product
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        service.deleteProduct(id);
        return "redirect:/products";
    }
}