package com.cred.users.user_service.utils

import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Component
class PasswordUtils {
    
    private val algorithm = "AES"
    private val transformation = "AES"
    
    // In production, this should be loaded from environment variables or secure configuration
    private val secretKey = "MySecretKey12345" // 16 bytes for AES-128
    
    fun encryptPassword(password: String): String {
        return try {
            val key = SecretKeySpec(secretKey.toByteArray(), algorithm)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(password.toByteArray())
            Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            throw RuntimeException("Error encrypting password", e)
        }
    }
    
    fun decryptPassword(encryptedPassword: String): String {
        return try {
            val key = SecretKeySpec(secretKey.toByteArray(), algorithm)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword))
            String(decryptedBytes)
        } catch (e: Exception) {
            throw RuntimeException("Error decrypting password", e)
        }
    }
    
    fun verifyPassword(plainPassword: String, encryptedPassword: String): Boolean {
        return try {
            val decryptedPassword = decryptPassword(encryptedPassword)
            plainPassword == decryptedPassword
        } catch (e: Exception) {
            false
        }
    }
}
