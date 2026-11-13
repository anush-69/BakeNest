package com.bakery.bake_nest.review.dto;

import com.bakery.bake_nest.model.Review;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReviewResponse {
    private final Long id, customerId, productId;
    private final int rating;
    private final String comment, status;
    private final LocalDateTime createdAt;

    public ReviewResponse(Review review) {
        id = review.getId();
        customerId = review.getCustomer().getId();
        productId = review.getProduct() == null ? null : review.getProduct().getId();
        rating = review.getRating();
        comment = review.getComment();
        status = review.getStatus();
        createdAt = review.getCreatedAt();
    }
}
