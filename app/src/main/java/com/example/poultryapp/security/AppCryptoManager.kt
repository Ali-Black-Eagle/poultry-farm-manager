package com.example.poultryapp.security

import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * مدیر رمزنگاری و امنیت پیشرفته داده‌های پشتیبان با استاندارد سازمانی AES-256-GCM
 * 
 * ویژگی‌ها:
 * ۱. محرمانگی کامل با کلید ۲۵۶ بیتی (Confidentiality)
 * ۲. احراز اصالت و ضد دستکاری داده‌ها با تگ AEAD ۱۲۸ بیتی (Integrity & Anti-Tamper)
 * ۳. بردار مقداردهی اولیه (IV) ۱۲ بایتی تصادفی و غیرتکراری برای هر عملیات بکاپ
 * ۴. مشتق‌سازی ایمن کلید با الگوریتم استاندارد PBKDF2WithHmacSHA256 و ۶۰۰,۰۰۰ دور
 * ۵. پشتیبانی دوطرفه از کلمه عبور اختصاصی کاربر و رمزگشایی فایل‌های قدیمی
 */
object AppCryptoManager {

    private const val ALGORITHM_CIPHER = "AES/GCM/NoPadding"
    private const val ALGORITHM_KEY_FACTORY = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH_BITS = 256
    private const val ITERATION_COUNT_SECURE = 600000
    private const val ITERATION_COUNT_LEGACY = 10000
    private const val PIN_ITERATION_COUNT = 100000
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val SALT_LENGTH_BYTES = 16

    // کلید ارشد سامانه (جهت سازگاری با بکاپ‌های بدون پسورد کاربر یا ساختارهای قبلی)
    private val MASTER_APPLICATION_SEED = charArrayOf(
        'P', 'o', 'u', 'l', 't', 'r', 'y', 'S', 'm', 'a', 'r', 't', 'F', 'a', 'r', 'm',
        'G', 'o', 'l', 'd', 'e', 'n', 'B', 'r', 'e', 'e', 'd', 'i', 'n', 'g', '2', '0', '2', '6',
        'S', 'e', 'c', 'u', 'r', 'e', 'V', 'a', 'u', 'l', 't', 'K', 'e', 'y'
    )

    data class EncryptedResult(
        val ciphertextBase64: String,
        val ivBase64: String,
        val saltBase64: String,
        val iterations: Int = ITERATION_COUNT_SECURE
    )

    fun generateSalt(lengthBytes: Int = SALT_LENGTH_BYTES): ByteArray {
        val salt = ByteArray(lengthBytes)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * رمزنگاری داده‌های متنی با استاندارد AES-256-GCM و مشتق‌سازی ۶۰۰,۰۰۰ دور PBKDF2
     */
    fun encrypt(plainText: String, userPassword: CharArray? = null): EncryptedResult {
        val secureRandom = SecureRandom()
        val salt = generateSalt()

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val pass = if (userPassword != null && userPassword.isNotEmpty()) userPassword else MASTER_APPLICATION_SEED
        val secretKey = deriveKey(pass, salt, ITERATION_COUNT_SECURE)

        val cipher = Cipher.getInstance(ALGORITHM_CIPHER)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return EncryptedResult(
            ciphertextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            iterations = ITERATION_COUNT_SECURE
        )
    }

    /**
     * رمزگشایی هوشمند با پشتیبانی از کلمه عبور کاربر و بازیابی فایل‌های قدیمی
     */
    @Throws(Exception::class)
    fun decrypt(
        ciphertextBase64: String,
        ivBase64: String,
        saltBase64: String,
        userPassword: CharArray? = null,
        iterations: Int? = null
    ): String {
        val cipherBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(ALGORITHM_CIPHER)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)

        val candidatePasswords = mutableListOf<CharArray>()
        if (userPassword != null && userPassword.isNotEmpty()) {
            candidatePasswords.add(userPassword)
        }
        candidatePasswords.add(MASTER_APPLICATION_SEED)

        val iterList = if (iterations != null && iterations > 0) {
            listOf(iterations, ITERATION_COUNT_SECURE, ITERATION_COUNT_LEGACY)
        } else {
            listOf(ITERATION_COUNT_SECURE, ITERATION_COUNT_LEGACY)
        }

        var lastException: Exception? = null
        for (pass in candidatePasswords) {
            for (iter in iterList) {
                try {
                    val secretKey = deriveKey(pass, salt, iter)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
                    val decryptedBytes = cipher.doFinal(cipherBytes)
                    return String(decryptedBytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    lastException = e
                }
            }
        }

        throw lastException ?: SecurityException("رمزگشایی با کلمه عبور وارد شده ناموفق بود.")
    }

    /**
     * رمزگشایی فایل‌های قدیمی نسخه ۱
     */
    fun decryptLegacy(ciphertextBase64: String, ivBase64: String, saltBase64: String): String {
        return decrypt(ciphertextBase64, ivBase64, saltBase64, MASTER_APPLICATION_SEED, ITERATION_COUNT_LEGACY)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        val keySpec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM_KEY_FACTORY)
        val keyBytes = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    // ==========================================
    // امنیت PIN و احراز هویت با سالت و نرخ محدود
    // ==========================================

    fun hashPinWithSalt(pin: String, salt: ByteArray): String {
        if (pin.isBlank()) return ""
        val keySpec = PBEKeySpec(pin.trim().toCharArray(), salt, PIN_ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM_KEY_FACTORY)
        val hash = factory.generateSecret(keySpec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String): String {
        if (pin.isBlank()) return ""
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.trim().toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String, storedHash: String, storedSaltHex: String?): Boolean {
        if (storedHash.isBlank()) return true
        if (storedSaltHex.isNullOrBlank()) {
            return hashPin(enteredPin) == storedHash
        }
        val salt = try {
            storedSaltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            return hashPin(enteredPin) == storedHash
        }
        return hashPinWithSalt(enteredPin, salt) == storedHash
    }

    private const val PREF_PIN_FAILED_ATTEMPTS = "auth_pin_failed_attempts"
    private const val PREF_PIN_LOCKOUT_UNTIL = "auth_pin_lockout_until"

    fun recordFailedPinAttempt(prefs: SharedPreferences) {
        val attempts = prefs.getInt(PREF_PIN_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(PREF_PIN_FAILED_ATTEMPTS, attempts)
        if (attempts >= 5) {
            val backoffSeconds = when {
                attempts >= 10 -> 300L // 5 minutes
                attempts >= 7 -> 60L  // 1 minute
                else -> 30L           // 30 seconds
            }
            editor.putLong(PREF_PIN_LOCKOUT_UNTIL, System.currentTimeMillis() + backoffSeconds * 1000)
        }
        editor.apply()
    }

    fun resetPinAttempts(prefs: SharedPreferences) {
        prefs.edit()
            .remove(PREF_PIN_FAILED_ATTEMPTS)
            .remove(PREF_PIN_LOCKOUT_UNTIL)
            .apply()
    }

    fun getPinLockoutRemainingSeconds(prefs: SharedPreferences): Long {
        val lockoutUntil = prefs.getLong(PREF_PIN_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) (lockoutUntil - now) / 1000 else 0L
    }

    fun generateRecoveryKey(): String {
        val alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val random = SecureRandom()
        val part1 = (1..4).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        val part2 = (1..4).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        return "PF-$part1-$part2"
    }
}
