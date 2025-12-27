package com.example.oudeika.util


import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object PasswordHasher {

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((password + salt).toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun verify(
        inputPassword: String,
        storedHash: String,
        storedSalt: String
    ): Boolean {
        val inputHash = hash(inputPassword, storedSalt)
        return inputHash == storedHash
    }
}
