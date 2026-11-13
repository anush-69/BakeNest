package com.bakery.bake_nest.review.repository;

import com.bakery.bake_nest.model.Review;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByStatus(String status);

    List<Review> findByProductIdAndStatus(Long productId, String status);

    List<Review> findByCustomerId(Long customerId);

    @Query(
            "select avg(r.rating) from Review r where r.product.id = :productId and r.status ="
                + " 'APPROVED'")
    Double averageRating(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Review r where r.id = :id")
    Optional<Review> findForUpdate(Long id);
}
