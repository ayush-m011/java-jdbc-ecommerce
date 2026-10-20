# Modern Fragrance & Bracelet Boutique — Project Overview

## What is this?
Console-based E-Commerce Management System for a luxury boutique selling
Fragrances and Bracelets. Built with **Core Java + JDBC + MySQL** only.
No Spring, no Hibernate, no web frontend. Single JAR runs both the
console UI ("frontend") and the JDBC business logic ("backend") together.

## Tech Stack
- **Language:** Java 17 (runs on 17/21/26)
- **Build:** Apache Maven 3.8+
- **Database:** MySQL Server 8.0+ (`ecommerce_db`)
- **Driver:** `com.mysql:mysql-connector-j:8.3.0`
- **Tests:** JUnit 5.10.2 + H2 2.2.224 (test scope only, MySQL-mode)
- **Packaging:** `maven-shade-plugin` fat JAR with `Main-Class: com.boutique.ecommerce.Main`

## How It Works
```
Console UI (Main.java)
   → Service layer (ShoppingService, AdminService)
   → DAO layer (UserDAO, ProductDAO, OrderDAO via PreparedStatement)
   → MySQL (users, products, orders, order_items)
```
- Auth uses `SHA-256(salt + password)` with random 16-byte salt.
- Checkout runs in one JDBC transaction:
  `INSERT orders → atomic UPDATE stock WHERE stock>=qty → INSERT order_items → COMMIT / ROLLBACK`.
- All SQL is parameterized — SQL-injection safe.

## Features
1. Customer registration / login / guest browse
2. Product catalog + case-insensitive search
3. Cart with live stock check, quantity update / remove
4. Subtotal + 5% tax + grand total calculation
5. Order placement persisted to `orders` + `order_items`
6. Order history per customer
7. Admin login (role=ADMIN), product CRUD, view all orders
8. Automatic atomic stock reduction, input validation

## Project Layout
```
pom.xml
src/main/java/com/boutique/ecommerce/
  Main.java                 # entry point + menus
  config/DBConnection.java  # db.properties / env loader + getConnection()
  model/User,Product,CartItem,Order,OrderItem.java
  dao/UserDAO,ProductDAO,OrderDAO.java
  service/ShoppingService,AdminService.java
  util/PasswordUtil,InputValidator.java
src/main/resources/
  db.properties             # REAL credentials (IGNORED by git)
  db.properties.example     # template (COMMITTED)
  schema.sql                # CREATE ecommerce_db + seed data
src/test/java/...           # ModelTest, PasswordUtilTest, ShoppingServiceTest, DAOTest
```

## Database
- DBMS required: **MySQL Server 8.0+** (MariaDB 10.5+ also works).
- Seed: 10 products, 2 users (`admin/admin123` ADMIN, `john_doe/password123` CUSTOMER).
- Config priority: `DB_URL/DB_USER/DB_PASSWORD` env vars → `db.properties` → defaults.

## Run (single command — no separate frontend/backend)
```bash
cd "/Users/ayushmerotha/Downloads/Modern Fragrance & Bracelet Boutique"

# one-time DB setup
mysql -u root -p < src/main/resources/schema.sql

# build
mvn clean package

# run whole app
java -jar target/ecommerce-management-system-1.0.0.jar
# dev alternative: mvn exec:java
# tests: mvn test   # 13/13 pass
```

## Security Note
- `src/main/resources/db.properties` holds the real password and is
  ignored via `.gitignore`.
- Only `db.properties.example` is committed. Copy it to create your local
  `db.properties`.
