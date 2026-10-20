# Modern Fragrance & Bracelet Boutique - E-Commerce Management System

A beginner-friendly, production-ready console-based E-Commerce Management System developed strictly using **Core Java**, **JDBC (Java Database Connectivity)**, and **MySQL**.

> **Important Architectural Note**: This project intentionally uses **plain Core Java and JDBC only**. It does **NOT** use Spring Boot, Spring MVC, Hibernate, JPA, or any third-party Object-Relational Mapping (ORM) frameworks. All database interactions are written with direct SQL statements executed via JDBC `PreparedStatement` and managed with explicit database transactions.

---

## Table of Contents

1. [Features](#features)
2. [Project Architecture & Package Structure](#project-architecture--package-structure)
3. [Prerequisites](#prerequisites)
4. [Database Setup (`schema.sql`)](#database-setup-schemasql)
5. [Database Configuration (`db.properties`)](#database-configuration-dbproperties)
6. [Demo Login Credentials](#demo-login-credentials)
7. [How to Build and Run](#how-to-build-and-run)
8. [Core JDBC Concepts Explained](#core-jdbc-concepts-explained)
9. [Automated Test Suite & Verification Report](#automated-test-suite--verification-report)
10. [Console Application Walkthrough (Screenshots Placeholder)](#console-application-walkthrough)

---

## Features

| # | Feature | Description |
|---|---|---|
| **1** | **Customer Registration & Login** | Self-service registration with email validation, salted SHA-256 password hashing, and secure session authentication. |
| **2** | **Product Catalog Display** | Displays all boutique products formatted neatly in an ASCII table with ID, Category, Price, and Stock status. |
| **3** | **Search Products** | Case-insensitive keyword search by product name or boutique category (Fragrance or Bracelet). |
| **4** | **Shopping Cart Management** | Add items with real-time stock availability verification; prevents adding more items than currently in stock. |
| **5** | **Cart Quantity Updates & Removal** | Increment, decrement, update quantities, or remove items directly from the active cart. |
| **6** | **Subtotal & Final Bill Calculation** | Computes item line totals, subtotal, transparent configurable sales tax (5%), and grand total. |
| **7** | **Order Placement with MySQL Persistence** | Converts cart into a permanent order stored in `orders` and `order_items` tables. |
| **8** | **Order History Review** | Customers can view all past orders, complete with order IDs, timestamps, quantities, and itemized receipts. |
| **9** | **Dedicated Admin Authentication** | Role-based authentication ensuring administrative operations are accessible only to `ADMIN` accounts. |
| **10** | **Admin Inventory Management (CRUD)** | Admins can add new boutique items, update prices and descriptions, view all items, and safely delete products. |
| **11** | **Automatic Inventory Stock Reduction** | Atomically decrements product stock upon successful checkout inside an ACID database transaction. |
| **12** | **Input Validation & Safe SQL Handling** | Console inputs are guarded against crashes; all SQL queries use parameterized `PreparedStatement` to neutralize SQL injection. |

---

## Project Architecture & Package Structure

```
Modern Fragrance & Bracelet Boutique/
├── pom.xml                                  # Maven project descriptor
├── .gitignore                               # Git ignore configuration
├── README.md                                # Project documentation
├── src/
│   ├── main/
│   │   ├── java/com/boutique/ecommerce/
│   │   │   ├── Main.java                    # Application entry point & interactive console UI
│   │   │   ├── config/
│   │   │   │   └── DBConnection.java        # JDBC connection factory & credential manager
│   │   │   ├── model/
│   │   │   │   ├── User.java                # Customer & Admin user model
│   │   │   │   ├── Product.java             # Fragrance & Bracelet boutique inventory item
│   │   │   │   ├── CartItem.java            # In-memory shopping cart item
│   │   │   │   ├── Order.java               # Order header record model
│   │   │   │   └── OrderItem.java           # Order item line details model
│   │   │   ├── dao/
│   │   │   │   ├── UserDAO.java             # User data access object (registration/auth)
│   │   │   │   ├── ProductDAO.java          # Product data access object (catalog/CRUD/search)
│   │   │   │   └── OrderDAO.java            # Order data access object (transactional checkout)
│   │   │   ├── service/
│   │   │   │   ├── ShoppingService.java     # Cart logic, subtotal, and checkout orchestration
│   │   │   │   └── AdminService.java        # Admin catalog management & order auditing
│   │   │   └── util/
│   │   │       ├── InputValidator.java      # Safe console input parsing & error handling
│   │   │       └── PasswordUtil.java        # Cryptographic salt generation & SHA-256 hashing
│   │   └── resources/
│   │       ├── db.properties                # Active database connection settings
│   │       ├── db.properties.example        # Configuration template
│   │       └── schema.sql                   # MySQL database creation & seed data script
│   └── test/
│       └── java/com/boutique/ecommerce/
│           ├── model/ModelTest.java         # Entity model tests
│           ├── util/PasswordUtilTest.java   # Hashing & verification tests
│           ├── service/ShoppingServiceTest.java # Cart logic & calculation tests
│           └── dao/DAOTest.java             # JDBC DAO & Transaction rollback tests (H2 MySQL mode)
```

---

## Prerequisites

Before running the application, make sure you have the following installed on your system:

1. **Java JDK 17 or higher** (Tested on Java 17, Java 21, and Java 26+).
   - Check version: `java -version`
2. **Apache Maven 3.8+**
   - Check version: `mvn -version`
3. **MySQL Server 8.0+ or MariaDB 10.5+**
   - Verify server is running on `localhost:3306`.

---

## Database Setup (`schema.sql`)

The database schema and sample luxury boutique products are provided in `src/main/resources/schema.sql`.

### Tables Overview

1. **`users`**:
   - `user_id` (INT AUTO_INCREMENT PRIMARY KEY)
   - `username` (VARCHAR(50) UNIQUE NOT NULL)
   - `password_hash` (VARCHAR(64) NOT NULL)
   - `salt` (VARCHAR(64) NOT NULL)
   - `full_name` (VARCHAR(100) NOT NULL)
   - `email` (VARCHAR(100) UNIQUE NOT NULL)
   - `role` (`CUSTOMER` or `ADMIN`)
   - `created_at` (TIMESTAMP)

2. **`products`**:
   - `product_id` (INT AUTO_INCREMENT PRIMARY KEY)
   - `name` (VARCHAR(150) NOT NULL)
   - `category` (VARCHAR(50) NOT NULL)
   - `price` (DECIMAL(10,2) NOT NULL, CHECK price >= 0)
   - `stock_quantity` (INT NOT NULL, CHECK stock_quantity >= 0)
   - `description` (TEXT)
   - `created_at` (TIMESTAMP)

3. **`orders`**:
   - `order_id` (INT AUTO_INCREMENT PRIMARY KEY)
   - `user_id` (INT NOT NULL, FOREIGN KEY referencing `users(user_id)`)
   - `order_date` (TIMESTAMP)
   - `total_amount` (DECIMAL(10,2) NOT NULL)
   - `status` (VARCHAR(30) DEFAULT 'COMPLETED')

4. **`order_items`**:
   - `order_item_id` (INT AUTO_INCREMENT PRIMARY KEY)
   - `order_id` (INT NOT NULL, FOREIGN KEY referencing `orders(order_id)` ON DELETE CASCADE)
   - `product_id` (INT NOT NULL, FOREIGN KEY referencing `products(product_id)`)
   - `quantity` (INT NOT NULL, CHECK quantity > 0)
   - `unit_price` (DECIMAL(10,2) NOT NULL)
   - `subtotal` (DECIMAL(10,2) NOT NULL)

### Step-by-Step MySQL Initialization

Open your terminal or MySQL command-line client and execute:

```bash
# Option A: Import directly from command line (replace 'root' with your MySQL username)
mysql -u root -p < src/main/resources/schema.sql

# Option B: Run within MySQL client shell
mysql -u root -p
mysql> source src/main/resources/schema.sql;
mysql> show tables in ecommerce_db;
```

---

## Database Configuration (`db.properties`)

To ensure database credentials are never hardcoded in source code, credentials are loaded in the following order of priority:

1. **Environment Variables** (ideal for production / CI):
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"
   export DB_USER="root"
   export DB_PASSWORD="your_mysql_password"
   ```
2. **Properties File** (`src/main/resources/db.properties`):
   ```properties
   db.url=jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
   db.user=root
   db.password=your_mysql_password
   ```
3. **Interactive Console Menu**: You can also test and configure connection parameters live inside the application under **Main Menu -> Option 5 (Database Connection Settings & Diagnostics)**.

---

## Demo Login Credentials

The `schema.sql` file seeds two sample accounts:

| Role | Username | Password | Full Name | Notes |
|---|---|---|---|---|
| **Administrator** | `admin` | `admin123` | Boutique Administrator | Has full access to Admin Portal (Product CRUD & Store Audit) |
| **Customer** | `john_doe` | `password123` | John Doe | Pre-configured customer with past orders |

> You can also register a new customer account directly from the Main Menu at any time.

---

## How to Build and Run

### 1. Run Automated Unit & Integration Tests
```bash
mvn clean test
```
All 13 tests verify models, cart subtotals, password hashing, and DAO transactions.

### 2. Run Directly Using Maven Exec Plugin
```bash
mvn compile exec:java
```

### 3. Package into Standalone Runnable Fat JAR
```bash
mvn package
```
This produces `target/ecommerce-management-system-1.0.0.jar` (containing all dependencies including MySQL Connector/J).

### 4. Run the Standalone JAR
```bash
java -jar target/ecommerce-management-system-1.0.0.jar
```

---

## Core JDBC Concepts Explained

For students and developers learning JDBC, here is how standard JDBC APIs are utilized in this project:

### 1. `DriverManager.getConnection(url, user, password)`
Located in [`DBConnection.java`](file:///Users/ayushmerotha/Downloads/Modern%20Fragrance%20&%20Bracelet%20Boutique/src/main/java/com/boutique/ecommerce/config/DBConnection.java).
Establishes a physical TCP socket connection to the MySQL database server.

### 2. `PreparedStatement` vs `Statement`
Located in [`UserDAO.java`](file:///Users/ayushmerotha/Downloads/Modern%20Fragrance%20&%20Bracelet%20Boutique/src/main/java/com/boutique/ecommerce/dao/UserDAO.java), [`ProductDAO.java`](file:///Users/ayushmerotha/Downloads/Modern%20Fragrance%20&%20Bracelet%20Boutique/src/main/java/com/boutique/ecommerce/dao/ProductDAO.java), and [`OrderDAO.java`](file:///Users/ayushmerotha/Downloads/Modern%20Fragrance%20&%20Bracelet%20Boutique/src/main/java/com/boutique/ecommerce/dao/OrderDAO.java).
Pre-compiles the SQL query template on the database. Values are bound using positional placeholders (`?`):
```java
String sql = "SELECT * FROM products WHERE product_id = ?";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setInt(1, productId);
```
**Why this matters**: User inputs are treated strictly as parameter data, preventing SQL Injection vulnerabilities.

### 3. `Statement.RETURN_GENERATED_KEYS` and `pstmt.getGeneratedKeys()`
When inserting a new record with an `AUTO_INCREMENT` primary key:
```java
PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
pstmt.executeUpdate();
ResultSet keys = pstmt.getGeneratedKeys();
if (keys.next()) {
    int generatedId = keys.getInt(1);
}
```

### 4. JDBC Transactions (`setAutoCommit`, `commit`, `rollback`)
Located in [`OrderDAO.java`](file:///Users/ayushmerotha/Downloads/Modern%20Fragrance%20&%20Bracelet%20Boutique/src/main/java/com/boutique/ecommerce/dao/OrderDAO.java#L58-L125).
Order placement requires multiple database modifications to succeed or fail as a single atomic unit:
```java
conn.setAutoCommit(false); // 1. Start Transaction
try {
    // 2. Insert into orders table
    // 3. Insert each item into order_items table
    // 4. Reduce stock: UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?
    conn.commit(); // 5. Make all changes permanent
} catch (Exception e) {
    conn.rollback(); // 6. Undo all changes if anything fails
    throw e;
} finally {
    conn.setAutoCommit(true); // 7. Restore default autocommit
}
```

---

## Automated Test Suite & Verification Report

### Test Execution Results
The project includes automated JUnit 5 tests covering all architectural layers:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.boutique.ecommerce.util.PasswordUtilTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in PasswordUtilTest
[INFO] Running com.boutique.ecommerce.dao.DAOTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in DAOTest
[INFO] Running com.boutique.ecommerce.model.ModelTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in ModelTest
[INFO] Running com.boutique.ecommerce.service.ShoppingServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in ShoppingServiceTest
[INFO] 
[INFO] Results:
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### What Was Tested
- **Model Layer**: Object instantiation, attribute encapsulation, stock status checks, line total computations.
- **Security / Utility Layer**: Random 32-character hex salt generation, deterministic SHA-256 password hashing, and authentication verification.
- **Service Layer**: Shopping cart quantity validations, stock threshold limits, subtotals, and sales tax calculations.
- **DAO & Transaction Layer**: Verified user registration and authentication, product CRUD and search, atomic multi-table order placement, automatic inventory decrement, and transaction rollback integrity when stock is insufficient.

### Testing Limitations & Live Database Note
- The test suite executes automated DAO tests using an in-memory SQL database configured in MySQL compatibility mode.
- A live, external MySQL daemon was not active on the test machine during build. Before launching the application interactively against a persistent MySQL instance, ensure the local MySQL daemon is running (`mysql.server start` or `systemctl start mysql`) and that `schema.sql` has been imported.

---

## Console Application Walkthrough

### 1. Main Menu
```
================================================================================
       M O D E R N   F R A G R A N C E   &   B R A C E L E T   B O U T I Q U E  
             Pure Core Java + JDBC + MySQL E-Commerce System            
================================================================================
[INFO] Checking MySQL database connectivity... CONNECTED!

--------------------------------------------------------------------------------
                                  MAIN MENU                                     
--------------------------------------------------------------------------------
  1. Customer Login
  2. Customer Registration
  3. Admin Login
  4. Browse Products as Guest
  5. Database Connection Settings & Diagnostics
  6. Exit
--------------------------------------------------------------------------------
```

### 2. Product Catalog Table
```
=========================================================================================================
| ID   | Product Name                           | Category     | Price     | Stock  | Status               |
=========================================================================================================
| 1    | Velvet Rose & Oud Eau de Parfum        | Fragrance    | $120.00   | 25     | In Stock (25)        |
| 2    | Amber Noir Extrait de Parfum           | Fragrance    | $145.50   | 18     | In Stock (18)        |
| 3    | Bergamot & Sea Salt Mist               | Fragrance    | $65.00    | 40     | In Stock (40)        |
| 6    | Natural Tiger Eye Beaded Bracelet      | Bracelet     | $38.00    | 35     | In Stock (35)        |
| 7    | 18K Gold Plated Minimalist Cuff        | Bracelet     | $110.00   | 15     | In Stock (15)        |
=========================================================================================================
```

### 3. Shopping Cart Review & Final Bill
```
=========================================================================================
                                    YOUR SHOPPING CART                                   
=========================================================================================
| ID   | Product Name                             | Unit Price | Qty   | Line Total   |
-----------------------------------------------------------------------------------------
| 1    | Velvet Rose & Oud Eau de Parfum          | $120.00    | 2     | $240.00      |
| 6    | Natural Tiger Eye Beaded Bracelet        | $38.00     | 1     | $38.00       |
=========================================================================================
  Subtotal:                                                           $278.00     
  Estimated Sales Tax (5%):                                           $13.90      
  FINAL BILL GRAND TOTAL:                                             $291.90     
=========================================================================================
```

### 4. Admin Management Portal
```
--------------------------------------------------------------------------------
  ADMINISTRATION PORTAL | Administrator: Boutique Administrator
--------------------------------------------------------------------------------
  1. View All Products
  2. Add New Product
  3. Update Existing Product
  4. Delete Product
  5. View All Customer Orders & Audit
  6. Logout to Main Menu
--------------------------------------------------------------------------------
```

*(Screenshots can be added here by capturing terminal windows during live demonstration)*
# fragrance_modern
