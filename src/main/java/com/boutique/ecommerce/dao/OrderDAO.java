package com.boutique.ecommerce.dao;

import com.boutique.ecommerce.config.DBConnection;
import com.boutique.ecommerce.model.CartItem;
import com.boutique.ecommerce.model.Order;
import com.boutique.ecommerce.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderDAO - Data Access Object for Orders
 *
 * Implements order placement and history retrieval using PreparedStatement
 * and ACID-compliant JDBC Transactions.
 *
 * Beginner JDBC Concepts Explained:
 * - conn.setAutoCommit(false): Disables auto-commit mode. This marks the beginning of a
 *   database transaction. All subsequent INSERT/UPDATE statements are staged in memory/logs.
 * - conn.commit(): Atomically commits all changes in the current transaction to the database disk.
 * - conn.rollback(): If an error or insufficient stock occurs, cancels all operations since
 *   the start of the transaction, leaving the database in a completely clean, consistent state.
 * - Atomic stock reduction: `UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?`
 *   guarantees no overselling even under concurrent requests without needing complex table locks.
 */
public class OrderDAO {

    /**
     * Places a customer order inside an atomic JDBC transaction.
     * Inserts the order record, inserts each line item, and decrements stock.
     * If any step fails (e.g., insufficient stock), the entire transaction is rolled back.
     *
     * @param userId The ID of the ordering customer.
     * @param cartItems The list of items currently in the shopping cart.
     * @return The fully saved and confirmed Order object.
     * @throws SQLException if a database error occurs or stock is insufficient.
     */
    public Order placeOrder(int userId, List<CartItem> cartItems) throws SQLException {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot place order with an empty cart.");
        }

        double totalAmount = 0.0;
        for (CartItem item : cartItems) {
            totalAmount += item.getLineTotal();
        }

        String insertOrderSql = "INSERT INTO orders (user_id, total_amount, status) VALUES (?, ?, ?)";
        String insertOrderItemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";
        String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            // 1. Begin JDBC Transaction
            conn.setAutoCommit(false);

            int orderId;

            // 2. Insert into orders table and capture generated order_id
            try (PreparedStatement pstmtOrder = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtOrder.setInt(1, userId);
                pstmtOrder.setDouble(2, totalAmount);
                pstmtOrder.setString(3, "COMPLETED");

                pstmtOrder.executeUpdate();

                try (ResultSet generatedKeys = pstmtOrder.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating order failed, no generated ID obtained.");
                    }
                }
            }

            // 3. Insert order items & reduce stock atomically
            try (PreparedStatement pstmtItem = conn.prepareStatement(insertOrderItemSql);
                 PreparedStatement pstmtStock = conn.prepareStatement(updateStockSql)) {

                for (CartItem item : cartItems) {
                    int productId = item.getProduct().getProductId();
                    int qty = item.getQuantity();

                    // Step 3a: Atomically decrement stock
                    pstmtStock.setInt(1, qty);
                    pstmtStock.setInt(2, productId);
                    pstmtStock.setInt(3, qty);

                    int rowsUpdated = pstmtStock.executeUpdate();
                    if (rowsUpdated == 0) {
                        // Stock was insufficient at time of order!
                        throw new SQLException("Order failed: Insufficient stock for product '" +
                                item.getProduct().getName() + "'. Available stock changed.");
                    }

                    // Step 3b: Insert line item
                    pstmtItem.setInt(1, orderId);
                    pstmtItem.setInt(2, productId);
                    pstmtItem.setInt(3, qty);
                    pstmtItem.setDouble(4, item.getProduct().getPrice());
                    pstmtItem.setDouble(5, item.getLineTotal());

                    pstmtItem.executeUpdate();
                }
            }

            // 4. Commit the transaction - all changes become permanent!
            conn.commit();

            // Build confirmed Order result
            Order order = new Order(orderId, userId, new Timestamp(System.currentTimeMillis()), totalAmount, "COMPLETED");
            for (CartItem item : cartItems) {
                order.addItem(new OrderItem(
                        item.getProduct().getProductId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getProduct().getPrice()
                ));
            }
            return order;

        } catch (SQLException e) {
            // If any error occurs, rollback transaction to protect database integrity
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[ERROR] Failed to rollback transaction: " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset connection autocommit state
                    conn.close();
                } catch (SQLException closeEx) {
                    System.err.println("[ERROR] Failed to close connection: " + closeEx.getMessage());
                }
            }
        }
    }

    /**
     * Retrieves all orders placed by a specific user.
     *
     * @param userId The customer ID.
     * @return List of past orders with line items populated.
     * @throws SQLException if a database error occurs.
     */
    public List<Order> getOrdersByUserId(int userId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT order_id, user_id, order_date, total_amount, status FROM orders " +
                     "WHERE user_id = ? ORDER BY order_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = new Order(
                            rs.getInt("order_id"),
                            rs.getInt("user_id"),
                            rs.getTimestamp("order_date"),
                            rs.getDouble("total_amount"),
                            rs.getString("status")
                    );
                    // Fetch line items for this order
                    order.setItems(getOrderItems(conn, order.getOrderId()));
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Error retrieving orders for user [" + userId + "]: " + e.getMessage(), e);
        }
        return orders;
    }

    /**
     * Retrieves all orders across all customers (for admin view).
     *
     * @return List of all customer orders.
     * @throws SQLException if a database error occurs.
     */
    public List<Order> getAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.order_id, o.user_id, u.username, o.order_date, o.total_amount, o.status " +
                     "FROM orders o " +
                     "JOIN users u ON o.user_id = u.user_id " +
                     "ORDER BY o.order_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Order order = new Order(
                        rs.getInt("order_id"),
                        rs.getInt("user_id"),
                        rs.getTimestamp("order_date"),
                        rs.getDouble("total_amount"),
                        rs.getString("status")
                );
                order.setCustomerUsername(rs.getString("username"));
                order.setItems(getOrderItems(conn, order.getOrderId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            throw new SQLException("Error retrieving all orders: " + e.getMessage(), e);
        }
        return orders;
    }

    /**
     * Helper method to fetch line items for a specific order.
     */
    private List<OrderItem> getOrderItems(Connection conn, int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT oi.order_item_id, oi.order_id, oi.product_id, p.name AS product_name, " +
                     "oi.quantity, oi.unit_price, oi.subtotal " +
                     "FROM order_items oi " +
                     "JOIN products p ON oi.product_id = p.product_id " +
                     "WHERE oi.order_id = ? " +
                     "ORDER BY oi.order_item_id";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(
                            rs.getInt("order_item_id"),
                            rs.getInt("order_id"),
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price"),
                            rs.getDouble("subtotal")
                    ));
                }
            }
        }
        return items;
    }
}
