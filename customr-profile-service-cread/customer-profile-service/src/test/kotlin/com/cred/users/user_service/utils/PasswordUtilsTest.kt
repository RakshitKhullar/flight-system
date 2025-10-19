package com.cred.users.user_service.utils

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PasswordUtilsTest {

    private lateinit var passwordUtils: PasswordUtils

    @BeforeEach
    fun setUp() {
        passwordUtils = PasswordUtils()
    }

    @Test
    fun `encryptPassword should encrypt password successfully`() {
        // Given
        val plainPassword = "mySecretPassword123"

        // When
        val encryptedPassword = passwordUtils.encryptPassword(plainPassword)

        // Then
        assertNotNull(encryptedPassword)
        assertNotEquals(plainPassword, encryptedPassword)
        assertTrue(encryptedPassword.isNotEmpty())
        assertTrue(encryptedPassword.length > plainPassword.length) // Base64 encoding makes it longer
    }

    @Test
    fun `encryptPassword should produce same results for same input on different calls`() {
        // Given
        val plainPassword = "mySecretPassword123"

        // When
        val encrypted1 = passwordUtils.encryptPassword(plainPassword)
        val encrypted2 = passwordUtils.encryptPassword(plainPassword)

        // Then
        assertEquals(encrypted1, encrypted2) // Deterministic encryption should produce same result
    }

    @Test
    fun `verifyPassword should return true for correct password`() {
        // Given
        val plainPassword = "mySecretPassword123"
        val encryptedPassword = passwordUtils.encryptPassword(plainPassword)

        // When
        val isValid = passwordUtils.verifyPassword(plainPassword, encryptedPassword)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `verifyPassword should return false for incorrect password`() {
        // Given
        val plainPassword = "mySecretPassword123"
        val wrongPassword = "wrongPassword456"
        val encryptedPassword = passwordUtils.encryptPassword(plainPassword)

        // When
        val isValid = passwordUtils.verifyPassword(wrongPassword, encryptedPassword)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `verifyPassword should handle empty passwords correctly`() {
        // Given
        val emptyPassword = ""
        val encryptedPassword = passwordUtils.encryptPassword(emptyPassword)

        // When
        val isValid = passwordUtils.verifyPassword(emptyPassword, encryptedPassword)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `verifyPassword should return false for empty encrypted password`() {
        // Given
        val plainPassword = "mySecretPassword123"
        val emptyEncrypted = ""

        // When
        val isValid = passwordUtils.verifyPassword(plainPassword, emptyEncrypted)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `verifyPassword should handle special characters in password`() {
        // Given
        val specialPassword = "p@ssw0rd!@#$%^&*()_+-=[]{}|;:,.<>?"
        val encryptedPassword = passwordUtils.encryptPassword(specialPassword)

        // When
        val isValid = passwordUtils.verifyPassword(specialPassword, encryptedPassword)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `verifyPassword should handle unicode characters in password`() {
        // Given
        val unicodePassword = "пароль密码パスワード🔐"
        val encryptedPassword = passwordUtils.encryptPassword(unicodePassword)

        // When
        val isValid = passwordUtils.verifyPassword(unicodePassword, encryptedPassword)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `verifyPassword should handle very long passwords`() {
        // Given
        val longPassword = "a".repeat(1000) // 1000 character password
        val encryptedPassword = passwordUtils.encryptPassword(longPassword)

        // When
        val isValid = passwordUtils.verifyPassword(longPassword, encryptedPassword)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `verifyPassword should handle invalid inputs gracefully`() {
        // Given
        val validPassword = "password123"
        val encryptedPassword = passwordUtils.encryptPassword(validPassword)

        // When & Then - Invalid encrypted password should return false
        val result1 = passwordUtils.verifyPassword(validPassword, "invalidEncryptedString")
        assertFalse(result1)

        val result2 = passwordUtils.verifyPassword("wrongPassword", encryptedPassword)
        assertFalse(result2)

        val result3 = passwordUtils.verifyPassword("anyString", "anyString")
        assertFalse(result3)
    }

    @Test
    fun `verifyPassword should return false for malformed encrypted password`() {
        // Given
        val plainPassword = "mySecretPassword123"
        val malformedEncrypted = "this-is-not-a-valid-encrypted-password"

        // When
        val isValid = passwordUtils.verifyPassword(plainPassword, malformedEncrypted)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `encryption and decryption should be consistent across multiple operations`() {
        // Given
        val passwords = listOf(
            "simple",
            "complex!@#$%",
            "unicode密码",
            "very-long-password-with-many-characters-to-test-consistency",
            "123456789",
            "MixedCASE123!@#"
        )

        // When & Then
        passwords.forEach { password ->
            val encrypted = passwordUtils.encryptPassword(password)
            val isValid = passwordUtils.verifyPassword(password, encrypted)
            assertTrue(isValid, "Failed for password: $password")
            
            // Verify wrong password fails
            val wrongPassword = password + "wrong"
            val isInvalid = passwordUtils.verifyPassword(wrongPassword, encrypted)
            assertFalse(isInvalid, "Should have failed for wrong password: $wrongPassword")
        }
    }

    @Test
    fun `encrypted passwords should be same for same input`() {
        // Given
        val password = "testPassword123"
        val iterations = 10

        // When
        val encryptedPasswords = (1..iterations).map { 
            passwordUtils.encryptPassword(password) 
        }

        // Then
        // All encrypted passwords should be the same due to deterministic encryption
        val uniquePasswords = encryptedPasswords.toSet()
        assertEquals(1, uniquePasswords.size, "All encrypted passwords should be identical")

        // And all should verify correctly
        encryptedPasswords.forEach { encrypted ->
            assertTrue(passwordUtils.verifyPassword(password, encrypted))
        }
    }
}
