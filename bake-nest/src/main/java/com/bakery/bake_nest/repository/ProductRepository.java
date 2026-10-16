package com.bakery.bake_nest.repository;

import com.bakery.bake_nest.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.List<Product> findByAvailableTrueOrderByNameAsc();

    // Spring Boot handles database queries automatically for you!
}