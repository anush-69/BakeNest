package com.bakery.bake_nest.repository;

import com.bakery.bake_nest.model.BakeryOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<BakeryOrder, Long> {
    List<BakeryOrder> findByIdInOrderByOrderDateDescIdDesc(List<Long> ids);
}
