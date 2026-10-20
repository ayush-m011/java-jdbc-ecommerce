package com.boutique.ecommerce.dao;

import com.boutique.ecommerce.config.DBConnection;
import com.boutique.ecommerce.model.User;
import com.boutique.ecommerce.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * UserDAO - Data Access Object for Users
 *
 * Handles database operations for customer and admin accounts using PreparedStatement.
 *
 * Key JDBC Methods Explained:
 * - conn.prepareStatement(sql): Pre-compiles the SQL query template on the database server.
 *   This separates code from data, completely neutralizing SQL Injection attacks.
 * - pstmt.setString(index, value): Safely binds parameters by index (1-based).
 * - pstmt.executeUpdate(): Executes an INSERT, UPDATE, or DELETE statement, returning the number of affected rows.
 * - pstmt.executeQuery(): Executes a SELECT statement, returning a ResultSet cursor.
 * - rs.next(): Advances the cursor to the next row; returns true if a row is present.
 */
public class UserDAO {

    /**
     * Registers a new user into the database.
     *
     * @param user The user object containing registration details.
     * @return The saved User with generated user_id, or null if registration failed.
     * @throws SQLException if a database error occurs (e.g. duplicate username/email).
     */
    public User register(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, salt, full_name, email, role) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getSalt());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getRole() != null ? user.getRole() : "CUSTOMER");

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1));
                    }
                }
                return user;
            }
        } catch (SQLException e) {
            // Check for duplicate entry constraint violation (MySQL error code 1062 or SQLState 23000)
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new SQLException("Registration failed: Username or email is already registered.", e);
            }
            throw new SQLException("Error registering user: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Retrieves a user by their unique username.
     *
     * @param username The username to look up.
     * @return The User object if found, or null otherwise.
     * @throws SQLException if a database error occurs.
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, salt, full_name, email, role, created_at FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Error finding user by username [" + username + "]: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Authenticates a user given their username and plain-text password.
     *
     * @param username Provided username.
     * @param plainPassword Provided plain password.
     * @return The authenticated User object, or null if credentials do not match.
     * @throws SQLException if a database error occurs.
     */
    public User authenticate(String username, String plainPassword) throws SQLException {
        User user = findByUsername(username);
        if (user == null) {
            return null; // Username not found
        }

        // Verify salted hash
        boolean passwordMatches = PasswordUtil.verifyPassword(plainPassword, user.getSalt(), user.getPasswordHash());
        if (passwordMatches) {
            return user;
        }
        return null; // Incorrect password
    }

    /**
     * Checks if a username is already taken.
     */
    public boolean usernameExists(String username) throws SQLException {
        return findByUsername(username) != null;
    }

    /**
     * Helper method to map a ResultSet row to a User object.
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("salt"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getTimestamp("created_at")
        );
    }
}
