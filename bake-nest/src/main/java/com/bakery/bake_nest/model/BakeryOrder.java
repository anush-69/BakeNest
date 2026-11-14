package com.bakery.bake_nest.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bakery_orders")
public class BakeryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false) // Encapsul
    private User customer; // Hide Complexcity and hide data using private access modifiers

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    private String status = "PENDING";

    @Column(name = "customer_note", length = 1000)
    private String customerNote;

    // Delivery and contact details captured at order time
    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_city", length = 100)
    private String deliveryCity;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "delivery_cost", precision = 10, scale = 2)
    private BigDecimal deliveryCost = BigDecimal.ZERO;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    public BakeryOrder() {
    }

    public Long getId() {
        return id;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    public void setCustomerNote(String customerNote) {
        this.customerNote = customerNote;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public void setDeliveryCity(String deliveryCity) {
        this.deliveryCity = deliveryCity;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getDeliveryCost() {
        return deliveryCost;
    }

    public void setDeliveryCost(BigDecimal deliveryCost) {
        this.deliveryCost = deliveryCost;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    // Calculate from saved purchase prices, so later product price changes do not affect history.
    public BigDecimal getTotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subtotal.add(deliveryCost != null ? deliveryCost : BigDecimal.ZERO);
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}
