package com.bakery.bake_nest.service;

import com.bakery.bake_nest.form.OrderForm;
import com.bakery.bake_nest.model.BakeryOrder;
import com.bakery.bake_nest.model.OrderItem;
import com.bakery.bake_nest.model.Product;
import com.bakery.bake_nest.repository.OrderRepository;
import com.bakery.bake_nest.repository.ProductRepository;
import java.util.HashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public BakeryOrder createOrder(OrderForm form) {
        checkText(form.getCustomerName(), "Customer name", 100);
        checkText(form.getDeliveryAddress(), "Delivery address", 500);
        checkText(form.getPhone(), "Phone number", 20);
        String phone = form.getPhone().trim();
        // Accept spaces, parentheses, hyphens and an optional international prefix.
        String digits = phone.replaceAll("[ ()-]", "");
        if (!digits.matches("[+]?[0-9]{9,15}")) {
            throw new IllegalArgumentException("Enter a valid phone number with 9 to 15 digits.");
        }
        if (form.getCustomerNote() != null && form.getCustomerNote().length() > 1000) {
            throw new IllegalArgumentException("The note must be 1000 characters or fewer.");
        }
        if (!"CASH_ON_DELIVERY".equals(form.getPaymentMethod())
                && !"CARD_DEMO".equals(form.getPaymentMethod())) {
            throw new IllegalArgumentException("Choose a valid payment method.");
        }
        if (form.getItems() == null || form.getItems().isEmpty()) {
            throw new IllegalArgumentException("Select at least one product.");
        }

        BakeryOrder order = new BakeryOrder();
        order.setCustomerName(form.getCustomerName().trim());
        order.setPhone(phone);
        order.setDeliveryAddress(form.getDeliveryAddress().trim());
        order.setCustomerNote(form.getCustomerNote() == null ? null : form.getCustomerNote().trim());
        order.setPaymentMethod(form.getPaymentMethod());
        order.setPaymentStatus("CARD_DEMO".equals(form.getPaymentMethod()) ? "NOT_PROCESSED" : "PENDING");
        order.setStatus("PENDING");

        Set<Long> selectedIds = new HashSet<>();
        for (OrderForm.Item input : form.getItems()) {
            if (input == null || input.getQuantity() == null || input.getQuantity() < 0
                    || input.getQuantity() > 99) {
                throw new IllegalArgumentException("Quantities must be whole numbers from 0 to 99.");
            }
            // Zero means the customer did not choose this product.
            if (input.getQuantity() == 0) continue;
            if (input.getProductId() == null || !selectedIds.add(input.getProductId())) {
                throw new IllegalArgumentException("Select each product only once.");
            }
            Product product = productRepository.findById(input.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("A selected product no longer exists."));
            if (!product.isAvailable()) {
                throw new IllegalArgumentException(product.getName() + " is unavailable.");
            }
            if (product.getPrice() == null || product.getPrice().signum() < 0) {
                throw new IllegalArgumentException("A selected product has an invalid price.");
            }
            OrderItem item = new OrderItem();
            item.setProduct(product);
            // Save the current name and price so future product edits do not change this order.
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(input.getQuantity());
            order.addItem(item);
        }
        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Choose at least one product with a quantity above zero.");
        }
        // Cascade saves the items with the order, in the same transaction.
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<BakeryOrder> getOrders(List<Long> orderIds) {
        return orderRepository.findByIdInOrderByOrderDateDescIdDesc(orderIds);
    }

    @Transactional(readOnly = true)
    public BakeryOrder getOrder(Long id, List<Long> orderIds) {
        // An order number alone does not give someone access to delivery details.
        if (!orderIds.contains(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private void checkText(String value, String label, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(label + " is required and must be at most " + maxLength + " characters.");
        }
    }
}
