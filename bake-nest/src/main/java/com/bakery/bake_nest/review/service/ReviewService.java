package com.bakery.bake_nest.review.service;

import com.bakery.bake_nest.model.Review;
import com.bakery.bake_nest.product.service.ProductService;
import com.bakery.bake_nest.review.dto.*;
import com.bakery.bake_nest.review.repository.ReviewRepository;
import com.bakery.bake_nest.shared.security.CurrentAccount;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReviewService {
    private final ReviewRepository reviews;
    private final ProductService products;
    private final CurrentAccount current;

    public ReviewService(
            ReviewRepository reviews, ProductService products, CurrentAccount current) {
        this.reviews = reviews;
        this.products = products;
        this.current = current;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    public ReviewResponse create(ReviewRequests.Create request) {
        Review review = new Review();
        review.setCustomer(current.get());
        review.setProduct(products.find(request.getProductId()));
        copy(review, request);
        return new ReviewResponse(reviews.saveAndFlush(review));
    }

    public List<ReviewResponse> approved() {
        return reviews.findByStatus("APPROVED").stream().map(ReviewResponse::new).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<ReviewResponse> all() {
        return reviews.findAll().stream().map(ReviewResponse::new).toList();
    }

    public List<ReviewResponse> byProduct(Long productId) {
        products.find(productId);
        return reviews.findByProductIdAndStatus(productId, "APPROVED").stream()
                .map(ReviewResponse::new)
                .toList();
    }

    public List<ReviewResponse> byCustomer(Long customerId) {
        current.requireOwnerOrAdmin(customerId);
        return reviews.findByCustomerId(customerId).stream().map(ReviewResponse::new).toList();
    }

    public ReviewResponse find(Long id) {
        Review review = requireReview(id);
        if (!"APPROVED".equals(review.getStatus()))
            current.requireOwnerOrAdmin(review.getCustomer().getId());
        return new ReviewResponse(review);
    }

    public double average(Long productId) {
        products.find(productId);
        Double average = reviews.averageRating(productId);
        return average == null ? 0.0 : average;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    public ReviewResponse update(Long id, ReviewRequests.Edit request) {
        Review review = lockedReview(id);
        current.requireOwner(review.getCustomer().getId());
        copy(review, request);
        // Edited content must be approved again before becoming public.
        review.setStatus("PENDING");
        review.setModeratedBy(null);
        review.setModeratedAt(null);
        review.setModerationNote(null);
        return new ReviewResponse(review);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ReviewResponse moderate(Long id, ReviewRequests.Moderation request) {
        Review review = lockedReview(id);
        review.setStatus(request.getStatus());
        review.setModerationNote(request.getNote());
        review.setModeratedBy(current.get());
        review.setModeratedAt(LocalDateTime.now());
        return new ReviewResponse(review);
    }

    public void delete(Long id) {
        Review review = lockedReview(id);
        current.requireOwnerOrAdmin(review.getCustomer().getId());
        reviews.delete(review);
    }

    private Review requireReview(Long id) {
        return reviews.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found."));
    }

    private Review lockedReview(Long id) {
        return reviews.findForUpdate(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found."));
    }

    private void copy(Review review, ReviewRequests.Edit request) {
        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
    }
}
