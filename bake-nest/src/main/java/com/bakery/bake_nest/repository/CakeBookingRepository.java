package com.bakery.bake_nest.repository;

import com.bakery.bake_nest.model.CakeBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CakeBookingRepository
        extends JpaRepository<CakeBooking, Long> {
    Page<CakeBooking> findByCustomer_IdOrderByIdDesc(Long customerId, Pageable pageable);
}