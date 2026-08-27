package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class to manage database connections and auto-initialization.
 * Supports MySQL connection with embedded database fallback for out-of-the-box execution.
 */
public class DatabaseConnection {

    private static final String MYSQL_HOST = System.getProperty("db.host", "localhost");
    private static final String MYSQL_PORT = System.getProperty("db.port", "3306");
    private static final String DB_NAME = System.getProperty("db.name", "vehicle_rental");
    private static final String MYSQL_USER = System.getProperty("db.user", "root");
    private static final String MYSQL_PASS = System.getProperty("db.password", "root");

    private static final String MYSQL_BASE_URL = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String MYSQL_DB_URL = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String H2_URL = "jdbc:h2:file:./database/vehicle_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String H2_USER = "sa";
    private static final String H2_PASS = "";

    private static boolean isInitialized = false;
    private static boolean useH2Fallback = false;

    static {
        // Pre-load JDBC drivers if available
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ignored) {
            }
        }

        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {
        }
    }

    /**
     * Obtains a JDBC Connection.
     */
    public static Connection getConnection() throws SQLException {
        if (!isInitialized) {
            initializeDatabase();
        }

        if (useH2Fallback) {
            return DriverManager.getConnection(H2_URL, H2_USER, H2_PASS);
        } else {
            return DriverManager.getConnection(MYSQL_DB_URL, MYSQL_USER, MYSQL_PASS);
        }
    }

    /**
     * Auto-initializes database, tables, and seed data if missing.
     */
    private static synchronized void initializeDatabase() {
        if (isInitialized) return;

        System.out.println("[DatabaseConnection] Initializing database connection...");

        // Try connecting to MySQL first
        try (Connection rootConn = DriverManager.getConnection(MYSQL_BASE_URL, MYSQL_USER, MYSQL_PASS);
             Statement stmt = rootConn.createStatement()) {

            System.out.println("[DatabaseConnection] Connected to MySQL server successfully.");
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            useH2Fallback = false;

        } catch (Exception mysqlEx) {
            System.out.println("[DatabaseConnection] MySQL server not reachable (" + mysqlEx.getMessage() + ").");
            System.out.println("[DatabaseConnection] Falling back to embedded persistent H2 database engine (MySQL compatibility mode).");
            useH2Fallback = true;
        }

        // Initialize schema and seed data
        try (Connection conn = getConnectionForSchemaInit();
             Statement stmt = conn.createStatement()) {

            // Create Vehicles Table
            String createVehicles = "CREATE TABLE IF NOT EXISTS vehicles (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "code VARCHAR(50) NOT NULL UNIQUE, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "vehicle_number VARCHAR(50) NOT NULL UNIQUE, " +
                    "type VARCHAR(20) NOT NULL, " +
                    "rent_per_day DECIMAL(10,2) NOT NULL, " +
                    "available BOOLEAN NOT NULL DEFAULT TRUE" +
                    ")";
            stmt.executeUpdate(createVehicles);

            // Create Customers Table
            String createCustomers = "CREATE TABLE IF NOT EXISTS customers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "age INT NOT NULL, " +
                    "phone VARCHAR(15) NOT NULL, " +
                    "license_number VARCHAR(50) NOT NULL UNIQUE" +
                    ")";
            stmt.executeUpdate(createCustomers);

            // Create Rentals Table
            String createRentals = "CREATE TABLE IF NOT EXISTS rentals (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "rental_code VARCHAR(50) NOT NULL UNIQUE, " +
                    "customer_id INT NOT NULL, " +
                    "vehicle_id INT NOT NULL, " +
                    "rental_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "return_date TIMESTAMP NULL DEFAULT NULL, " +
                    "days INT NOT NULL, " +
                    "total_amount DECIMAL(10,2) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', " +
                    "FOREIGN KEY (customer_id) REFERENCES customers(id), " +
                    "FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)" +
                    ")";
            stmt.executeUpdate(createRentals);

            // Seed Initial Vehicles if empty
            String checkVehicles = "SELECT COUNT(*) FROM vehicles";
            try (ResultSet rs = stmt.executeQuery(checkVehicles)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("[DatabaseConnection] Seeding initial 3 vehicles (V101, V102, V103)...");
                    stmt.executeUpdate("INSERT INTO vehicles (code, name, vehicle_number, type, rent_per_day, available) VALUES " +
                            "('V101', 'Honda City', 'KL 02 AB 1234', 'Car', 2000.00, TRUE), " +
                            "('V102', 'Royal Enfield Classic', 'KL 24 CD 5678', 'Bike', 1000.00, TRUE), " +
                            "('V103', 'Tata Ace', 'KL 02 EF 9012', 'Truck', 2500.00, TRUE)");
                }
            }

            System.out.println("[DatabaseConnection] Database schema & seed initialization complete.");
            isInitialized = true;

        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Error initializing database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Connection getConnectionForSchemaInit() throws SQLException {
        if (useH2Fallback) {
            return DriverManager.getConnection(H2_URL, H2_USER, H2_PASS);
        } else {
            return DriverManager.getConnection(MYSQL_DB_URL, MYSQL_USER, MYSQL_PASS);
        }
    }
}
