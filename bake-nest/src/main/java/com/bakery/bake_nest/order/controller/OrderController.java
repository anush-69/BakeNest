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

    // Create Order
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequests.Create request) {
        return orders.create(request);
    }

    // Read all Order list by admin
    @GetMapping
    public List<OrderResponse> all() {
        return orders.all();
    }

    // Read (my)orders by the customer
    @GetMapping("/mine")
    public List<OrderResponse> mine() {
        return orders.mine();
    }

    // Read order using ID
    @GetMapping("/{id}")
    public OrderResponse find(@PathVariable Long id) {
        return orders.find(id);
    }

    // Update delivary details
    @PutMapping("/{id}")
    public OrderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequests.Update request) {
        return orders.update(id, request);
    }

    // Update item quantity
    @PatchMapping("/{id}/items/{itemId}")
    public OrderResponse quantity(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderRequests.Quantity request) {
        return orders.quantity(id, itemId, request.getQuantity());
    }

    // Update order status
    @PatchMapping("/{id}/status")
    public OrderResponse status(
            @PathVariable Long id, @Valid @RequestBody OrderRequests.Status request) {
        return orders.status(id, request.getStatus());
    }

    // Cancel order
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return orders.cancel(id);
    }

    // Delete order using ID
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        orders.delete(id);
    }
}
