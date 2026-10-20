package com.boutique.ecommerce;

import com.boutique.ecommerce.config.DBConnection;
import com.boutique.ecommerce.dao.ProductDAO;
import com.boutique.ecommerce.dao.UserDAO;
import com.boutique.ecommerce.model.CartItem;
import com.boutique.ecommerce.model.Order;
import com.boutique.ecommerce.model.OrderItem;
import com.boutique.ecommerce.model.Product;
import com.boutique.ecommerce.model.User;
import com.boutique.ecommerce.service.AdminService;
import com.boutique.ecommerce.service.ShoppingService;
import com.boutique.ecommerce.util.InputValidator;
import com.boutique.ecommerce.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Main - Console Application Entry Point
 *
 * Modern Fragrance & Bracelet Boutique E-Commerce System.
 * Provides interactive menus for Customers, Admins, and Guests.
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final UserDAO userDAO = new UserDAO();
    private static final ProductDAO productDAO = new ProductDAO();
    private static final AdminService adminService = new AdminService();
    private static ShoppingService shoppingService = new ShoppingService();

    // Standard sales tax rate (e.g., 5% sales tax)
    private static final double TAX_RATE = 0.05;

    public static void main(String[] args) {
        printBanner();
        checkDatabaseStatus();

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = InputValidator.readInt(scanner, "Please select an option (1-6): ", 1, 6);
            System.out.println();

            switch (choice) {
                case 1 -> customerLogin();
                case 2 -> customerRegistration();
                case 3 -> adminLogin();
                case 4 -> browseCatalogAsGuest();
                case 5 -> databaseSettingsMenu();
                case 6 -> {
                    System.out.println("=============================================================");
                    System.out.println("   Thank you for visiting Modern Fragrance & Bracelet Boutique! ");
                    System.out.println("                        Goodbye!                             ");
                    System.out.println("=============================================================");
                    running = false;
                }
            }
        }
        scanner.close();
    }

    // =========================================================================
    // BANNER & STATUS
    // =========================================================================

    private static void printBanner() {
        System.out.println("================================================================================");
        System.out.println("       M O D E R N   F R A G R A N C E   &   B R A C E L E T   B O U T I Q U E  ");
        System.out.println("             Pure Core Java + JDBC + MySQL E-Commerce System            ");
        System.out.println("================================================================================");
    }

    private static void checkDatabaseStatus() {
        System.out.print("[INFO] Checking MySQL database connectivity... ");
        if (DBConnection.testConnection()) {
            System.out.println("CONNECTED! (Using: " + DBConnection.getActiveUrl() + ")");
        } else {
            System.out.println("FAILED.");
            System.out.println("  [!] Could not connect to MySQL at: " + DBConnection.getActiveUrl());
            System.out.println("  [!] Please ensure:");
            System.out.println("      1. Your MySQL server is running.");
            System.out.println("      2. You have executed 'schema.sql' to create 'ecommerce_db'.");
            System.out.println("      3. Your credentials in 'src/main/resources/db.properties' (or DB_PASSWORD env) are correct.");
            System.out.println("  [!] You can update database credentials from Main Menu Option 5.");
        }
        System.out.println();
    }

    // =========================================================================
    // MAIN MENU
    // =========================================================================

    private static void printMainMenu() {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("                                  MAIN MENU                                     ");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("  1. Customer Login");
        System.out.println("  2. Customer Registration");
        System.out.println("  3. Admin Login");
        System.out.println("  4. Browse Products as Guest");
        System.out.println("  5. Database Connection Settings & Diagnostics");
        System.out.println("  6. Exit");
        System.out.println("--------------------------------------------------------------------------------");
    }

    // =========================================================================
    // AUTHENTICATION
    // =========================================================================

    private static void customerLogin() {
        System.out.println(">>> CUSTOMER LOGIN <<<");
        String username = InputValidator.readNonEmptyString(scanner, "Enter Username: ");
        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        try {
            User user = userDAO.authenticate(username, password);
            if (user == null) {
                System.out.println("\n  [!] Invalid username or password. Please try again.\n");
                return;
            }

            if (user.isAdmin()) {
                System.out.println("\n  [*] Notice: This is an Administrator account. Redirecting to Admin Portal...\n");
                runAdminMenu(user);
                return;
            }

            System.out.printf("%n  [+] Welcome back, %s!%n%n", user.getFullName());
            // Create a fresh shopping service for this customer's session
            shoppingService = new ShoppingService();
            runCustomerMenu(user);

        } catch (SQLException e) {
            System.out.println("\n  [!] Database error during login: " + e.getMessage() + "\n");
        }
    }

    private static void customerRegistration() {
        System.out.println(">>> CUSTOMER REGISTRATION <<<");
        String username = InputValidator.readNonEmptyString(scanner, "Choose Username: ");

        try {
            if (userDAO.usernameExists(username)) {
                System.out.println("  [!] That username is already taken. Please choose another.\n");
                return;
            }

            String fullName = InputValidator.readNonEmptyString(scanner, "Enter Full Name: ");
            String email = InputValidator.readEmail(scanner, "Enter Email Address: ");
            String password = InputValidator.readNonEmptyString(scanner, "Choose Password (min 6 characters): ");

            if (password.length() < 6) {
                System.out.println("  [!] Password must be at least 6 characters long.\n");
                return;
            }

            String salt = PasswordUtil.generateSalt();
            String passwordHash = PasswordUtil.hashPassword(password, salt);

            User newUser = new User(username, passwordHash, salt, fullName, email, "CUSTOMER");
            User registered = userDAO.register(newUser);

            if (registered != null) {
                System.out.println("\n  [+] Registration successful! You can now log in.\n");
            } else {
                System.out.println("\n  [!] Registration could not be completed. Please try again.\n");
            }
        } catch (SQLException e) {
            System.out.println("\n  [!] Registration error: " + e.getMessage() + "\n");
        }
    }

    private static void adminLogin() {
        System.out.println(">>> ADMIN LOGIN <<<");
        System.out.println("Demo credentials -> Username: admin | Password: admin123");
        String username = InputValidator.readNonEmptyString(scanner, "Enter Admin Username: ");
        System.out.print("Enter Admin Password: ");
        String password = scanner.nextLine().trim();

        try {
            User user = userDAO.authenticate(username, password);
            if (user == null) {
                System.out.println("\n  [!] Invalid admin credentials.\n");
                return;
            }

            if (!user.isAdmin()) {
                System.out.println("\n  [!] Access Denied: User '" + username + "' is not an administrator.\n");
                return;
            }

            System.out.printf("%n  [+] Welcome Administrator, %s!%n%n", user.getFullName());
            runAdminMenu(user);

        } catch (SQLException e) {
            System.out.println("\n  [!] Database error during admin login: " + e.getMessage() + "\n");
        }
    }

    // =========================================================================
    // CUSTOMER MENU & ACTIONS
    // =========================================================================

    private static void runCustomerMenu(User customer) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("  CUSTOMER PORTAL | Logged in as: %s (Cart: %d item(s))%n",
                    customer.getUsername(), shoppingService.getCartItems().size());
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("  1. View All Products");
            System.out.println("  2. Search Products (by Name or Category)");
            System.out.println("  3. Add Product to Shopping Cart");
            System.out.println("  4. View Cart & Manage Quantities (Update / Remove)");
            System.out.println("  5. Checkout & Place Order");
            System.out.println("  6. View My Order History");
            System.out.println("  7. Logout");
            System.out.println("--------------------------------------------------------------------------------");

            int choice = InputValidator.readInt(scanner, "Select an option (1-7): ", 1, 7);
            System.out.println();

            switch (choice) {
                case 1 -> displayProductsTable(null);
                case 2 -> searchProducts();
                case 3 -> addProductToCartMenu();
                case 4 -> viewAndManageCart();
                case 5 -> checkoutOrder(customer);
                case 6 -> viewCustomerOrderHistory(customer);
                case 7 -> {
                    System.out.println("  [+] Logged out successfully.\n");
                    loggedIn = false;
                }
            }
        }
    }

    private static void browseCatalogAsGuest() {
        System.out.println(">>> GUEST PRODUCT CATALOG <<<");
        displayProductsTable(null);
        System.out.println("  [*] Tip: Register or Log In to add items to your cart and place orders!\n");
    }

    private static void displayProductsTable(List<Product> specificList) {
        try {
            List<Product> products = (specificList != null) ? specificList : productDAO.getAllProducts();
            if (products.isEmpty()) {
                System.out.println("  [i] No products found in the catalog.\n");
                return;
            }

            System.out.println("=========================================================================================================");
            System.out.printf("| %-4s | %-38s | %-12s | %-9s | %-6s | %-20s |%n",
                    "ID", "Product Name", "Category", "Price", "Stock", "Status");
            System.out.println("=========================================================================================================");

            for (Product p : products) {
                String stockStatus = p.getStockQuantity() > 0 ? "In Stock (" + p.getStockQuantity() + ")" : "OUT OF STOCK";
                System.out.printf("| %-4d | %-38s | %-12s | $%-8.2f | %-6d | %-20s |%n",
                        p.getProductId(),
                        truncate(p.getName(), 38),
                        truncate(p.getCategory(), 12),
                        p.getPrice(),
                        p.getStockQuantity(),
                        stockStatus);
            }
            System.out.println("=========================================================================================================");
            System.out.println();
        } catch (SQLException e) {
            System.out.println("  [!] Failed to load products: " + e.getMessage() + "\n");
        }
    }

    private static void searchProducts() {
        System.out.println(">>> SEARCH PRODUCTS <<<");
        String keyword = InputValidator.readNonEmptyString(scanner, "Enter product name or category keyword: ");
        try {
            List<Product> results = productDAO.searchProducts(keyword);
            System.out.printf("%n  Found %d matching product(s) for '%s':%n", results.size(), keyword);
            displayProductsTable(results);
        } catch (SQLException e) {
            System.out.println("  [!] Search error: " + e.getMessage() + "\n");
        }
    }

    private static void addProductToCartMenu() {
        System.out.println(">>> ADD PRODUCT TO CART <<<");
        displayProductsTable(null);
        int productId = InputValidator.readInt(scanner, "Enter Product ID to add: ", 1, Integer.MAX_VALUE);
        int quantity = InputValidator.readInt(scanner, "Enter Quantity: ", 1, 999);

        String result = shoppingService.addToCart(productId, quantity);
        System.out.println("\n  " + result + "\n");
    }

    private static void viewAndManageCart() {
        List<CartItem> items = shoppingService.getCartItems();
        if (items.isEmpty()) {
            System.out.println("  [i] Your shopping cart is empty.\n");
            return;
        }

        printCartDetails(items);

        System.out.println("Cart Options:");
        System.out.println("  1. Update Item Quantity");
        System.out.println("  2. Remove Item from Cart");
        System.out.println("  3. Clear Entire Cart");
        System.out.println("  4. Return to Customer Menu");

        int choice = InputValidator.readInt(scanner, "Choose action (1-4): ", 1, 4);
        System.out.println();

        switch (choice) {
            case 1 -> {
                int prodId = InputValidator.readInt(scanner, "Enter Product ID to update: ", 1, Integer.MAX_VALUE);
                int newQty = InputValidator.readInt(scanner, "Enter New Quantity (0 to remove): ", 0, 999);
                System.out.println("  " + shoppingService.updateCartQuantity(prodId, newQty) + "\n");
            }
            case 2 -> {
                int prodId = InputValidator.readInt(scanner, "Enter Product ID to remove: ", 1, Integer.MAX_VALUE);
                System.out.println("  " + shoppingService.removeFromCart(prodId) + "\n");
            }
            case 3 -> {
                if (InputValidator.readConfirmation(scanner, "Are you sure you want to empty your cart?")) {
                    shoppingService.clearCart();
                    System.out.println("  [+] Cart emptied.\n");
                }
            }
            case 4 -> {}
        }
    }

    private static void printCartDetails(List<CartItem> items) {
        System.out.println("=========================================================================================");
        System.out.println("                                    YOUR SHOPPING CART                                   ");
        System.out.println("=========================================================================================");
        System.out.printf("| %-4s | %-40s | %-10s | %-5s | %-12s |%n",
                "ID", "Product Name", "Unit Price", "Qty", "Line Total");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (CartItem item : items) {
            System.out.printf("| %-4d | %-40s | $%-9.2f | %-5d | $%-11.2f |%n",
                    item.getProduct().getProductId(),
                    truncate(item.getProduct().getName(), 40),
                    item.getProduct().getPrice(),
                    item.getQuantity(),
                    item.getLineTotal());
        }

        double subtotal = shoppingService.calculateSubtotal();
        double tax = subtotal * TAX_RATE;
        double finalBill = subtotal + tax;

        System.out.println("=========================================================================================");
        System.out.printf("  Subtotal:                                                           $%-11.2f%n", subtotal);
        System.out.printf("  Estimated Sales Tax (%.0f%%):                                          $%-11.2f%n", TAX_RATE * 100, tax);
        System.out.printf("  FINAL BILL GRAND TOTAL:                                             $%-11.2f%n", finalBill);
        System.out.println("=========================================================================================\n");
    }

    private static void checkoutOrder(User customer) {
        List<CartItem> items = shoppingService.getCartItems();
        if (items.isEmpty()) {
            System.out.println("  [!] Your cart is empty. Please add items before checking out.\n");
            return;
        }

        printCartDetails(items);

        boolean confirm = InputValidator.readConfirmation(scanner, "Confirm and place this order?");
        if (!confirm) {
            System.out.println("  [*] Order checkout cancelled.\n");
            return;
        }

        try {
            System.out.println("  [*] Processing order transaction with MySQL...");
            Order order = shoppingService.checkout(customer);
            System.out.println("\n=========================================================================================");
            System.out.println("                         SUCCESS! YOUR ORDER HAS BEEN PLACED                             ");
            System.out.println("=========================================================================================");
            System.out.printf("  Order ID:      #%d%n", order.getOrderId());
            System.out.printf("  Order Date:    %s%n", order.getOrderDate());
            System.out.printf("  Total Paid:    $%.2f%n", order.getTotalAmount());
            System.out.printf("  Status:        %s%n", order.getStatus());
            System.out.println("  Items Purchased:");
            for (OrderItem oi : order.getItems()) {
                System.out.printf("    - %s (Qty: %d) @ $%.2f each = $%.2f%n",
                        oi.getProductName(), oi.getQuantity(), oi.getUnitPrice(), oi.getSubtotal());
            }
            System.out.println("=========================================================================================\n");

        } catch (Exception e) {
            System.out.println("\n  [!] Checkout failed: " + e.getMessage());
            System.out.println("  [*] Transaction was safely rolled back. Your cart items remain intact.\n");
        }
    }

    private static void viewCustomerOrderHistory(User customer) {
        System.out.printf(">>> ORDER HISTORY FOR %s <<<%n", customer.getUsername().toUpperCase());
        try {
            List<Order> orders = shoppingService.getOrderHistory(customer.getUserId());
            if (orders.isEmpty()) {
                System.out.println("  [i] You have not placed any orders yet.\n");
                return;
            }

            for (Order order : orders) {
                System.out.println("=========================================================================================");
                System.out.printf("  Order #%-4d | Date: %-19s | Status: %-10s | Total: $%.2f%n",
                        order.getOrderId(), order.getOrderDate(), order.getStatus(), order.getTotalAmount());
                System.out.println("  Items:");
                for (OrderItem item : order.getItems()) {
                    System.out.printf("    • %-40s | Qty: %-3d | Unit Price: $%-7.2f | Subtotal: $%.2f%n",
                            truncate(item.getProductName(), 40),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getSubtotal());
                }
            }
            System.out.println("=========================================================================================\n");

        } catch (SQLException e) {
            System.out.println("  [!] Error fetching order history: " + e.getMessage() + "\n");
        }
    }

    // =========================================================================
    // ADMIN MENU & ACTIONS
    // =========================================================================

    private static void runAdminMenu(User admin) {
        boolean inAdmin = true;
        while (inAdmin) {
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("  ADMINISTRATION PORTAL | Administrator: %s%n", admin.getFullName());
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("  1. View All Products");
            System.out.println("  2. Add New Product");
            System.out.println("  3. Update Existing Product");
            System.out.println("  4. Delete Product");
            System.out.println("  5. View All Customer Orders & Audit");
            System.out.println("  6. Logout to Main Menu");
            System.out.println("--------------------------------------------------------------------------------");

            int choice = InputValidator.readInt(scanner, "Select an admin option (1-6): ", 1, 6);
            System.out.println();

            switch (choice) {
                case 1 -> displayProductsTable(null);
                case 2 -> adminAddProduct();
                case 3 -> adminUpdateProduct();
                case 4 -> adminDeleteProduct();
                case 5 -> adminViewAllOrders();
                case 6 -> {
                    System.out.println("  [+] Exited Admin Portal.\n");
                    inAdmin = false;
                }
            }
        }
    }

    private static void adminAddProduct() {
        System.out.println(">>> ADD NEW PRODUCT <<<");
        String name = InputValidator.readNonEmptyString(scanner, "Enter Product Name: ");
        String category = InputValidator.readNonEmptyString(scanner, "Enter Category (e.g. Fragrance / Bracelet): ");
        double price = InputValidator.readDouble(scanner, "Enter Price ($): ", 0.01);
        int stock = InputValidator.readInt(scanner, "Enter Initial Stock Quantity: ", 0, 10000);
        String description = InputValidator.readNonEmptyString(scanner, "Enter Description: ");

        try {
            Product added = adminService.addProduct(name, category, price, stock, description);
            if (added != null) {
                System.out.printf("%n  [+] Product '%s' added successfully with ID #%d!%n%n",
                        added.getName(), added.getProductId());
            } else {
                System.out.println("\n  [!] Failed to add product.\n");
            }
        } catch (SQLException e) {
            System.out.println("\n  [!] Database error while adding product: " + e.getMessage() + "\n");
        }
    }

    private static void adminUpdateProduct() {
        System.out.println(">>> UPDATE PRODUCT <<<");
        displayProductsTable(null);
        int productId = InputValidator.readInt(scanner, "Enter Product ID to update: ", 1, Integer.MAX_VALUE);

        try {
            Product existing = adminService.getProductById(productId);
            if (existing == null) {
                System.out.println("  [!] Product with ID " + productId + " not found.\n");
                return;
            }

            System.out.println("\nLeave fields blank and press ENTER to keep current values:");
            String newName = InputValidator.readOptionalString(scanner, "Product Name", existing.getName());
            String newCategory = InputValidator.readOptionalString(scanner, "Category", existing.getCategory());
            double newPrice = InputValidator.readOptionalDouble(scanner, "Price", existing.getPrice(), 0.01);
            int newStock = InputValidator.readOptionalInt(scanner, "Stock Quantity", existing.getStockQuantity(), 0);
            String newDescription = InputValidator.readOptionalString(scanner, "Description", existing.getDescription());

            existing.setName(newName);
            existing.setCategory(newCategory);
            existing.setPrice(newPrice);
            existing.setStockQuantity(newStock);
            existing.setDescription(newDescription);

            boolean updated = adminService.updateProduct(existing);
            if (updated) {
                System.out.printf("%n  [+] Product ID #%d updated successfully!%n%n", productId);
            } else {
                System.out.println("\n  [!] Could not update product.\n");
            }
        } catch (SQLException e) {
            System.out.println("\n  [!] Database error during update: " + e.getMessage() + "\n");
        }
    }

    private static void adminDeleteProduct() {
        System.out.println(">>> DELETE PRODUCT <<<");
        displayProductsTable(null);
        int productId = InputValidator.readInt(scanner, "Enter Product ID to delete: ", 1, Integer.MAX_VALUE);

        boolean confirm = InputValidator.readConfirmation(scanner,
                "Are you sure you want to permanently delete Product ID #" + productId + "?");
        if (!confirm) {
            System.out.println("  [*] Deletion cancelled.\n");
            return;
        }

        try {
            boolean deleted = adminService.deleteProduct(productId);
            if (deleted) {
                System.out.printf("%n  [+] Product ID #%d deleted successfully!%n%n", productId);
            } else {
                System.out.println("  [!] Product not found or could not be deleted.\n");
            }
        } catch (SQLException e) {
            System.out.println("\n  [!] " + e.getMessage() + "\n");
        }
    }

    private static void adminViewAllOrders() {
        System.out.println(">>> ALL STORE ORDERS AUDIT <<<");
        try {
            List<Order> orders = adminService.getAllOrders();
            if (orders.isEmpty()) {
                System.out.println("  [i] No customer orders have been placed yet.\n");
                return;
            }

            double grandRevenue = 0.0;
            for (Order order : orders) {
                grandRevenue += order.getTotalAmount();
                System.out.println("=========================================================================================");
                System.out.printf("  Order #%-4d | Customer: %-15s | Date: %-19s | Total: $%-8.2f | Status: %s%n",
                        order.getOrderId(),
                        order.getCustomerUsername() != null ? order.getCustomerUsername() : "User #" + order.getUserId(),
                        order.getOrderDate(),
                        order.getTotalAmount(),
                        order.getStatus());
                System.out.println("  Items:");
                for (OrderItem item : order.getItems()) {
                    System.out.printf("    • %-40s | Qty: %-3d | Unit Price: $%-7.2f | Subtotal: $%.2f%n",
                            truncate(item.getProductName(), 40),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getSubtotal());
                }
            }
            System.out.println("=========================================================================================");
            System.out.printf("  TOTAL ORDERS: %d | CUMULATIVE REVENUE: $%.2f%n", orders.size(), grandRevenue);
            System.out.println("=========================================================================================\n");

        } catch (SQLException e) {
            System.out.println("  [!] Error retrieving store orders: " + e.getMessage() + "\n");
        }
    }

    // =========================================================================
    // DATABASE SETTINGS & DIAGNOSTICS
    // =========================================================================

    private static void databaseSettingsMenu() {
        System.out.println(">>> DATABASE CONNECTION SETTINGS & DIAGNOSTICS <<<");
        System.out.println("  Current JDBC URL:  " + DBConnection.getActiveUrl());
        System.out.println("  Current User:      " + DBConnection.getActiveUser());
        System.out.print("  Connection Status: ");
        if (DBConnection.testConnection()) {
            System.out.println("CONNECTED (OK)");
        } else {
            System.out.println("DISCONNECTED / ERROR");
        }
        System.out.println();
        System.out.println("Options:");
        System.out.println("  1. Test Connection Now");
        System.out.println("  2. Temporarily Set MySQL Credentials");
        System.out.println("  3. Reset to db.properties");
        System.out.println("  4. Return to Main Menu");

        int choice = InputValidator.readInt(scanner, "Select an option (1-4): ", 1, 4);
        System.out.println();

        switch (choice) {
            case 1 -> {
                System.out.print("Testing connection... ");
                if (DBConnection.testConnection()) {
                    System.out.println("SUCCESS! MySQL connection is healthy.\n");
                } else {
                    System.out.println("FAILED. Could not establish connection.\n");
                }
            }
            case 2 -> {
                String host = InputValidator.readOptionalString(scanner, "MySQL Host", "localhost");
                int port = InputValidator.readOptionalInt(scanner, "MySQL Port", 3306, 1);
                String dbName = InputValidator.readOptionalString(scanner, "Database Name", "ecommerce_db");
                String user = InputValidator.readOptionalString(scanner, "Username", "root");
                System.out.print("Enter Password: ");
                String pass = scanner.nextLine();

                String newUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                        host, port, dbName);

                DBConnection.setCustomConnection(newUrl, user, pass);
                System.out.print("\nTesting new credentials... ");
                if (DBConnection.testConnection()) {
                    System.out.println("SUCCESS! Connected with new settings.\n");
                } else {
                    System.out.println("FAILED. Connection still unreachable.\n");
                }
            }
            case 3 -> {
                DBConnection.resetToDefault();
                System.out.println("  [+] Reset to db.properties defaults.\n");
            }
            case 4 -> {}
        }
    }

    // =========================================================================
    // HELPER UTILITIES
    // =========================================================================

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
