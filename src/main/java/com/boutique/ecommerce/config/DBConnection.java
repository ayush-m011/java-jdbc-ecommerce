package com.boutique.ecommerce.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection - Database Connection Manager
 *
 * This class handles database connection initialization using plain JDBC.
 * It reads configuration properties from:
 *   1. Environment variables (DB_URL, DB_USER, DB_PASSWORD) if set.
 *   2. The classpath resource 'db.properties' as a fallback.
 *   3. Safe default localhost values if neither provides a setting.
 *
 * Beginner JDBC Concepts Explained:
 * - DriverManager: The factory service for managing JDBC drivers and establishing connections.
 * - Connection: Represents an active session with the MySQL database.
 * - Transactions: Managed via conn.setAutoCommit(false), conn.commit(), and conn.rollback().
 */
public class DBConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static String url;
    private static String user;
    private static String password;

    // Optional override for automated testing (e.g., in-memory H2 database)
    private static String customUrl = null;
    private static String customUser = null;
    private static String customPassword = null;

    static {
        loadConfiguration();
    }

    /**
     * Loads database configuration from classpath properties file and environment variables.
     */
    public static void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try to load from db.properties file in resources
        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            System.err.println("[WARN] Could not load db.properties: " + e.getMessage());
        }

        // 2. Read values with precedence: Environment Variable -> db.properties -> Defaults
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        url = (envUrl != null && !envUrl.isBlank()) ? envUrl : props.getProperty("db.url", DEFAULT_URL);
        user = (envUser != null && !envUser.isBlank()) ? envUser : props.getProperty("db.user", DEFAULT_USER);
        password = (envPass != null) ? envPass : props.getProperty("db.password", DEFAULT_PASSWORD);

        // Explicitly register MySQL JDBC driver class for compatibility across runtimes
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            // Modern JDBC drivers auto-register via ServiceLoader, class loading is fallback
        }
    }

    /**
     * Retrieves a new database connection.
     *
     * @return an open {@link Connection} to the database.
     * @throws SQLException if a database access error occurs or server is unreachable.
     */
    public static Connection getConnection() throws SQLException {
        String activeUrl = (customUrl != null) ? customUrl : url;
        String activeUser = (customUser != null) ? customUser : user;
        String activePassword = (customPassword != null) ? customPassword : password;

        try {
            return DriverManager.getConnection(activeUrl, activeUser, activePassword);
        } catch (SQLException e) {
            throw new SQLException(
                "Failed to connect to database at [" + activeUrl + "] with user [" + activeUser + "].\n" +
                "Reason: " + e.getMessage() + "\n" +
                "Tip: Ensure MySQL is running, 'ecommerce_db' is created using schema.sql, " +
                "and valid credentials are set in db.properties or via DB_USER/DB_PASSWORD environment variables.",
                e
            );
        }
    }

    /**
     * Tests whether a connection can be successfully established.
     *
     * @return true if connection succeeds, false otherwise.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Allows tests to override database connection details (e.g., pointing to H2 test database).
     */
    public static void setCustomConnection(String testUrl, String testUser, String testPassword) {
        customUrl = testUrl;
        customUser = testUser;
        customPassword = testPassword;
    }

    /**
     * Resets any custom test connection override back to default settings.
     */
    public static void resetToDefault() {
        customUrl = null;
        customUser = null;
        customPassword = null;
        loadConfiguration();
    }

    public static String getActiveUrl() {
        return (customUrl != null) ? customUrl : url;
    }

    public static String getActiveUser() {
        return (customUser != null) ? customUser : user;
    }
}
