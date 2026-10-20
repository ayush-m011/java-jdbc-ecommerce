package com.boutique.ecommerce.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    @DisplayName("Should generate unique non-empty salts")
    void testSaltGeneration() {
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();

        assertNotNull(salt1);
        assertNotNull(salt2);
        assertEquals(32, salt1.length());
        assertEquals(32, salt2.length());
        assertNotEquals(salt1, salt2, "Each generated salt must be uniquely randomized");
    }

    @Test
    @DisplayName("Should hash passwords deterministically with given salt")
    void testHashPassword() {
        String salt = "s@lt_boutique_2026";
        String plain = "admin123";

        String hash1 = PasswordUtil.hashPassword(plain, salt);
        String hash2 = PasswordUtil.hashPassword(plain, salt);

        assertEquals(64, hash1.length(), "SHA-256 output must be 64 hex characters");
        assertEquals(hash1, hash2, "Hashing same password and salt must yield identical hash");
        assertEquals("8b1ab071c62ea9da1a5ae6c06ad50fecaa58181762f5bda31adab09bfda0928d", hash1);
    }

    @Test
    @DisplayName("Should correctly verify valid passwords and reject invalid ones")
    void testVerifyPassword() {
        String salt = PasswordUtil.generateSalt();
        String plain = "SecretP@ss2026";
        String hash = PasswordUtil.hashPassword(plain, salt);

        assertTrue(PasswordUtil.verifyPassword(plain, salt, hash), "Correct password must verify to true");
        assertFalse(PasswordUtil.verifyPassword("WrongPassword", salt, hash), "Incorrect password must verify to false");
        assertFalse(PasswordUtil.verifyPassword(plain, "different_salt", hash), "Wrong salt must fail verification");
        assertFalse(PasswordUtil.verifyPassword(null, salt, hash));
    }
}
