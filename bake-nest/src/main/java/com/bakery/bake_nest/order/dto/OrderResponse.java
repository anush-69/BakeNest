package com.bakery.bake_nest.order.dto;

import com.bakery.bake_nest.model.BakeryOrder;
import com.bakery.bake_nest.model.OrderItem;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderResponse {
    private final Long id, customerId;
    private final LocalDateTime orderDate;
    private final String status, customerNote;
    private final String customerName, customerPhone;
    private final String deliveryAddress, deliveryCity;
    private final String paymentMethod;
    private final BigDecimal deliveryCost, total;
    private final List<Item> items;

    public OrderResponse(BakeryOrder order) {
        id = order.getId();
        customerId = order.getCustomer().getId();
        orderDate = order.getOrderDate();
        status = order.getStatus();
        customerNote = order.getCustomerNote();
        customerName = order.getCustomerName();
        customerPhone = order.getCustomerPhone();
        deliveryAddress = order.getDeliveryAddress();
        deliveryCity = order.getDeliveryCity();
        paymentMethod = order.getPaymentMethod();
        deliveryCost = order.getDeliveryCost();
        total = order.getTotal();
        items = order.getItems().stream().map(Item::new).toList();
    }

    @Getter
    public static class Item {
        private final Long id, productId;
        private final String productName;
        private final int quantity;
        private final BigDecimal unitPrice, subtotal;

        public Item(OrderItem item) {
            id = item.getId();
            productId = item.getProduct().getId();
            productName = item.getProductName();
            quantity = item.getQuantity();
            unitPrice = item.getUnitPrice();
            subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
