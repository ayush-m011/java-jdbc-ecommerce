package com.boutique.ecommerce.service;

import com.boutique.ecommerce.dao.OrderDAO;
import com.boutique.ecommerce.dao.ProductDAO;
import com.boutique.ecommerce.model.CartItem;
import com.boutique.ecommerce.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ShoppingServiceTest {

    private ShoppingService shoppingService;
    private Product samplePerfume;
    private Product sampleBracelet;

    @BeforeEach
    void setUp() {
        samplePerfume = new Product(1, "Velvet Rose & Oud", "Fragrance", 100.00, 10, "Rose perfume", null);
        sampleBracelet = new Product(2, "Gold Cuff", "Bracelet", 50.00, 5, "Gold cuff", null);

        // Dummy ProductDAO stub for in-memory service testing
        ProductDAO stubProductDAO = new ProductDAO() {
            @Override
            public Product getProductById(int productId) throws SQLException {
                if (productId == 1) return samplePerfume;
                if (productId == 2) return sampleBracelet;
                return null;
            }
        };

        shoppingService = new ShoppingService(stubProductDAO, new OrderDAO());
    }

    @Test
    @DisplayName("Should add items to cart and check stock limits")
    void testAddToCart() {
        String msg1 = shoppingService.addToCart(1, 2);
        assertTrue(msg1.contains("[OK]"), "Should successfully add 2 perfumes");
        assertEquals(1, shoppingService.getCartItems().size());

        // Add 3 more perfumes
        String msg2 = shoppingService.addToCart(1, 3);
        assertTrue(msg2.contains("[OK]"));
        assertEquals(5, shoppingService.getCartItems().get(0).getQuantity());

        // Try adding 6 more (total 11 > 10 stock)
        String msg3 = shoppingService.addToCart(1, 6);
        assertTrue(msg3.contains("[!] Insufficient stock"));
    }

    @Test
    @DisplayName("Should calculate subtotal and tax accurately")
    void testSubtotalAndBill() {
        shoppingService.addToCart(1, 2); // 2 * $100 = $200
        shoppingService.addToCart(2, 1); // 1 * $50 = $50

        assertEquals(250.00, shoppingService.calculateSubtotal(), 0.001);
        // 5% tax on $250 = $12.50 -> final bill $262.50
        assertEquals(262.50, shoppingService.calculateFinalBill(0.05), 0.001);
    }

    @Test
    @DisplayName("Should update and remove cart items")
    void testUpdateAndRemove() {
        shoppingService.addToCart(1, 4);
        shoppingService.updateCartQuantity(1, 2);

        List<CartItem> items = shoppingService.getCartItems();
        assertEquals(2, items.get(0).getQuantity());

        // Update to 0 removes the item
        shoppingService.updateCartQuantity(1, 0);
        assertTrue(shoppingService.isCartEmpty());
    }
}
