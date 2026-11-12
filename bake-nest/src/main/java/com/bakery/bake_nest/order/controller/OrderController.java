package com.bakery.bake_nest.order.controller;

import com.bakery.bake_nest.order.dto.*;
import com.bakery.bake_nest.order.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequests.Create request) {
        return orders.create(request);
    }

    @GetMapping
    public List<OrderResponse> all() {
        return orders.all();
    }

    @GetMapping("/mine")
    public List<OrderResponse> mine() {
        return orders.mine();
    }

    @GetMapping("/{id}")
    public OrderResponse find(@PathVariable Long id) {
        return orders.find(id);
    }

    @PutMapping("/{id}")
    public OrderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequests.Update request) {
        return orders.update(id, request);
    }

    @PatchMapping("/{id}/items/{itemId}")
    public OrderResponse quantity(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderRequests.Quantity request) {
        return orders.quantity(id, itemId, request.getQuantity());
    }

    @PatchMapping("/{id}/status")
    public OrderResponse status(
            @PathVariable Long id, @Valid @RequestBody OrderRequests.Status request) {
        return orders.status(id, request.getStatus());
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return orders.cancel(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        orders.delete(id);
    }
}
