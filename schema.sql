-- Vehicle Rental Management System Database Schema
-- KTU S3 OOP Micro-Project

CREATE DATABASE IF NOT EXISTS vehicle_rental;
USE vehicle_rental;

-- --------------------------------------------------------
-- Table structure for `vehicles`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    vehicle_number VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL, -- 'Car', 'Bike', 'Truck'
    rent_per_day DECIMAL(10, 2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------
-- Table structure for `customers`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    phone VARCHAR(15) NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------
-- Table structure for `rentals`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS rentals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rental_code VARCHAR(50) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    rental_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    return_date TIMESTAMP NULL DEFAULT NULL,
    days INT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'RETURNED'
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT
);

-- --------------------------------------------------------
-- Initial Seed Data: Vehicles
-- --------------------------------------------------------
INSERT INTO vehicles (code, name, vehicle_number, type, rent_per_day, available) 
VALUES ('V101', 'Honda City', 'KL 02 AB 1234', 'Car', 2000.00, TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO vehicles (code, name, vehicle_number, type, rent_per_day, available) 
VALUES ('V102', 'Royal Enfield Classic', 'KL 24 CD 5678', 'Bike', 1000.00, TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO vehicles (code, name, vehicle_number, type, rent_per_day, available) 
VALUES ('V103', 'Tata Ace', 'KL 02 EF 9012', 'Truck', 2500.00, TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name);
