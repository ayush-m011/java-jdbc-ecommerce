package com.boutique.ecommerce.service;

import com.boutique.ecommerce.dao.OrderDAO;
import com.boutique.ecommerce.dao.ProductDAO;
import com.boutique.ecommerce.model.Order;
import com.boutique.ecommerce.model.Product;

import java.sql.SQLException;
import java.util.List;

/**
 * AdminService - Business Logic for Store Administration
 *
 * Coordinates inventory management (CRUD on products) and order auditing.
 */
public class AdminService {

    private final ProductDAO productDAO;
    private final OrderDAO orderDAO;

    public AdminService() {
        this.productDAO = new ProductDAO();
        this.orderDAO = new OrderDAO();
    }

    public AdminService(ProductDAO productDAO, OrderDAO orderDAO) {
        this.productDAO = productDAO;
        this.orderDAO = orderDAO;
    }

    /**
     * Lists all products currently in the database.
     */
    public List<Product> getAllProducts() throws SQLException {
        return productDAO.getAllProducts();
    }

    /**
     * Retrieves a single product by ID.
     */
    public Product getProductById(int productId) throws SQLException {
        return productDAO.getProductById(productId);
    }

    /**
     * Adds a new product to the catalog.
     */
    public Product addProduct(String name, String category, double price, int stockQuantity, String description) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be empty.");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Product category cannot be empty.");
        }
        if (price < 0.0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }

        Product product = new Product(name.trim(), category.trim(), price, stockQuantity, description != null ? description.trim() : "");
        return productDAO.addProduct(product);
    }

    /**
     * Updates an existing product in the catalog.
     */
    public boolean updateProduct(Product product) throws SQLException {
        if (product == null || product.getProductId() <= 0) {
            throw new IllegalArgumentException("Invalid product supplied for update.");
        }
        return productDAO.updateProduct(product);
    }

    /**
     * Deletes a product by ID.
     */
    public boolean deleteProduct(int productId) throws SQLException {
        return productDAO.deleteProduct(productId);
    }

    /**
     * Retrieves all customer orders for auditing.
     */
    public List<Order> getAllOrders() throws SQLException {
        return orderDAO.getAllOrders();
    }
}
