package com.boutique.ecommerce.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

public class ModelTest {

    @Test
    @DisplayName("Should test Product model attributes and stock status")
    void testProductModel() {
        Product product = new Product(1, "Velvet Rose & Oud", "Fragrance", 120.00, 10, "Decadent scent", new Timestamp(System.currentTimeMillis()));

        assertEquals(1, product.getProductId());
        assertEquals("Velvet Rose & Oud", product.getName());
        assertEquals("Fragrance", product.getCategory());
        assertEquals(120.00, product.getPrice());
        assertEquals(10, product.getStockQuantity());
        assertTrue(product.isInStock());

        product.setStockQuantity(0);
        assertFalse(product.isInStock());
        assertTrue(product.toString().contains("Velvet Rose & Oud"));
    }

    @Test
    @DisplayName("Should test CartItem line total calculations")
    void testCartItemCalculations() {
        Product product = new Product(2, "Tiger Eye Bracelet", "Bracelet", 38.00, 20, "Gemstone bracelet", null);
        CartItem cartItem = new CartItem(product, 3);

        assertEquals(product, cartItem.getProduct());
        assertEquals(3, cartItem.getQuantity());
        assertEquals(114.00, cartItem.getLineTotal(), 0.001);

        cartItem.setQuantity(5);
        assertEquals(190.00, cartItem.getLineTotal(), 0.001);
    }

    @Test
    @DisplayName("Should test User model and role helper")
    void testUserModel() {
        User customer = new User("alice", "hash1", "salt1", "Alice Smith", "alice@example.com", "CUSTOMER");
        assertFalse(customer.isAdmin());

        User admin = new User("admin_user", "hash2", "salt2", "Admin User", "admin@store.com", "ADMIN");
        assertTrue(admin.isAdmin());
        assertTrue(admin.toString().contains("ADMIN"));
    }

    @Test
    @DisplayName("Should test Order and OrderItem structures")
    void testOrderAndOrderItem() {
        Order order = new Order(101, 1, new Timestamp(System.currentTimeMillis()), 200.00, "COMPLETED");
        OrderItem item1 = new OrderItem(1, 101, 10, "Rose Fragrance", 2, 50.00, 100.00);
        OrderItem item2 = new OrderItem(2, 101, 11, "Gold Cuff", 1, 100.00, 100.00);

        order.addItem(item1);
        order.addItem(item2);

        assertEquals(2, order.getItems().size());
        assertEquals(200.00, order.getTotalAmount());
        assertEquals("COMPLETED", order.getStatus());
        assertTrue(order.toString().contains("#101"));
    }
}
