-- ====================================================================
-- Modern Fragrance & Bracelet Boutique - Database Schema
-- Database Name: ecommerce_db
-- ====================================================================

-- 1. Create the Database if it does not already exist
CREATE DATABASE IF NOT EXISTS ecommerce_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_db;

-- 2. Drop existing tables in reverse dependency order (if resetting database)
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

-- 3. Users Table (Customer & Admin Accounts)
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
) ENGINE=InnoDB;

-- 4. Products Table
CREATE TABLE products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_product_price CHECK (price >= 0.00),
    CONSTRAINT chk_product_stock CHECK (stock_quantity >= 0)
) ENGINE=InnoDB;

-- 5. Orders Table
CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    CONSTRAINT chk_order_total CHECK (total_amount >= 0.00),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 6. Order Items Table (Junction between Orders and Products)
CREATE TABLE order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    CONSTRAINT chk_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_item_price CHECK (unit_price >= 0.00),
    CONSTRAINT chk_item_subtotal CHECK (subtotal >= 0.00),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ====================================================================
-- Sample Seed Data
-- ====================================================================

-- Users:
-- Admin:    admin / admin123
-- Customer: john_doe / password123
INSERT INTO users (username, password_hash, salt, full_name, email, role) VALUES
('admin', '8b1ab071c62ea9da1a5ae6c06ad50fecaa58181762f5bda31adab09bfda0928d', 's@lt_boutique_2026', 'Boutique Administrator', 'admin@boutique.com', 'ADMIN'),
('john_doe', '19ebdc7412d9329f8d71185817fc91b2b13efda516d5ccf1e155f93d67c180c8', 's@lt_boutique_2026', 'John Doe', 'john.doe@example.com', 'CUSTOMER');

-- Products (Luxury Fragrances and Designer Bracelets):
INSERT INTO products (name, category, price, stock_quantity, description) VALUES
('Velvet Rose & Oud Eau de Parfum', 'Fragrance', 120.00, 25, 'A decadent blend of Damascus rose wrapped in smoky oud wood, clove, and praline.'),
('Amber Noir Extrait de Parfum', 'Fragrance', 145.50, 18, 'Intense and sultry oriental fragrance with rich vanilla, golden amber, and patchouli.'),
('Bergamot & Sea Salt Mist', 'Fragrance', 65.00, 40, 'Fresh ocean breeze infused with sun-drenched Italian bergamot and mineral driftwood.'),
('Midnight Santal Cologne', 'Fragrance', 95.00, 22, 'Earthy Australian sandalwood paired with cardamom, violet leaves, and smooth cedar.'),
('Vanilla Silk Artisan Perfume Oil', 'Fragrance', 55.00, 30, 'Concentrated fragrance oil featuring pure Madagascar bourbon vanilla and warm skin musk.'),
('Natural Tiger Eye Beaded Bracelet', 'Bracelet', 38.00, 35, 'Handcrafted 8mm natural golden tiger eye gemstones on resilient stretch cord.'),
('18K Gold Plated Minimalist Cuff', 'Bracelet', 110.00, 15, 'Timeless solid brass cuff plated with genuine 18-karat gold with subtle hand-hammered texture.'),
('Sterling Silver Herringbone Chain Bracelet', 'Bracelet', 75.00, 20, 'Sleek and liquid-like 925 sterling silver flat herringbone chain bracelet, 7.5 inch.'),
('Lapis Lazuli & Silver Bead Bracelet', 'Bracelet', 48.50, 28, 'Deep royal blue celestial lapis lazuli stones accented with antique silver-finished spacer rings.'),
('Braided Italian Leather & Steel Bangle', 'Bracelet', 58.00, 25, 'Premium double-braided black Italian leather bracelet with a magnetic brushed stainless steel clasp.');
