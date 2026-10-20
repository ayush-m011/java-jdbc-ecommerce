package com.boutique.ecommerce.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * PasswordUtil - Secure Password Hashing and Verification
 *
 * Provides cryptographic hashing using SHA-256 combined with a random salt.
 * This prevents plain-text storage of user credentials in the MySQL database.
 */
public class PasswordUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a cryptographically secure random hexadecimal salt.
     *
     * @return 32-character hexadecimal string salt.
     */
    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        SECURE_RANDOM.nextBytes(saltBytes);
        return bytesToHex(saltBytes);
    }

    /**
     * Hashes the plain text password combined with the provided salt using SHA-256.
     *
     * @param password Plain-text user password.
     * @param salt Unique per-user salt.
     * @return 64-character hexadecimal SHA-256 digest string.
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPassword = salt + password;
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in current JVM", e);
        }
    }

    /**
     * Verifies whether an input password matches the stored hash when combined with the salt.
     *
     * @param password Plain-text password entered by user.
     * @param salt Stored salt from the database.
     * @param expectedHash Stored password hash from the database.
     * @return true if password is correct, false otherwise.
     */
    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        if (password == null || salt == null || expectedHash == null) {
            return false;
        }
        String computedHash = hashPassword(password, salt);
        return computedHash.equalsIgnoreCase(expectedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
