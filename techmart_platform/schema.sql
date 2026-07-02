-- Create database schema for TechMart
CREATE DATABASE IF NOT EXISTS techmart_db;
USE techmart_db;

-- Drop existing tables to ensure clean slate
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

-- Create users table
-- Create users table (must come before orders because orders references customer email but not FK)
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    active TINYINT(1) NOT NULL DEFAULT 1,
    address VARCHAR(255),
    telephone VARCHAR(20),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username)
);

-- Create products table
CREATE TABLE products (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL
);

-- Create cart items table
CREATE TABLE cart_items (
    id VARCHAR(36) PRIMARY KEY,
    customer_email VARCHAR(100) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_items_customer_product UNIQUE (customer_email, product_id),
    FOREIGN KEY (customer_email) REFERENCES users(email) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Create orders table
CREATE TABLE orders (
    id VARCHAR(50) PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    customer_email VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Seed default ADMIN user (password: admin123)
INSERT INTO users (id, username, email, password_hash, role, active) VALUES
    (UUID(), 'admin', 'admin@techmart.com', 'admin123', 'ADMIN', 1),
    (UUID(), 'customer', 'customer@techmart.com', '1234', 'CUSTOMER', 1);

-- Seed initial product inventory
INSERT INTO products (id, name, price, stock) VALUES
('PROD_001', 'Enterprise SSD 1TB', 199.99, 50000),
('PROD_002', 'Mechanical Keyboard', 89.99, 250);
