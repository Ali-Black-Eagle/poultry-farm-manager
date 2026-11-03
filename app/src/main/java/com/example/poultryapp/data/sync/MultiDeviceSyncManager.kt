package com.example.poultryapp.data.sync

import android.content.Context
import com.example.poultryapp.data.backup.FullAppBackupManager
import com.example.poultryapp.data.local.AppDatabaseHelper
import com.example.poultryapp.security.AppCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Base64

object MultiDeviceSyncManager {

    data class SyncResult(
        val success: Boolean,
        val mergedFlocksCount: Int = 0,
        val mergedTransactionsCount: Int = 0,
        val mergedWarehouseItemsCount: Int = 0,
        val message: String
    )

    /**
     * صدور بسته همگام‌سازی فشرده و امن جهت انتقال به دستگاه دیگر یا گوگل درایو.
     */
    suspend fun createSecureSyncPackage(context: Context, syncSecretKey: String? = null): String = withContext(Dispatchers.IO) {
        val rawJson = FullAppBackupManager.createFullBackupJson(context)
        val passwordToUse = syncSecretKey?.toCharArray()
        val encResult = AppCryptoManager.encrypt(rawJson, passwordToUse)
        val packageObj = JSONObject().apply {
            put("version", 3)
            put("syncTimestampMillis", System.currentTimeMillis())
            put("deviceFingerprint", android.os.Build.MODEL)
            put("ciphertext", encResult.ciphertextBase64)
            put("iv", encResult.ivBase64)
            put("salt", encResult.saltBase64)
            put("iterations", encResult.iterations)
            put("checksum", AppCryptoManager.hashPin(encResult.ciphertextBase64).take(32))
        }
        packageObj.toString(2)
    }

    /**
     * دریافت و ادغام بسته همگام‌سازی با استراتژی Last-Write-Wins (LWW) بدون حذف داده‌های دیگر.
     */
    suspend fun mergeSecureSyncPackage(context: Context, syncPackageStr: String, syncSecretKey: String? = null): SyncResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(syncPackageStr)
            val rawJson: String

            if (root.has("ciphertext") && root.has("iv") && root.has("salt")) {
                val ciphertext = root.getString("ciphertext")
                val iv = root.getString("iv")
                val salt = root.getString("salt")
                val iterations = root.optInt("iterations", 600000)
                val expectedChecksum = root.optString("checksum")
                val actualChecksum = AppCryptoManager.hashPin(ciphertext).take(32)

                if (expectedChecksum.isBlank() || expectedChecksum != actualChecksum) {
                    return@withContext SyncResult(
                        success = false,
                        message = "خطا در اعتبارسنجی: بسته همگام‌سازی مخدوش شده است یا کلید تطابق ندارد."
                    )
                }
                rawJson = AppCryptoManager.decrypt(ciphertext, iv, salt, syncSecretKey?.toCharArray(), iterations)
            } else if (root.has("encryptedPayload")) {
                // سازگاری با فرمت قدیمی
                val base64Payload = root.getString("encryptedPayload")
                val rawJsonBytes = Base64.getDecoder().decode(base64Payload)
                rawJson = String(rawJsonBytes, Charsets.UTF_8)
            } else {
                return@withContext SyncResult(
                    success = false,
                    message = "ساختار بسته همگام‌سازی نامعتبر است."
                )
            }

            // ادغام هوشمند در دیتابیس بدون رونویسی مخرب داده‌های محلی (Smart Upsert)
            val dbHelper = AppDatabaseHelper.getInstance(context)
            val flocksBefore = dbHelper.getAllFlocks().size
            val txBefore = dbHelper.getAllFinancialTransactions().size
            val itemsBefore = dbHelper.getAllWarehouseItems().size

            val mergeSuccess = FullAppBackupManager.mergeFullBackup(context, rawJson)
            if (mergeSuccess) {
                val flocksAfter = dbHelper.getAllFlocks().size
                val txAfter = dbHelper.getAllFinancialTransactions().size
                val itemsAfter = dbHelper.getAllWarehouseItems().size

                SyncResult(
                    success = true,
                    mergedFlocksCount = flocksAfter,
                    mergedTransactionsCount = txAfter,
                    mergedWarehouseItemsCount = itemsAfter,
                    message = "همگام‌سازی هوشمند با موفقیت انجام شد ($flocksAfter گله و $itemsAfter قلم کالا تثبیت شدند)."
                )
            } else {
                SyncResult(
                    success = false,
                    message = "خطا در تحلیل و ادغام جداول دیتابیس."
                )
            }
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "خطا در خواندن فایل همگام‌سازی: ${e.localizedMessage ?: "فرمت نامعتبر"}"
            )
        }
    }
}
