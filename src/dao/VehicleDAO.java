package dao;

import model.Bike;
import model.Car;
import model.Truck;
import model.Vehicle;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Vehicle operations using JDBC PreparedStatements.
 * Demonstrates Polymorphism by instantiating Car, Bike, or Truck subclasses.
 */
public class VehicleDAO {

    /**
     * Map ResultSet row to appropriate Vehicle subclass instance (Polymorphism)
     */
    private Vehicle mapRowToVehicle(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String code = rs.getString("code");
        String name = rs.getString("name");
        String vehicleNumber = rs.getString("vehicle_number");
        String type = rs.getString("type");
        double rentPerDay = rs.getDouble("rent_per_day");
        boolean available = rs.getBoolean("available");

        if ("Bike".equalsIgnoreCase(type)) {
            return new Bike(id, code, name, vehicleNumber, rentPerDay, available);
        } else if ("Truck".equalsIgnoreCase(type)) {
            return new Truck(id, code, name, vehicleNumber, rentPerDay, available);
        } else {
            return new Car(id, code, name, vehicleNumber, rentPerDay, available);
        }
    }

    public List<Vehicle> getAllVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        String sql = "SELECT * FROM vehicles ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                vehicles.add(mapRowToVehicle(rs));
            }
        } catch (SQLException e) {
            System.err.println("[VehicleDAO] Error fetching all vehicles: " + e.getMessage());
        }

        return vehicles;
    }

    public Vehicle getVehicleById(int id) {
        String sql = "SELECT * FROM vehicles WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToVehicle(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[VehicleDAO] Error fetching vehicle by ID: " + e.getMessage());
        }
        return null;
    }

    public Vehicle getVehicleByCode(String code) {
        String sql = "SELECT * FROM vehicles WHERE LOWER(code) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToVehicle(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[VehicleDAO] Error fetching vehicle by Code: " + e.getMessage());
        }
        return null;
    }

    public boolean addVehicle(Vehicle vehicle) throws SQLException {
        String sql = "INSERT INTO vehicles (code, name, vehicle_number, type, rent_per_day, available) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, vehicle.getCode());
            ps.setString(2, vehicle.getName());
            ps.setString(3, vehicle.getVehicleNumber());
            ps.setString(4, vehicle.getVehicleType()); // Calls overridden getVehicleType()
            ps.setDouble(5, vehicle.getRentPerDay());
            ps.setBoolean(6, vehicle.isAvailable());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        vehicle.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean updateVehicle(Vehicle vehicle) throws SQLException {
        String sql = "UPDATE vehicles SET code = ?, name = ?, vehicle_number = ?, type = ?, rent_per_day = ?, available = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vehicle.getCode());
            ps.setString(2, vehicle.getName());
            ps.setString(3, vehicle.getVehicleNumber());
            ps.setString(4, vehicle.getVehicleType());
            ps.setDouble(5, vehicle.getRentPerDay());
            ps.setBoolean(6, vehicle.isAvailable());
            ps.setInt(7, vehicle.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteVehicle(int id) throws SQLException {
        // Check if vehicle has associated rentals before deleting
        String checkSql = "SELECT COUNT(*) FROM rentals WHERE vehicle_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, id);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Cannot delete vehicle because it has associated rental records.");
                }
            }
        }

        String sql = "DELETE FROM vehicles WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Vehicle> searchAndFilterVehicles(String query, String typeFilter, String availabilityFilter) {
        List<Vehicle> vehicles = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM vehicles WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (LOWER(code) LIKE ? OR LOWER(name) LIKE ? OR LOWER(vehicle_number) LIKE ?)");
            String q = "%" + query.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (typeFilter != null && !typeFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(typeFilter)) {
            sql.append(" AND LOWER(type) = LOWER(?)");
            params.add(typeFilter.trim());
        }

        if (availabilityFilter != null && !availabilityFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(availabilityFilter)) {
            if ("AVAILABLE".equalsIgnoreCase(availabilityFilter)) {
                sql.append(" AND available = TRUE");
            } else if ("RENTED".equalsIgnoreCase(availabilityFilter)) {
                sql.append(" AND available = FALSE");
            }
        }

        sql.append(" ORDER BY id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vehicles.add(mapRowToVehicle(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[VehicleDAO] Error searching vehicles: " + e.getMessage());
        }

        return vehicles;
    }

    public boolean isCodeExists(String code, int excludeId) {
        String sql = "SELECT COUNT(*) FROM vehicles WHERE LOWER(code) = LOWER(?) AND id <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isVehicleNumberExists(String vehicleNumber, int excludeId) {
        String sql = "SELECT COUNT(*) FROM vehicles WHERE LOWER(vehicle_number) = LOWER(?) AND id <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicleNumber);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
