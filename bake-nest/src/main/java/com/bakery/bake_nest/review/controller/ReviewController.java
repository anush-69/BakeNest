package com.bakery.bake_nest.review.controller;

import com.bakery.bake_nest.review.dto.*;
import com.bakery.bake_nest.review.service.ReviewService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@Valid @RequestBody ReviewRequests.Create request) {
        return reviews.create(request);
    }

    @GetMapping
    public List<ReviewResponse> approved() {
        return reviews.approved();
    }

    @GetMapping("/all")
    public List<ReviewResponse> all() {
        return reviews.all();
    }

    @GetMapping("/{id}")
    public ReviewResponse find(@PathVariable Long id) {
        return reviews.find(id);
    }

    @GetMapping("/product/{productId}")
    public List<ReviewResponse> byProduct(@PathVariable Long productId) {
        return reviews.byProduct(productId);
    }

    @GetMapping("/product/{productId}/average")
    public Map<String, Double> average(@PathVariable Long productId) {
        return Map.of("averageRating", reviews.average(productId));
    }

    @GetMapping("/customer/{customerId}")
    public List<ReviewResponse> byCustomer(@PathVariable Long customerId) {
        return reviews.byCustomer(customerId);
    }

    @PutMapping("/{id}")
    public ReviewResponse update(
            @PathVariable Long id, @Valid @RequestBody ReviewRequests.Edit request) {
        return reviews.update(id, request);
    }

    @PatchMapping("/{id}/moderation")
    public ReviewResponse moderate(
            @PathVariable Long id, @Valid @RequestBody ReviewRequests.Moderation request) {
        return reviews.moderate(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        reviews.delete(id);
    }
}
