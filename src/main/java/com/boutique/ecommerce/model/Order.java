package com.boutique.ecommerce.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Entity Model
 *
 * Represents a customer's placed order and contains associated line items.
 */
public class Order {

    private int orderId;
    private int userId;
    private String customerUsername; // Optional convenience for admin order views
    private Timestamp orderDate;
    private double totalAmount;
    private String status;
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public Order(int orderId, int userId, Timestamp orderDate, double totalAmount, String status) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public Order(int userId, double totalAmount, String status) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }

    public Timestamp getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Timestamp orderDate) {
        this.orderDate = orderDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void addItem(OrderItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    @Override
    public String toString() {
        return String.format("Order #%d | Date: %s | Total: $%.2f | Status: %s | Items: %d",
                orderId,
                orderDate != null ? orderDate.toString() : "N/A",
                totalAmount,
                status,
                items != null ? items.size() : 0);
    }
}
