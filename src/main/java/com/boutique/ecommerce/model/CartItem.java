package com.boutique.ecommerce.model;

/**
 * CartItem Model
 *
 * Represents an item currently held in a customer's shopping cart.
 */
public class CartItem {

    private Product product;
    private int quantity;

    public CartItem() {
    }

    public CartItem(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.quantity = quantity;
    }

    /**
     * Calculates the line total for this item: unit price * quantity.
     */
    public double getLineTotal() {
        if (product == null) {
            return 0.0;
        }
        return product.getPrice() * quantity;
    }

    @Override
    public String toString() {
        return String.format("%s x %d @ $%.2f = $%.2f",
                product != null ? product.getName() : "Unknown",
                quantity,
                product != null ? product.getPrice() : 0.0,
                getLineTotal());
    }
}
