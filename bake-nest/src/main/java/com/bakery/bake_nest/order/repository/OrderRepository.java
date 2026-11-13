package com.bakery.bake_nest.order.repository;

import com.bakery.bake_nest.model.BakeryOrder;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<BakeryOrder, Long> {
    List<BakeryOrder> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    // Prevent two requests from changing the same order at the same time.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from BakeryOrder o where o.id = :id")
    Optional<BakeryOrder> findForUpdate(Long id);
}
