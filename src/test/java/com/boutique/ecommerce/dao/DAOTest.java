package com.boutique.ecommerce.dao;

import com.boutique.ecommerce.config.DBConnection;
import com.boutique.ecommerce.model.CartItem;
import com.boutique.ecommerce.model.Order;
import com.boutique.ecommerce.model.Product;
import com.boutique.ecommerce.model.User;
import com.boutique.ecommerce.util.PasswordUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DAOTest {

    private static UserDAO userDAO;
    private static ProductDAO productDAO;
    private static OrderDAO orderDAO;

    @BeforeAll
    static void setUp() throws SQLException {
        // Point DBConnection to in-memory H2 database in MySQL compatibility mode
        DBConnection.setCustomConnection(
                "jdbc:h2:mem:boutique_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        userDAO = new UserDAO();
        productDAO = new ProductDAO();
        orderDAO = new OrderDAO();

        // Create schema tables
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS order_items;");
            stmt.execute("DROP TABLE IF EXISTS orders;");
            stmt.execute("DROP TABLE IF EXISTS products;");
            stmt.execute("DROP TABLE IF EXISTS users;");

            stmt.execute("""
                CREATE TABLE users (
                    user_id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password_hash VARCHAR(64) NOT NULL,
                    salt VARCHAR(64) NOT NULL,
                    full_name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            stmt.execute("""
                CREATE TABLE products (
                    product_id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(150) NOT NULL,
                    category VARCHAR(50) NOT NULL,
                    price DECIMAL(10, 2) NOT NULL,
                    stock_quantity INT NOT NULL,
                    description TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            stmt.execute("""
                CREATE TABLE orders (
                    order_id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    total_amount DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                );
            """);

            stmt.execute("""
                CREATE TABLE order_items (
                    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
                    order_id INT NOT NULL,
                    product_id INT NOT NULL,
                    quantity INT NOT NULL,
                    unit_price DECIMAL(10, 2) NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
                    FOREIGN KEY (product_id) REFERENCES products(product_id)
                );
            """);
        }
    }

    @AfterAll
    static void tearDown() {
        DBConnection.resetToDefault();
    }

    @Test
    @DisplayName("Should register, authenticate, and lookup users via UserDAO")
    void testUserDAOLifecycle() throws SQLException {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword("testpass123", salt);

        User newUser = new User("sarah_connor", hash, salt, "Sarah Connor", "sarah@cyberdyne.com", "CUSTOMER");
        User registered = userDAO.register(newUser);

        assertNotNull(registered);
        assertTrue(registered.getUserId() > 0);
        assertTrue(userDAO.usernameExists("sarah_connor"));

        // Authenticate with valid password
        User authenticated = userDAO.authenticate("sarah_connor", "testpass123");
        assertNotNull(authenticated);
        assertEquals("Sarah Connor", authenticated.getFullName());

        // Authenticate with invalid password
        User failed = userDAO.authenticate("sarah_connor", "wrongpass");
        assertNull(failed);

        // Duplicate registration should throw SQLException
        assertThrows(SQLException.class, () -> userDAO.register(newUser));
    }

    @Test
    @DisplayName("Should perform CRUD and search operations on ProductDAO")
    void testProductDAOLifecycle() throws SQLException {
        Product p = new Product("Oud Wood Extrait", "Fragrance", 150.00, 20, "Rich oud fragrance");
        Product added = productDAO.addProduct(p);

        assertNotNull(added);
        assertTrue(added.getProductId() > 0);

        Product fetched = productDAO.getProductById(added.getProductId());
        assertNotNull(fetched);
        assertEquals("Oud Wood Extrait", fetched.getName());
        assertEquals(150.00, fetched.getPrice());

        // Search
        List<Product> searchResults = productDAO.searchProducts("Wood");
        assertFalse(searchResults.isEmpty());

        // Update
        fetched.setPrice(135.00);
        fetched.setStockQuantity(18);
        assertTrue(productDAO.updateProduct(fetched));

        Product updated = productDAO.getProductById(added.getProductId());
        assertEquals(135.00, updated.getPrice());
        assertEquals(18, updated.getStockQuantity());
    }

    @Test
    @DisplayName("Should execute transactional order placement and reduce stock atomically")
    void testOrderPlacementTransaction() throws Exception {
        // 1. Create customer
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword("buyerpass", salt);
        User buyer = userDAO.register(new User("buyer_test", hash, salt, "Test Buyer", "buyer@test.com", "CUSTOMER"));

        // 2. Create product with initial stock = 10
        Product bracelet = productDAO.addProduct(new Product("Lapis Charm Bracelet", "Bracelet", 40.00, 10, "Celestial blue beads"));

        // 3. Place order for 4 units
        List<CartItem> cart = new ArrayList<>();
        cart.add(new CartItem(bracelet, 4));

        Order placedOrder = orderDAO.placeOrder(buyer.getUserId(), cart);
        assertNotNull(placedOrder);
        assertTrue(placedOrder.getOrderId() > 0);
        assertEquals(160.00, placedOrder.getTotalAmount(), 0.001);

        // 4. Verify stock was automatically reduced: 10 - 4 = 6
        Product reloaded = productDAO.getProductById(bracelet.getProductId());
        assertEquals(6, reloaded.getStockQuantity(), "Stock must automatically decrease to 6 after order");

        // 5. Verify order history
        List<Order> history = orderDAO.getOrdersByUserId(buyer.getUserId());
        assertFalse(history.isEmpty());
        assertEquals(1, history.get(0).getItems().size());
        assertEquals(4, history.get(0).getItems().get(0).getQuantity());

        // 6. Test Transaction Rollback when requesting more than available stock (try ordering 10 units when only 6 remain)
        List<CartItem> excessiveCart = new ArrayList<>();
        excessiveCart.add(new CartItem(reloaded, 10));

        assertThrows(SQLException.class, () -> orderDAO.placeOrder(buyer.getUserId(), excessiveCart),
                "Order should fail and roll back when stock is insufficient");

        // Verify stock was NOT reduced upon rollback (must still be 6)
        Product afterRollback = productDAO.getProductById(bracelet.getProductId());
        assertEquals(6, afterRollback.getStockQuantity(), "Stock must remain unchanged at 6 after transaction rollback");
    }
}
