package com.example.poultryapp.data.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.poultryapp.data.local.AppDatabaseHelper
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.security.AppCryptoManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupSummary(
    val appVersion: String,
    val backupDateJalali: String,
    val backupTimestampMillis: Long,
    val flockCount: Int,
    val totalBirdsCount: Int,
    val warehouseItemsCount: Int,
    val financialTxCount: Int,
    val eggRecordsCount: Int,
    val rationPlansCount: Int,
    val totalGameStars: Int,
    val unlockedBirdsCount: Int,
    val proCompletedStagesCount: Int,
    val isEncrypted: Boolean = true
)

/**
 * مدیر پشتیبان‌گیری و بازیابی یکپارچه دیتابیس فارم و پیشرفت بازی در گوگل درایو و حافظه ابری
 */
object FullAppBackupManager {
    private const val BACKUP_FORMAT_VERSION = 2
    private const val PREFS_GAME_NAME = "golden_breeding_academy_prefs"
    private const val PREFS_GAME_LEGACY = "poultry_breeding_game_prefs"

    private const val ENCRYPTED_FORMAT_IDENTIFIER = "POULTRY_ENCRYPTED_BACKUP_V1"

    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "3.2.1"
        } catch (e: Exception) {
            "3.2.1"
        }
    }

    /**
     * تولید شیء خام JSON شامل داده‌های کامل دیتابیس SQLite و پیشرفت بازی
     */
    fun createRawBackupJson(context: Context): JSONObject {
        val root = JSONObject()
        val dbHelper = AppDatabaseHelper.getInstance(context)

        // ۱. متادیتای اختصاصی بکاپ
        val meta = JSONObject().apply {
            put("backupFormatVersion", BACKUP_FORMAT_VERSION)
            put("timestampMillis", System.currentTimeMillis())
            put("jalaliDateStr", JalaliDateHelper.getJalaliDate().toString())
            put("appVersion", getAppVersion(context))
            put("appName", "سامانه هوشمند پرورش طیور و آکادمی بازی طلایی")
        }
        root.put("meta", meta)

        // ۲. خروجی کامل دیتابیس SQLite (۱۰ جدول فارم)
        val sqliteJsonStr = dbHelper.exportDatabaseToJson()
        root.put("sqliteDatabase", JSONObject(sqliteJsonStr))

        // ۳. ذخیره تمام کلیدهای بازی آکادمی طلایی (فصل ۱ مبتدی، فصل ۲ فوق‌حرفه‌ای، واژه‌نامه، مدال‌ها و ستاره‌ها)
        val gamePrefs = context.getSharedPreferences(PREFS_GAME_NAME, Context.MODE_PRIVATE)
        val gamePrefsObj = JSONObject()
        for ((key, value) in gamePrefs.all) {
            when (value) {
                is Boolean -> gamePrefsObj.put(key, value)
                is Int -> gamePrefsObj.put(key, value)
                is Long -> gamePrefsObj.put(key, value)
                is Float -> gamePrefsObj.put(key, value.toDouble())
                is String -> gamePrefsObj.put(key, value)
                is Set<*> -> {
                    val arr = JSONArray()
                    value.forEach { item -> if (item != null) arr.put(item.toString()) }
                    gamePrefsObj.put(key, arr)
                }
            }
        }
        root.put("goldenBreedingPrefs", gamePrefsObj)

        // ۴. تنظیمات مینی‌گیم‌های جانبی
        val legacyPrefs = context.getSharedPreferences(PREFS_GAME_LEGACY, Context.MODE_PRIVATE)
        val legacyPrefsObj = JSONObject()
        for ((key, value) in legacyPrefs.all) {
            when (value) {
                is Boolean -> legacyPrefsObj.put(key, value)
                is Int -> legacyPrefsObj.put(key, value)
                is Long -> legacyPrefsObj.put(key, value)
                is Float -> legacyPrefsObj.put(key, value.toDouble())
                is String -> legacyPrefsObj.put(key, value)
            }
        }
        root.put("legacyGamePrefs", legacyPrefsObj)

        return root
    }

    /**
     * تولید فایل پشتیبان جامع رمزنگاری‌شده با استاندارد نظامی AES-256-GCM
     * کل اطلاعات فارم و پیشرفت بازی به صورت کاملاً غیرقابل خواندن کدگذاری می‌شود.
     */
    fun createFullBackupJson(context: Context, userPassword: String? = null): String {
        val rawRoot = createRawBackupJson(context)
        val plainText = rawRoot.toString()

        // رمزنگاری محتوای جامع با کلید ارشد PBKDF2 و نمک تصادفی (و رمز عبور اختیاری کاربر)
        val encResult = AppCryptoManager.encrypt(plainText, userPassword?.toCharArray())

        val envelope = JSONObject().apply {
            put("format", ENCRYPTED_FORMAT_IDENTIFIER)
            put("version", 1)
            put("algorithm", "AES-256-GCM")
            put("salt", encResult.saltBase64)
            put("iv", encResult.ivBase64)
            put("ciphertext", encResult.ciphertextBase64)

            // متادیتای غیرمحرمانه برای نمایش کلی در سیستم‌عامل
            put("meta", JSONObject().apply {
                put("backupFormatVersion", BACKUP_FORMAT_VERSION)
                put("timestampMillis", System.currentTimeMillis())
                put("jalaliDateStr", JalaliDateHelper.getJalaliDate().toString())
                put("appVersion", getAppVersion(context))
                put("appName", "سامانه هوشمند پرورش طیور و آکادمی بازی طلایی")
                put("isEncrypted", true)
                put("hasUserPassword", !userPassword.isNullOrBlank())
            })
        }

        return envelope.toString(2)
    }

    /**
     * استخراج و رمزگشایی هوشمند بسته داده‌ها (پشتیبانی از فرمت جدید رمزنگاری‌شده و نسخه‌های قدیمی متنی)
     */
    @Throws(Exception::class)
    fun extractDecryptedPayload(jsonStr: String, userPassword: String? = null): Pair<JSONObject, Boolean> {
        val root = JSONObject(jsonStr)
        if (root.optString("format") == ENCRYPTED_FORMAT_IDENTIFIER && root.has("ciphertext")) {
            val ciphertext = root.getString("ciphertext")
            val iv = root.getString("iv")
            val salt = root.getString("salt")
            val decryptedPlain = AppCryptoManager.decrypt(ciphertext, iv, salt, userPassword?.toCharArray())
            return Pair(JSONObject(decryptedPlain), true)
        }
        // بکاپ‌های قدیمی بدون رمزنگاری
        return Pair(root, false)
    }

    /**
     * بازبینی و استخراج خلاصه داده‌ها قبل از اعمال بازیابی به کاربر
     */
    fun inspectBackup(jsonStr: String, userPassword: String? = null): BackupSummary? {
        return try {
            val (root, isEncrypted) = extractDecryptedPayload(jsonStr, userPassword)
            val meta = root.optJSONObject("meta")
            val dateStr = meta?.optString("jalaliDateStr", "نامشخص") ?: "نامشخص"
            val ts = meta?.optLong("timestampMillis", System.currentTimeMillis()) ?: System.currentTimeMillis()
            val ver = meta?.optString("appVersion", "3.0") ?: "3.0"

            val sqlite = root.optJSONObject("sqliteDatabase") ?: root
            val flocksArr = sqlite.optJSONArray("flocks")
            val flockCount = flocksArr?.length() ?: 0
            var totalBirds = 0
            if (flocksArr != null) {
                for (i in 0 until flocksArr.length()) {
                    totalBirds += flocksArr.getJSONObject(i).optInt("birdCount", 0)
                }
            }
            val whCount = sqlite.optJSONArray("warehouseItems")?.length() ?: 0
            val finCount = sqlite.optJSONArray("financialTransactions")?.length() ?: 0
            val eggCount = sqlite.optJSONArray("eggProductions")?.length() ?: 0
            val ratCount = sqlite.optJSONArray("rationPlans")?.length() ?: 0
            val incCount = sqlite.optJSONArray("incubationBatches")?.length() ?: 0
            val weightCount = sqlite.optJSONArray("flockWeightSamples")?.length() ?: 0

            val game = root.optJSONObject("goldenBreedingPrefs")
            val totalStars = game?.optInt("pref_total_stars", 0) ?: 0
            var unlockedBirds = 0
            var proCompleted = 0
            if (game != null) {
                val keys = game.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k.startsWith("bird_unlocked_") && game.optBoolean(k, false)) {
                        unlockedBirds++
                    }
                    if (k.startsWith("stage_passed_pro_") && game.optBoolean(k, false)) {
                        proCompleted++
                    }
                }
            }

            BackupSummary(
                appVersion = ver,
                backupDateJalali = dateStr,
                backupTimestampMillis = ts,
                flockCount = flockCount,
                totalBirdsCount = totalBirds,
                warehouseItemsCount = whCount,
                financialTxCount = finCount,
                eggRecordsCount = eggCount,
                rationPlansCount = ratCount,
                totalGameStars = totalStars,
                unlockedBirdsCount = unlockedBirds.coerceAtLeast(1),
                proCompletedStagesCount = proCompleted,
                isEncrypted = isEncrypted
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * بازیابی یکپارچه و اتمیک کل سیستم مدیریت فارم و آکادمی بازی با قابلیت رول‌بک در صورت شکست
     */
    fun restoreFullBackup(context: Context, jsonStr: String, userPassword: String? = null): Boolean {
        val dbHelper = AppDatabaseHelper.getInstance(context)
        val gamePrefs = context.getSharedPreferences(PREFS_GAME_NAME, Context.MODE_PRIVATE)
        val legacyPrefs = context.getSharedPreferences(PREFS_GAME_LEGACY, Context.MODE_PRIVATE)

        // تهیه نسخه پشتیبان سریع قبل از بازیابی برای تضمین اتمیک بودن (Rollback Snapshot)
        val snapshotDbJson = try { dbHelper.exportDatabaseToJson() } catch (e: Exception) { null }
        val snapshotGamePrefs = HashMap(gamePrefs.all)
        val snapshotLegacyPrefs = HashMap(legacyPrefs.all)

        fun rollback() {
            try {
                if (snapshotDbJson != null) {
                    dbHelper.importDatabaseFromJson(snapshotDbJson)
                }
                restorePrefsMap(gamePrefs, snapshotGamePrefs)
                restorePrefsMap(legacyPrefs, snapshotLegacyPrefs)
            } catch (re: Exception) {
                re.printStackTrace()
            }
        }

        return try {
            val (root, _) = extractDecryptedPayload(jsonStr, userPassword)

            // ۱. بازیابی کامل دیتابیس SQLite
            val sqliteObj = if (root.has("sqliteDatabase")) {
                root.getJSONObject("sqliteDatabase")
            } else {
                root
            }
            val dbSuccess = dbHelper.importDatabaseFromJson(sqliteObj.toString())
            if (!dbSuccess) {
                rollback()
                return false
            }

            // ۲. بازیابی پیشرفت بازی و بورد حرفه‌ای
            if (root.has("goldenBreedingPrefs")) {
                val gameObj = root.getJSONObject("goldenBreedingPrefs")
                val editor = gamePrefs.edit()
                editor.clear()
                val keys = gameObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = gameObj.get(k)
                    when (v) {
                        is Boolean -> editor.putBoolean(k, v)
                        is Int -> editor.putInt(k, v)
                        is Long -> editor.putLong(k, v)
                        is Double -> editor.putFloat(k, v.toFloat())
                        is String -> editor.putString(k, v)
                        is JSONArray -> {
                            val set = mutableSetOf<String>()
                            for (i in 0 until v.length()) {
                                set.add(v.getString(i))
                            }
                            editor.putStringSet(k, set)
                        }
                    }
                }
                if (!editor.commit()) {
                    rollback()
                    return false
                }
            }

            // ۳. بازیابی تنظیمات مینی‌گیم‌های قبلی
            if (root.has("legacyGamePrefs")) {
                val legObj = root.getJSONObject("legacyGamePrefs")
                val editor = legacyPrefs.edit()
                editor.clear()
                val keys = legObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = legObj.get(k)
                    when (v) {
                        is Boolean -> editor.putBoolean(k, v)
                        is Int -> editor.putInt(k, v)
                        is Long -> editor.putLong(k, v)
                        is Double -> editor.putFloat(k, v.toFloat())
                        is String -> editor.putString(k, v)
                    }
                }
                if (!editor.commit()) {
                    rollback()
                    return false
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            rollback()
            false
        }
    }

    private fun restorePrefsMap(prefs: android.content.SharedPreferences, map: Map<String, *>) {
        val editor = prefs.edit()
        editor.clear()
        for ((key, value) in map) {
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    editor.putStringSet(key, value as? Set<String>)
                }
            }
        }
        editor.commit()
    }

    /**
     * ادغام هوشمند داده‌های فارم از دستگاه دیگر یا پشتیبان ابری بدون حذف داده‌های محلی (Feature 12)
     */
    fun mergeFullBackup(context: Context, jsonStr: String, userPassword: String? = null): Boolean {
        return try {
            val (root, _) = extractDecryptedPayload(jsonStr, userPassword)
            val sqliteObj = if (root.has("sqliteDatabase")) {
                root.getJSONObject("sqliteDatabase")
            } else {
                root
            }
            val dbHelper = AppDatabaseHelper.getInstance(context)
            dbHelper.mergeDatabaseFromJson(sqliteObj.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * ایجاد نام پیش‌فرض استاندارد برای فایل خروجی
     */
    fun generateDefaultFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        return "PoultryPro_FullBackup_${sdf.format(Date())}.json"
    }

    /**
     * ایجاد Intent برای ذخیره یا اشتراک مستقیم با Google Drive
     */
    fun createShareToDriveIntent(context: Context, backupJson: String): Intent {
        val fileName = generateDefaultFileName()
        val backupsDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        val cacheFile = File(backupsDir, fileName)
        cacheFile.writeText(backupJson, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "پشتیبان کامل سامانه پرورش طیور و آکادمی بازی")
            putExtra(Intent.EXTRA_TEXT, "فایل پشتیبان ابری شامل تمامی گله‌ها، انبار، تراکنش‌ها، تخم‌گذاری و پیشرفت بازی طلایی")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * نوشتن محتوای بکاپ در خروجی Storage Access Framework (گوگل درایو یا حافظه انتخابی کاربر)
     */
    fun writeBackupToStream(backupJson: String, outputStream: OutputStream) {
        outputStream.use { os ->
            os.write(backupJson.toByteArray(Charsets.UTF_8))
            os.flush()
        }
    }

    /**
     * خواندن متن فایل پشتیبان از ورودی انتخابی کاربر از طریق SAF
     */
    fun readBackupFromStream(inputStream: InputStream): String {
        return inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
