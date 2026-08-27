package dao;

import model.Rental;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Rental transactions.
 * Implements strict ACID Transactional Processing (setAutoCommit(false), commit, rollback).
 */
public class RentalDAO {

    private Rental mapRowToRental(ResultSet rs) throws SQLException {
        Rental r = new Rental();
        r.setId(rs.getInt("id"));
        r.setRentalCode(rs.getString("rental_code"));
        r.setCustomerId(rs.getInt("customer_id"));
        r.setCustomerName(rs.getString("customer_name"));
        r.setCustomerAge(rs.getInt("customer_age"));
        r.setCustomerPhone(rs.getString("customer_phone"));
        r.setCustomerLicense(rs.getString("customer_license"));

        r.setVehicleId(rs.getInt("vehicle_id"));
        r.setVehicleCode(rs.getString("vehicle_code"));
        r.setVehicleName(rs.getString("vehicle_name"));
        r.setVehicleNumber(rs.getString("vehicle_number"));
        r.setVehicleType(rs.getString("vehicle_type"));
        r.setRentPerDay(rs.getDouble("rent_per_day"));

        r.setRentalDate(rs.getTimestamp("rental_date"));
        r.setReturnDate(rs.getTimestamp("return_date"));
        r.setDays(rs.getInt("days"));
        r.setTotalAmount(rs.getDouble("total_amount"));
        r.setStatus(rs.getString("status"));
        return r;
    }

    private static final String SELECT_RENTAL_JOIN = 
            "SELECT r.*, " +
            "c.name AS customer_name, c.age AS customer_age, c.phone AS customer_phone, c.license_number AS customer_license, " +
            "v.code AS vehicle_code, v.name AS vehicle_name, v.vehicle_number AS vehicle_number, v.type AS vehicle_type, v.rent_per_day AS rent_per_day " +
            "FROM rentals r " +
            "JOIN customers c ON r.customer_id = c.id " +
            "JOIN vehicles v ON r.vehicle_id = v.id ";

    /**
     * Process a vehicle rental using ACID transaction handling.
     */
    public Rental processRental(int customerId, int vehicleId, int days) throws SQLException {
        if (days <= 0) {
            throw freshSQLException("Rental days must be greater than zero.");
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Verify Customer
            String custSql = "SELECT * FROM customers WHERE id = ?";
            String customerName, customerPhone, customerLicense;
            int customerAge;
            try (PreparedStatement custPs = conn.prepareStatement(custSql)) {
                custPs.setInt(1, customerId);
                try (ResultSet rs = custPs.executeQuery()) {
                    if (!rs.next()) {
                        throw freshSQLException("Selected customer not found (ID: " + customerId + ").");
                    }
                    customerName = rs.getString("name");
                    customerAge = rs.getInt("age");
                    customerPhone = rs.getString("phone");
                    customerLicense = rs.getString("license_number");
                    if (customerAge < 18) {
                        throw freshSQLException("Customer must be at least 18 years old to rent a vehicle.");
                    }
                }
            }

            // 2. Lock & Check Vehicle Availability
            String vehSql = "SELECT * FROM vehicles WHERE id = ?";
            String vehicleCode, vehicleName, vehicleNumber, vehicleType;
            double rentPerDay;
            boolean available;

            try (PreparedStatement vehPs = conn.prepareStatement(vehSql)) {
                vehPs.setInt(1, vehicleId);
                try (ResultSet rs = vehPs.executeQuery()) {
                    if (!rs.next()) {
                        throw freshSQLException("Selected vehicle not found (ID: " + vehicleId + ").");
                    }
                    vehicleCode = rs.getString("code");
                    vehicleName = rs.getString("name");
                    vehicleNumber = rs.getString("vehicle_number");
                    vehicleType = rs.getString("type");
                    rentPerDay = rs.getDouble("rent_per_day");
                    available = rs.getBoolean("available");

                    if (!available) {
                        throw freshSQLException("Vehicle " + vehicleName + " (" + vehicleCode + ") is currently rented / unavailable.");
                    }
                }
            }

            // 3. Calculate Total Amount
            double totalAmount = rentPerDay * days;

            // 4. Generate Unique Rental Code
            String rentalCode = "RNT-" + System.currentTimeMillis() % 1000000;

            // 5. Insert Rental Record
            String insertSql = "INSERT INTO rentals (rental_code, customer_id, vehicle_id, days, total_amount, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')";
            int rentalId = -1;
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPs.setString(1, rentalCode);
                insertPs.setInt(2, customerId);
                insertPs.setInt(3, vehicleId);
                insertPs.setInt(4, days);
                insertPs.setDouble(5, totalAmount);
                insertPs.executeUpdate();

                try (ResultSet keys = insertPs.getGeneratedKeys()) {
                    if (keys.next()) {
                        rentalId = keys.getInt(1);
                    }
                }
            }

            // 6. Update Vehicle Status to Rented (available = false)
            String updateVehSql = "UPDATE vehicles SET available = FALSE WHERE id = ?";
            try (PreparedStatement updatePs = conn.prepareStatement(updateVehSql)) {
                updatePs.setInt(1, vehicleId);
                updatePs.executeUpdate();
            }

            // Commit Transaction
            conn.commit();

            // Fetch created rental details for receipt
            Rental rental = getRentalById(rentalId);
            if (rental == null) {
                // Construct fallback object
                rental = new Rental();
                rental.setId(rentalId);
                rental.setRentalCode(rentalCode);
                rental.setCustomerId(customerId);
                rental.setCustomerName(customerName);
                rental.setCustomerAge(customerAge);
                rental.setCustomerPhone(customerPhone);
                rental.setCustomerLicense(customerLicense);
                rental.setVehicleId(vehicleId);
                rental.setVehicleCode(vehicleCode);
                rental.setVehicleName(vehicleName);
                rental.setVehicleNumber(vehicleNumber);
                rental.setVehicleType(vehicleType);
                rental.setRentPerDay(rentPerDay);
                rental.setRentalDate(new Timestamp(System.currentTimeMillis()));
                rental.setDays(days);
                rental.setTotalAmount(totalAmount);
                rental.setStatus("ACTIVE");
            }
            return rental;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[RentalDAO] Rollback failed: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Return a rented vehicle and restore its status to Available.
     */
    public boolean returnVehicle(int rentalId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Fetch active rental
            String fetchSql = "SELECT vehicle_id, status FROM rentals WHERE id = ?";
            int vehicleId = -1;
            String status = "";
            try (PreparedStatement ps = conn.prepareStatement(fetchSql)) {
                ps.setInt(1, rentalId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw freshSQLException("Rental record not found (ID: " + rentalId + ").");
                    }
                    vehicleId = rs.getInt("vehicle_id");
                    status = rs.getString("status");
                }
            }

            if (!"ACTIVE".equalsIgnoreCase(status)) {
                throw freshSQLException("Rental is already marked as RETURNED.");
            }

            // 2. Update rental status
            String updateRentalSql = "UPDATE rentals SET status = 'RETURNED', return_date = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateRentalSql)) {
                ps.setInt(1, rentalId);
                ps.executeUpdate();
            }

            // 3. Mark vehicle as available
            String updateVehSql = "UPDATE vehicles SET available = TRUE WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateVehSql)) {
                ps.setInt(1, vehicleId);
                ps.executeUpdate();
            }

            // Commit Transaction
            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[RentalDAO] Rollback failed: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public Rental getRentalById(int rentalId) {
        String sql = SELECT_RENTAL_JOIN + "WHERE r.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, rentalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRental(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RentalDAO] Error fetching rental by ID: " + e.getMessage());
        }
        return null;
    }

    public Rental getRentalByCode(String rentalCode) {
        String sql = SELECT_RENTAL_JOIN + "WHERE LOWER(r.rental_code) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, rentalCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRental(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RentalDAO] Error fetching rental by code: " + e.getMessage());
        }
        return null;
    }

    public List<Rental> getActiveRentals() {
        List<Rental> list = new ArrayList<>();
        String sql = SELECT_RENTAL_JOIN + "WHERE r.status = 'ACTIVE' ORDER BY r.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToRental(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RentalDAO] Error fetching active rentals: " + e.getMessage());
        }
        return list;
    }

    public List<Rental> getAllRentals() {
        List<Rental> list = new ArrayList<>();
        String sql = SELECT_RENTAL_JOIN + "ORDER BY r.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToRental(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RentalDAO] Error fetching all rentals: " + e.getMessage());
        }
        return list;
    }

    public List<Rental> searchRentals(String query, String statusFilter) {
        List<Rental> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_RENTAL_JOIN + "WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (LOWER(r.rental_code) LIKE ? OR LOWER(c.name) LIKE ? OR LOWER(v.name) LIKE ? OR LOWER(v.vehicle_number) LIKE ?)");
            String q = "%" + query.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append(" AND LOWER(r.status) = LOWER(?)");
            params.add(statusFilter.trim());
        }

        sql.append(" ORDER BY r.id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToRental(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RentalDAO] Error searching rentals: " + e.getMessage());
        }

        return list;
    }

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> map = new HashMap<>();
        int totalVehicles = 0;
        int availableVehicles = 0;
        int rentedVehicles = 0;
        int totalCustomers = 0;
        int activeRentals = 0;
        double totalRevenue = 0.0;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Vehicles breakdown
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*), SUM(CASE WHEN available = TRUE THEN 1 ELSE 0 END) FROM vehicles")) {
                if (rs.next()) {
                    totalVehicles = rs.getInt(1);
                    availableVehicles = rs.getInt(2);
                    rentedVehicles = totalVehicles - availableVehicles;
                }
            }

            // Total Customers
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                if (rs.next()) {
                    totalCustomers = rs.getInt(1);
                }
            }

            // Active Rentals
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rentals WHERE status = 'ACTIVE'")) {
                if (rs.next()) {
                    activeRentals = rs.getInt(1);
                }
            }

            // Total Revenue
            try (ResultSet rs = stmt.executeQuery("SELECT SUM(total_amount) FROM rentals")) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("[RentalDAO] Error fetching dashboard metrics: " + e.getMessage());
        }

        map.put("totalVehicles", totalVehicles);
        map.put("availableVehicles", availableVehicles);
        map.put("rentedVehicles", rentedVehicles);
        map.put("totalCustomers", totalCustomers);
        map.put("activeRentals", activeRentals);
        map.put("totalRevenue", totalRevenue);

        return map;
    }

    private SQLException freshSQLException(String msg) {
        return new SQLException(msg);
    }
}
