package com.antitheft.app.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object CryptoUtils {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    private const val ITERATIONS = 1000
    private const val KEY_LENGTH = 256

    fun encrypt(data: String): String {
        return try {
            val key = generateKey(Constants.ENCRYPTION_KEY)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(data.toByteArray())
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) { data }
    }

    fun decrypt(encryptedData: String): String {
        return try {
            val key = generateKey(Constants.ENCRYPTION_KEY)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decrypted = cipher.doFinal(Base64.decode(encryptedData, Base64.NO_WRAP))
            String(decrypted)
        } catch (e: Exception) { encryptedData }
    }

    private fun generateKey(password: String): SecretKey {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, ALGORITHM)
    }
}
