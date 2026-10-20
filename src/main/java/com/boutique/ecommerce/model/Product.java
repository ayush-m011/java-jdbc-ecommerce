package com.boutique.ecommerce.model;

import java.sql.Timestamp;

/**
 * Product Entity Model
 *
 * Represents an item in the boutique inventory (Fragrance or Bracelet).
 */
public class Product {

    private int productId;
    private String name;
    private String category;
    private double price;
    private int stockQuantity;
    private String description;
    private Timestamp createdAt;

    public Product() {
    }

    public Product(int productId, String name, String category, double price, int stockQuantity, String description, Timestamp createdAt) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Product(String name, String category, double price, int stockQuantity, String description) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.description = description;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - $%.2f | Stock: %d",
                productId, name, category, price, stockQuantity);
    }
}
