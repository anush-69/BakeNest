package com.bakery.bake_nest.order.service;

import com.bakery.bake_nest.model.*;
import com.bakery.bake_nest.order.dto.*;
import com.bakery.bake_nest.order.model.OrderStatus;
import com.bakery.bake_nest.order.repository.OrderRepository;
import com.bakery.bake_nest.product.service.ProductService;
import com.bakery.bake_nest.shared.security.CurrentAccount;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class OrderService {
    private final OrderRepository orders;
    private final ProductService products;
    private final CurrentAccount current;

    public OrderService(OrderRepository orders, ProductService products, CurrentAccount current) {
        this.orders = orders;
        this.products = products;
        this.current = current;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse create(OrderRequests.Create request) {
        BakeryOrder order = new BakeryOrder();
        order.setCustomer(current.get());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryCity(request.getDeliveryCity());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setDeliveryCost(request.getDeliveryCost());
        order.setCustomerNote(request.getCustomerNote());
        var productIds = new HashSet<Long>();
        for (var requested : request.getItems()) {
            if (!productIds.add(requested.getProductId()))
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Include each product only once.");
            Product product = products.find(requested.getProductId());
            checkAvailable(product);
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            // Copy the price now; do not trust a price supplied by the customer.
            item.setUnitPrice(product.getPrice());
            item.setQuantity(requested.getQuantity());
            order.addItem(item);
        }
        return new OrderResponse(orders.saveAndFlush(order));
    }

    public OrderResponse find(Long id) {
        BakeryOrder order = requireOrder(id);
        current.requireOwnerOrAdmin(order.getCustomer().getId());
        return new OrderResponse(order);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    public List<OrderResponse> mine() {
        return orders.findByCustomerIdOrderByOrderDateDesc(current.get().getId()).stream()
                .map(OrderResponse::new)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> all() {
        return orders.findAll().stream().map(OrderResponse::new).toList();
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse quantity(Long id, Long itemId, int quantity) {
        BakeryOrder order = lockedOrder(id);
        current.requireOwner(order.getCustomer().getId());
        if (!"PENDING".equals(order.getStatus()))
            throw conflict("Only pending orders can be edited.");
        OrderItem item =
                order.getItems().stream()
                        .filter(value -> value.getId().equals(itemId))
                        .findFirst()
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Order item not found."));
        checkAvailable(item.getProduct());
        item.setQuantity(quantity); // The stored purchase price stays unchanged.
        return new OrderResponse(order);
    }

    // Customer can update delivery details while the order is still PENDING
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse update(Long id, OrderRequests.Update request) {
        BakeryOrder order = lockedOrder(id);
        current.requireOwner(order.getCustomer().getId());
        if (!"PENDING".equals(order.getStatus()))
            throw conflict("Order details can only be changed before the order is confirmed.");
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryCity(request.getDeliveryCity());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setDeliveryCost(request.getDeliveryCost());
        order.setCustomerNote(request.getCustomerNote());
        return new OrderResponse(order);
    }

    // Customer can delete while PENDING or CONFIRMED; admin can delete any time
    public void delete(Long id) {
        BakeryOrder order = lockedOrder(id);
        User caller = current.get();
        current.requireOwnerOrAdmin(order.getCustomer().getId());
        boolean isAdmin = current.isAdmin(caller);
        if (!isAdmin) {
            // Customers may only delete before the bakery starts preparing
            if ("PREPARING".equals(order.getStatus())
                    || "READY".equals(order.getStatus())
                    || "COMPLETED".equals(order.getStatus())) {
                throw conflict("Orders can only be deleted before they start being prepared.");
            }
        }
        orders.delete(order);
    }

    public OrderResponse cancel(Long id) {
        BakeryOrder order = lockedOrder(id);
        current.requireOwnerOrAdmin(order.getCustomer().getId());
        if (!"PENDING".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus()))
            throw conflict("Only pending or confirmed orders can be cancelled.");
        order.setStatus("CANCELLED");
        return new OrderResponse(order);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse status(Long id, OrderStatus next) {
        if (next == OrderStatus.CANCELLED) return cancel(id);
        BakeryOrder order = lockedOrder(id);
        // Administrative processing moves forward one step at a time.
        String expected =
                switch (order.getStatus()) {
                    case "PENDING" -> "CONFIRMED";
                    case "CONFIRMED" -> "PREPARING";
                    case "PREPARING" -> "READY";
                    case "READY" -> "COMPLETED";
                    default -> "";
                };
        if (!next.name().equals(expected)) throw conflict("Invalid order status transition.");
        order.setStatus(next.name());
        return new OrderResponse(order);
    }

    private BakeryOrder requireOrder(Long id) {
        return orders.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
    }

    private BakeryOrder lockedOrder(Long id) {
        return orders.findForUpdate(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
    }

    private void checkAvailable(Product product) {
        if (!product.isAvailable()) throw conflict("Product is unavailable.");
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
