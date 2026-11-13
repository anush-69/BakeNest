package com.bakery.bake_nest.product.repository;

import com.bakery.bake_nest.model.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query(
            "select p from Product p where p.available = true and (:name is null or lower(p.name)"
                + " like lower(concat('%', :name, '%'))) and (:categoryId is null or p.category.id"
                + " = :categoryId)")
    List<Product> search(String name, Long categoryId);
}
