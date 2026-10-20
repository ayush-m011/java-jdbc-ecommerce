package com.boutique.ecommerce.service;

import com.boutique.ecommerce.dao.OrderDAO;
import com.boutique.ecommerce.dao.ProductDAO;
import com.boutique.ecommerce.model.CartItem;
import com.boutique.ecommerce.model.Order;
import com.boutique.ecommerce.model.Product;
import com.boutique.ecommerce.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ShoppingService - Business Logic for Customer Shopping & Cart Management
 *
 * Manages the in-memory shopping cart, performs inventory validation,
 * computes subtotals, and orchestrates order placement through OrderDAO.
 */
public class ShoppingService {

    private final ProductDAO productDAO;
    private final OrderDAO orderDAO;

    // In-memory cart mapping Product ID -> CartItem
    private final Map<Integer, CartItem> cart = new LinkedHashMap<>();

    public ShoppingService() {
        this.productDAO = new ProductDAO();
        this.orderDAO = new OrderDAO();
    }

    public ShoppingService(ProductDAO productDAO, OrderDAO orderDAO) {
        this.productDAO = productDAO;
        this.orderDAO = orderDAO;
    }

    /**
     * Adds a product to the customer's cart after checking stock availability.
     *
     * @param productId The ID of the product.
     * @param quantity The quantity to add.
     * @return Confirmation message or error message.
     */
    public String addToCart(int productId, int quantity) {
        if (quantity <= 0) {
            return "[!] Quantity must be at least 1.";
        }

        try {
            Product product = productDAO.getProductById(productId);
            if (product == null) {
                return "[!] Product with ID " + productId + " does not exist.";
            }

            int currentCartQty = cart.containsKey(productId) ? cart.get(productId).getQuantity() : 0;
            int totalDesiredQty = currentCartQty + quantity;

            if (totalDesiredQty > product.getStockQuantity()) {
                return String.format("[!] Insufficient stock. Available: %d, Already in cart: %d.",
                        product.getStockQuantity(), currentCartQty);
            }

            if (cart.containsKey(productId)) {
                cart.get(productId).setQuantity(totalDesiredQty);
            } else {
                cart.put(productId, new CartItem(product, quantity));
            }

            return String.format("[OK] Added %d x '%s' to your cart.", quantity, product.getName());
        } catch (SQLException e) {
            return "[!] Database error while checking product: " + e.getMessage();
        }
    }

    /**
     * Updates the quantity of a product in the cart.
     * If newQuantity is 0, the item is removed from the cart.
     *
     * @param productId The ID of the product.
     * @param newQuantity The new desired quantity.
     * @return Result message.
     */
    public String updateCartQuantity(int productId, int newQuantity) {
        if (!cart.containsKey(productId)) {
            return "[!] Product ID " + productId + " is not in your cart.";
        }

        if (newQuantity == 0) {
            return removeFromCart(productId);
        }

        if (newQuantity < 0) {
            return "[!] Quantity cannot be negative.";
        }

        try {
            Product product = productDAO.getProductById(productId);
            if (product == null) {
                cart.remove(productId);
                return "[!] Product is no longer available. Removed from cart.";
            }

            if (newQuantity > product.getStockQuantity()) {
                return String.format("[!] Cannot update. Only %d units in stock.", product.getStockQuantity());
            }

            cart.get(productId).setQuantity(newQuantity);
            return String.format("[OK] Updated quantity of '%s' to %d.", product.getName(), newQuantity);
        } catch (SQLException e) {
            return "[!] Database error: " + e.getMessage();
        }
    }

    /**
     * Removes an item completely from the cart.
     */
    public String removeFromCart(int productId) {
        CartItem removed = cart.remove(productId);
        if (removed != null) {
            return String.format("[OK] Removed '%s' from your cart.", removed.getProduct().getName());
        }
        return "[!] Item was not found in your cart.";
    }

    /**
     * Clears all items from the current cart.
     */
    public void clearCart() {
        cart.clear();
    }

    /**
     * Returns an unmodifiable copy of cart items.
     */
    public List<CartItem> getCartItems() {
        return new ArrayList<>(cart.values());
    }

    /**
     * Returns whether the cart is empty.
     */
    public boolean isCartEmpty() {
        return cart.isEmpty();
    }

    /**
     * Computes the subtotal of all items in the cart.
     */
    public double calculateSubtotal() {
        double subtotal = 0.0;
        for (CartItem item : cart.values()) {
            subtotal += item.getLineTotal();
        }
        return subtotal;
    }

    /**
     * Computes the final bill with optional sales tax.
     *
     * @param taxRate Decimal tax rate (e.g. 0.05 for 5%, or 0.0 for none).
     * @return Final bill total.
     */
    public double calculateFinalBill(double taxRate) {
        double subtotal = calculateSubtotal();
        double tax = subtotal * taxRate;
        return subtotal + tax;
    }

    /**
     * Executes the checkout process using OrderDAO inside a database transaction.
     *
     * @param user The logged-in customer.
     * @return The confirmed Order.
     * @throws Exception if validation or transaction fails.
     */
    public Order checkout(User user) throws Exception {
        if (user == null) {
            throw new IllegalStateException("You must be logged in to checkout.");
        }
        if (isCartEmpty()) {
            throw new IllegalStateException("Your cart is empty. Add products before checking out.");
        }

        List<CartItem> itemsToOrder = getCartItems();
        Order order = orderDAO.placeOrder(user.getUserId(), itemsToOrder);

        // Clear cart once order is safely committed to the database
        clearCart();
        return order;
    }

    /**
     * Retrieves the past order history for a specific customer.
     */
    public List<Order> getOrderHistory(int userId) throws SQLException {
        return orderDAO.getOrdersByUserId(userId);
    }
}
