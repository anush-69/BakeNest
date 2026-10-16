package com.bakery.bake_nest.repository;

import com.bakery.bake_nest.model.BakeryOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<BakeryOrder, Long> {
}
