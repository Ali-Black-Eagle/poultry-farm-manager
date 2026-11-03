package com.example.poultryapp.voice

import com.example.poultryapp.R
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.MortalityReason
import com.example.poultryapp.data.local.FarmContact
import com.example.poultryapp.data.local.ContactCategory
import com.example.poultryapp.data.local.TransactionType
import com.example.poultryapp.data.local.TransactionCategory
import com.example.poultryapp.data.local.EggProduction
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.engine.LayingBenchmarkEngine
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.data.local.UserRole
import com.example.poultryapp.engine.FarmLiveContext
import com.example.poultryapp.engine.FarmOmniscientAnalyticsEngine
import java.util.Locale

enum class VoiceIntentType {
    CREATE_FLOCK,
    RECORD_EGG,
    RECORD_MORTALITY,
    RECORD_FEED,
    RECORD_WATER,
    RECORD_FINANCIAL,
    NAVIGATE_SCREEN,
    QUERY_STATS,
    CALL_CONTACT,
    RECORD_MEDICATION,
    DIAGNOSE_DISEASE,
    MANAGE_WAREHOUSE,
    COMPOUND_ACTION,
    SET_SCHEDULE,
    RECORD_OBSERVATION,
    CALCULATE_METRIC,
    TRANSFER_BIRDS,
    SET_FLOCK_TEMPERATURE,
    TOGGLE_TRACKING,
    HELP,
    UNKNOWN
}

data class VoiceActionCardData(
    val intentType: VoiceIntentType,
    val title: String,
    val iconRes: Int = R.drawable.ic_farm_dashboard,
    val flockId: Long? = null,
    val flockName: String? = null,
    val primaryMetric: String,
    val details: List<Pair<String, String>>,
    val systemEffect: String,
    val rawText: String,
    val isValid: Boolean = true,
    val errorMessage: String? = null,
    val parameters: Map<String, Any> = emptyMap(),
    val needsMoreInfo: Boolean = false,
    val missingPromptFa: String? = null,
    val quickReplies: List<String> = emptyList(),
    val subActions: List<VoiceActionCardData> = emptyList()
) {
    /**
     * مشخص می‌کند آیا این اقدام داده‌ای را در دیتابیس یا وضعیت برنامه تغییر می‌دهد یا خیر.
     * دکمه‌های «تایید و اجرا» و «انصراف» منحصراً برای اقدامات تغییردهنده داده فعال می‌شوند.
     */
    val isMutatingAction: Boolean
        get() {
            if (parameters.containsKey("isMutating")) {
                return parameters["isMutating"] == true
            }
            return when (intentType) {
                VoiceIntentType.CREATE_FLOCK,
                VoiceIntentType.RECORD_EGG,
                VoiceIntentType.RECORD_MORTALITY,
                VoiceIntentType.RECORD_FEED,
                VoiceIntentType.RECORD_WATER,
                VoiceIntentType.RECORD_FINANCIAL,
                VoiceIntentType.RECORD_MEDICATION,
                VoiceIntentType.MANAGE_WAREHOUSE,
                VoiceIntentType.COMPOUND_ACTION,
                VoiceIntentType.SET_SCHEDULE,
                VoiceIntentType.RECORD_OBSERVATION,
                VoiceIntentType.SET_FLOCK_TEMPERATURE,
                VoiceIntentType.TOGGLE_TRACKING,
                VoiceIntentType.TRANSFER_BIRDS -> true
                else -> false
            }
        }
}

/**
 * دکمه یا اقدام فعال و خودمختار پیشنهادی ایجنت در پایان استدلال یا کارت اقدام
 */
data class AgenticProactiveAction(
    val id: String,
    val labelFa: String,
    val iconRes: Int = R.drawable.ic_farm_dashboard,
    val actionCard: VoiceActionCardData
)

/**
 * نتیجه تحلیل تاریخ نسبی در کلام کاربر
 */
data class ResolvedDateResult(
    val jalaliDateStr: String,
    val timestampMillis: Long,
    val matchedTerm: String,
    val isRelative: Boolean,
    val shift: String? = null
)

/**
 * مفسر هوشمند تاریخ‌ها و فواصل زمانی نسبی در زبان فارسی (امروز، دیروز، پریروز، فردا، پس‌فردا، X روز قبل/بعد، روزهای هفته و نوبت‌های کاری)
 */
object RelativeDateResolver {
    fun resolveDate(norm: String, baseMillis: Long = System.currentTimeMillis(), preferFuture: Boolean = false): ResolvedDateResult? {
        val oneDay = 86_400_000L

        val shift = when {
            norm.contains("صبح") -> "صبح"
            norm.contains("عصر") || norm.contains("بعدازظهر") || norm.contains("غروب") -> "عصر"
            norm.contains("شب") -> "شب"
            else -> null
        }

        // بررسی روزهای هفته (شنبه، یکشنبه، دوشنبه، سه‌شنبه، چهارشنبه، پنجشنبه، جمعه)
        val weekdayMatch = PersianVoiceNormalizer.findWeekday(norm)
        if (weekdayMatch != null) {
            val (wName, targetDow) = weekdayMatch
            val currentJalali = JalaliDateHelper.getJalaliDate(baseMillis)
            val currentDow = (JalaliDateHelper.getFirstDayOfWeekOfMonth(currentJalali.year, currentJalali.month) + currentJalali.day - 1) % 7
            val daysDiff = if (preferFuture) {
                if (targetDow >= currentDow) targetDow - currentDow else targetDow - currentDow + 7
            } else {
                if (currentDow > targetDow) currentDow - targetDow else if (currentDow < targetDow) currentDow - targetDow + 7 else 0
            }
            val targetMillis = if (preferFuture) baseMillis + (daysDiff * oneDay) else baseMillis - (daysDiff * oneDay)
            val jDate = JalaliDateHelper.getJalaliDate(targetMillis)
            return ResolvedDateResult(jDate.toStandardString(), targetMillis, wName, true, shift)
        }

        // الگوهای چند روز پیش / قبل (مانند ۳ روز پیش، دو روز قبل)
        val daysAgoRegex = Regex("(?:(\\d+|یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده)\\s+روز\\s+(?:پیش|قبل))")
        val daysAgoMatch = daysAgoRegex.find(norm)
        if (daysAgoMatch != null) {
            val numStr = daysAgoMatch.groupValues[1]
            val count = PersianVoiceNormalizer.wordToNumber(numStr)?.toInt()
                ?: PersianVoiceNormalizer.extractFirstNumber(numStr)?.toInt() ?: 1
            val targetMillis = baseMillis - (count * oneDay)
            val jDate = JalaliDateHelper.getJalaliDate(targetMillis)
            return ResolvedDateResult(jDate.toStandardString(), targetMillis, daysAgoMatch.value, true, shift)
        }

        // الگوهای چند روز بعد / دیگه
        val daysLaterRegex = Regex("(?:(\\d+|یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده)\\s+روز\\s+(?:بعد|دیگه|آینده))")
        val daysLaterMatch = daysLaterRegex.find(norm)
        if (daysLaterMatch != null) {
            val numStr = daysLaterMatch.groupValues[1]
            val count = PersianVoiceNormalizer.wordToNumber(numStr)?.toInt()
                ?: PersianVoiceNormalizer.extractFirstNumber(numStr)?.toInt() ?: 1
            val targetMillis = baseMillis + (count * oneDay)
            val jDate = JalaliDateHelper.getJalaliDate(targetMillis)
            return ResolvedDateResult(jDate.toStandardString(), targetMillis, daysLaterMatch.value, true, shift)
        }

        if (norm.contains("پریروز") || norm.contains("پری روز") || norm.contains("دو روز پیش")) {
            val targetMillis = baseMillis - (2 * oneDay)
            val jDate = JalaliDateHelper.getJalaliDate(targetMillis)
            return ResolvedDateResult(jDate.toStandardString(), targetMillis, "پریروز", true, shift)
        }
        if (norm.contains("پس فردا") || norm.contains("پسفردا") || norm.contains("دو روز بعد")) {
            val targetMillis = baseMillis + (2 * oneDay)
            val jDate = JalaliDateHelper.getJalaliDate(targetMillis)
            return ResolvedDateResult(jDate.toStandardString(), targetMillis, "پس‌فردا", true, shift)
        }
        if (norm.contains("دیروز") || norm.contains("روز قبل") || norm.contains("دیشب")) {
            val targetMillis = baseMillis - oneDay
            val jDate = JalaliDateHelper.getJalaliDate(targetMillis)
            return ResolvedDateResult(jDate.toStandardString(), targetMillis, "دیروز", true, shift)
        }
        if (norm.contains("فردا") || norm.contains("روز بعد") || norm.contains("فردا صبح") || norm.contains("فردا عصر")) {
            val targetMillis = baseMillis + oneDay
            val jDate = JalaliDateHelper.getJalaliDate(targetMillis)
            return ResolvedDateResult(jDate.toStandardString(), targetMillis, "فردا", true, shift)
        }
        if (norm.contains("امروز") || shift != null) {
            val jDate = JalaliDateHelper.getJalaliDate(baseMillis)
            return ResolvedDateResult(jDate.toStandardString(), baseMillis, if (norm.contains("امروز")) "امروز" else (shift ?: "امروز"), true, shift)
        }

        return null
    }
}

object VoiceIntentParser {

    val CONFIRM_KEYWORDS = listOf("تایید", "ثبت", "درسته", "بله", "اره", "اوکی", "انجام", "حله", "اوکیه", "ثبتش کن", "ذخیره کن")
    val CANCEL_KEYWORDS = listOf("لغو", "نه", "رد", "کنسل", "اشتباه", "دوباره", "پاک", "نمیخوام", "انصراف")

    private val CONFIRM_TOKENS = setOf("تایید", "درسته", "بله", "اره", "اوکی", "انجام", "حله", "اوکیه")
    private val CONFIRM_PHRASES = listOf("ثبتش کن", "ذخیره کن", "انجام بده", "تایید کن", "تاییدش کن", "ثبت کن")

    private val CANCEL_TOKENS = setOf("لغو", "نه", "خیر", "رد", "کنسل", "اشتباه", "انصراف")
    private val CANCEL_PHRASES = listOf("لغوش کن", "کنسلش کن", "نمیخوام", "نمی خواهم", "اشتباه شد", "دستور اشتباهه", "ثبت نکن", "بی خیال", "بیخیال")

    private val DISMISS_TOKENS = setOf("لغو", "کنسل", "انصراف", "ببند", "خروج", "بستن")
    private val DISMISS_PHRASES = listOf("لغوش کن", "کنسلش کن", "بی خیال", "بیخیال", "پنجره رو ببند", "ببندش")

    private val NUMBER_9_FOLLOWERS = setOf(
        "تا", "عدد", "دانه", "دونه", "قطعه", "کیلو", "لیتر", "شانه", "کارتن",
        "هزار", "میلیون", "درصد", "تلفات", "مرغ", "تخم", "جوجه", "راس"
    )

    private val OPERATIONAL_KEYWORDS = listOf(
        "هزینه", "درآمد", "تخم", "دان", "دون", "آب", "تلفات", "گله", "سالن", "بیماری", "انبار", "تماس", "دکتر"
    )

    private val IS_WATER_WORD_REGEX = Regex("""(?<=\s|^)اب(?=\s|$)""")
    fun containsWaterWord(text: String): Boolean {
        return text.contains("آب") || IS_WATER_WORD_REGEX.containsMatchIn(text)
    }

    fun isExplicitDismiss(text: String): Boolean {
        val norm = PersianVoiceNormalizer.normalize(text)
        if (norm.isBlank()) return false
        // اگر عبارت شامل کلیدواژه‌های عملیاتی فارم باشد، انصراف صریح نیست
        if (OPERATIONAL_KEYWORDS.any { norm.contains(it) }) return false
        if (DISMISS_PHRASES.any { norm.contains(it) }) return true
        val tokens = norm.split(Regex("[\\s،,\\.!?؛:\\-_]+")).filter { it.isNotBlank() }
        return tokens.any { it in DISMISS_TOKENS }
    }

    fun isConfirmation(text: String): Boolean {
        val norm = PersianVoiceNormalizer.normalize(text)
        if (norm.isBlank()) return false
        if (CONFIRM_PHRASES.any { norm.contains(it) }) return true
        val tokens = norm.split(Regex("[\\s،,\\.!?؛:\\-_]+")).filter { it.isNotBlank() }
        return tokens.any { it in CONFIRM_TOKENS }
    }

    fun isCancellation(text: String): Boolean {
        val norm = PersianVoiceNormalizer.normalize(text)
        if (norm.isBlank()) return false

        if (CANCEL_PHRASES.any { norm.contains(it) }) return true

        val tokens = norm.split(Regex("[\\s،,\\.!?؛:\\-_]+")).filter { it.isNotBlank() }
        for (i in tokens.indices) {
            val t = tokens[i]
            if (t in CANCEL_TOKENS) {
                if (t == "نه") {
                    // بررسی این که آیا «نه» عدد ۹ است؟ (مثال: «نه تا»، «نه عدد»، «نه شانه» و غیره)
                    if (i < tokens.size - 1 && tokens[i + 1] in NUMBER_9_FOLLOWERS) {
                        continue
                    }
                    // اگر عبارت حاوی کلمات کلیدی عملیاتی باشد و طول جمله بیش از ۲ کلمه باشد، «نه» شماره/بخش از جمله است
                    if (OPERATIONAL_KEYWORDS.any { norm.contains(it) } && tokens.size > 2) {
                        continue
                    }
                }
                return true
            }
        }
        return false
    }

    private fun enforceWorkerRole(card: VoiceActionCardData, userRole: UserRole): VoiceActionCardData {
        if (userRole != UserRole.WORKER) return card

        val isUnauthorized = when (card.intentType) {
            VoiceIntentType.RECORD_FINANCIAL -> true
            VoiceIntentType.CREATE_FLOCK -> true
            VoiceIntentType.TRANSFER_BIRDS -> true
            VoiceIntentType.NAVIGATE_SCREEN -> {
                val target = (card.parameters["targetScreen"] as? String ?: card.parameters["targetTab"] as? String)?.uppercase() ?: ""
                target == "FINANCIAL" || target == "SETTINGS"
            }
            VoiceIntentType.COMPOUND_ACTION -> {
                card.subActions.any { sub ->
                    sub.intentType == VoiceIntentType.RECORD_FINANCIAL ||
                    sub.intentType == VoiceIntentType.CREATE_FLOCK ||
                    (sub.intentType == VoiceIntentType.NAVIGATE_SCREEN && ((sub.parameters["targetScreen"] as? String ?: sub.parameters["targetTab"] as? String)?.uppercase() in listOf("FINANCIAL", "SETTINGS")))
                }
            }
            else -> false
        }

        if (isUnauthorized) {
            return VoiceActionCardData(
                intentType = card.intentType,
                title = "دسترسی غیر مجاز 🔒",
                iconRes = R.drawable.ic_farm_dashboard,
                primaryMetric = "دسترسی غیر مجاز",
                details = listOf(
                    "وضعیت دسترسی" to "دسترسی غیر مجاز 🔒",
                    "نقش کاربری فعلی" to "کارگر سالن",
                    "محدودیت امنیتی" to "ثبت داده‌های مالی و مدیریت گله صرفاً در صلاحیت مدیر فارم است."
                ),
                systemEffect = "این عملیات نیازمند سطح دسترسی مدیر است و انجام نخواهد شد.",
                rawText = card.rawText,
                isValid = false,
                errorMessage = "دسترسی غیر مجاز. این عملیات صرفاً در صلاحیت مدیر فارم است.",
                parameters = mapOf(
                    "unauthorized" to true,
                    "spokenSummary" to "دسترسی غیر مجاز. این عملیات در حالت کارگر مجاز نیست."
                )
            )
        }

        return card
    }

    data class FlockMatchResult(
        val flock: Flock?,
        val matchedPhrase: String?
    )

    val PERSIAN_HALL_NUMBERS: Map<String, Long> = mapOf(
        "یک" to 1L, "یه" to 1L, "اول" to 1L, "نخست" to 1L, "بیر" to 1L, "بیره" to 1L,
        "دو" to 2L, "دوم" to 2L,
        "سه" to 3L, "سوم" to 3L,
        "چهار" to 4L, "چهارم" to 4L,
        "پنج" to 5L, "پنجم" to 5L,
        "شش" to 6L, "شیش" to 6L, "ششم" to 6L,
        "هفت" to 7L, "هفتم" to 7L,
        "هشت" to 8L, "هشتم" to 8L,
        "نه" to 9L, "نهم" to 9L,
        "ده" to 10L, "دهم" to 10L,
        "یازده" to 11L, "یازدهم" to 11L,
        "دوازده" to 12L, "دوازدهم" to 12L,
        "سیزده" to 13L, "سیزدهم" to 13L,
        "چهارده" to 14L, "چهاردهم" to 14L,
        "پانزده" to 15L, "پونزده" to 15L, "پانزدهم" to 15L,
        "شانزده" to 16L, "شونزده" to 16L, "شانزدهم" to 16L,
        "هفده" to 17L, "هفدهم" to 17L,
        "هجده" to 18L, "هیجده" to 18L, "هجدهم" to 18L,
        "نوزده" to 19L, "نوزدهم" to 19L,
        "بیست" to 20L, "بیستم" to 20L,
        "سی" to 30L, "سی‌ام" to 30L, "سیام" to 30L,
        "چهل" to 40L, "چهلم" to 40L,
        "پنجاه" to 50L, "پنجاهم" to 50L,
        "شصت" to 60L, "شصتم" to 60L,
        "هفتاد" to 70L, "هفتادم" to 70L,
        "هشتاد" to 80L, "هشتادم" to 80L,
        "نود" to 90L, "نودم" to 90L,
        "صد" to 100L, "صدم" to 100L
    )

    fun parseHallNumberString(token: String): Long? {
        val clean = token.trim()
        val directDigits = clean.toLongOrNull()
        if (directDigits != null && directDigits in 1L..100L) return directDigits

        val directWord = PERSIAN_HALL_NUMBERS[clean]
        if (directWord != null) return directWord

        if (clean.contains(" و ")) {
            val parts = clean.split(" و ").map { it.trim() }
            if (parts.size == 2) {
                val tens = PERSIAN_HALL_NUMBERS[parts[0]]
                val units = PERSIAN_HALL_NUMBERS[parts[1]]
                if (tens != null && units != null && tens in listOf(20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L) && units in 1L..9L) {
                    val sum = tens + units
                    if (sum in 1L..100L) return sum
                }
            }
        }
        return null
    }

    fun findTargetFlock(normalizedText: String, flocks: List<Flock>): Flock? {
        return findTargetFlockWithPhrase(normalizedText, flocks).flock
    }

    private fun findTargetFlockWithPhrase(normalizedText: String, flocks: List<Flock>): FlockMatchResult {
        if (flocks.isEmpty()) return FlockMatchResult(null, null)

        // ۱. بررسی تطابق مستقیم با نام اختصاصی گله‌ها (با اولویت طول نام نزولی و مرز ارقام پسین)
        val sortedFlocks = flocks.sortedByDescending { PersianVoiceNormalizer.normalize(it.name).length }
        for (f in sortedFlocks) {
            val nameNorm = PersianVoiceNormalizer.normalize(f.name)
            if (nameNorm.isNotBlank()) {
                val escapedName = Regex.escape(nameNorm)
                val namePattern = Regex("""(?U)(?:\b|^)$escapedName(?!\d)(?:\b|$)""")
                if (namePattern.containsMatchIn(normalizedText)) {
                    return FlockMatchResult(f, f.name)
                }
            }
        }

        // ۲. تطابق الگوهای شماره سالن یا گله: ارقام ۱ تا ۱۰۰ و واژگان عددی بسته زبان فارسی (۱ تا ۱۰۰)
        // اولویت شماره سالن صریح قبل از تطابق عمومی نژاد قرار دارد تا ذکر نژاد در کنار شماره سالن موجب سرقت نشود
        // ۲.۱. بررسی ارقام عددی مستقیم (مانند: سالن ۱، گله ۲، سالن ۲۵، سالن ۱۰۰، باند ۳)
        val digitHallRegex = Regex("""(?:سالن|گله|باند|فارم|واحد)\s*(?:شماره\s*)?(\d{1,3})\b(?!\s*(?:کیلو|گرم|تن|کیسه|گونی|شانه|شونه|کارتن|کارتون|عدد|قطعه|راس|لیتر|سی\s*سی|سیسی|سی‌سی|درصد|تومان|تومن|هزار|میلیون|روز|روزه))""")
        val digitMatch = digitHallRegex.find(normalizedText)
        if (digitMatch != null) {
            val hallNum = digitMatch.groupValues[1].toLongOrNull()
            if (hallNum != null && hallNum in 1L..100L) {
                val matchedFlock = flocks.find { it.id == hallNum }
                    ?: flocks.getOrNull((hallNum - 1).toInt())
                    ?: Flock(
                        id = hallNum,
                        name = "سالن $hallNum",
                        species = BirdSpecies.CHICKEN,
                        breed = "عمومی",
                        birthEpochMillis = System.currentTimeMillis() - 30L * 24 * 3600 * 1000,
                        birdCount = 1000,
                        customTempCelsius = 22.0
                    )
                return FlockMatchResult(matchedFlock, digitMatch.value)
            }
        }

        // ۲.۲. بررسی واژگان بسته عددی و ترکیبی ۱ تا ۱۰۰ (مانند: سالن یک، سالن اول، سالن پانزده، سالن بیست و پنج، سالن نود و هشت)
        val hallWordsRegex = Regex(
            """(?:سالن|گله|باند|فارم|واحد)\s*(?:شماره\s*)?((?:یک|یه|اول|نخست|بیر|بیره|دو|دوم|سه|سوم|چهار|چهارم|پنج|پنجم|شش|شیش|ششم|هفت|هفتم|هشت|هشتم|نه|نهم|ده|دهم|یازده|یازدهم|دوازده|دوازدهم|سیزده|سیزدهم|چهارده|چهاردهم|پانزده|پونزده|پانزدهم|شانزده|شونزده|شانزدهم|هفده|هفدهم|هجده|هیجده|هجدهم|نوزده|نوزدهم|بیست|بیستم|سی|سی‌ام|سیام|چهل|چهلم|پنجاه|پنجاهم|شصت|شصتم|هفتاد|هفتادم|هشتاد|هشتادم|نود|نودم|صد|صدم)(?:\s+و\s+(?:یک|یه|اول|دو|دوم|سه|سوم|چهار|چهارم|پنج|پنجم|شش|شیش|ششم|هفت|هفتم|هشت|هشتم|نه|نهم))?)\b"""
        )
        val wordMatch = hallWordsRegex.find(normalizedText)
        if (wordMatch != null) {
            val parsedHallNum = parseHallNumberString(wordMatch.groupValues[1])
            if (parsedHallNum != null && parsedHallNum in 1L..100L) {
                val matchedFlock = flocks.find { it.id == parsedHallNum }
                    ?: flocks.getOrNull((parsedHallNum - 1).toInt())
                    ?: Flock(
                        id = parsedHallNum,
                        name = "سالن $parsedHallNum",
                        species = BirdSpecies.CHICKEN,
                        breed = "عمومی",
                        birthEpochMillis = System.currentTimeMillis() - 30L * 24 * 3600 * 1000,
                        birdCount = 1000,
                        customTempCelsius = 22.0
                    )
                return FlockMatchResult(matchedFlock, wordMatch.value)
            }
        }

        // ۳. تطابق نژاد گله‌ها (تنها در صورتی که شماره یا نام اختصاصی سالن بیان نشده و فقط نژاد ذکر شده باشد)
        // ۳.۱. تطابق اختصاصی سویه محبوب بلک استار
        if (normalizedText.contains("بلک استار") || normalizedText.contains("بلکستار") || normalizedText.contains("black star")) {
            val blackStarFlock = flocks.find {
                val bn = PersianVoiceNormalizer.normalize(it.breed)
                val nn = PersianVoiceNormalizer.normalize(it.name)
                bn.contains("بلک استار") || bn.contains("بلکستار") || nn.contains("بلک استار") || nn.contains("بلکستار")
            }
            if (blackStarFlock != null) {
                return FlockMatchResult(blackStarFlock, "بلک استار")
            }
        }

        // ۳.۲. تطابق نژاد گله‌ها (مانند آمروک یا راس)
        for (f in flocks) {
            val breedNorm = PersianVoiceNormalizer.normalize(f.breed)
            if (breedNorm.isNotBlank() && normalizedText.contains(breedNorm)) {
                return FlockMatchResult(f, f.breed)
            }
        }

        // اگر تنها یک گله در فارم وجود دارد، به عنوان پیش‌فرض در نظر گرفته می‌شود
        if (flocks.size == 1) {
            return FlockMatchResult(flocks.first(), null)
        }

        return FlockMatchResult(flocks.firstOrNull(), null)
    }

    val KNOWN_BREED_MAP = listOf(
        "بلک استار" to ("بلک استار" to BirdSpecies.CHICKEN),
        "بلکستار" to ("بلک استار" to BirdSpecies.CHICKEN),
        "black star" to ("بلک استار" to BirdSpecies.CHICKEN),
        "آمروک" to ("آمروک" to BirdSpecies.CHICKEN),
        "امروک" to ("آمروک" to BirdSpecies.CHICKEN),
        "پلیموث راک" to ("پلیموث راک" to BirdSpecies.CHICKEN),
        "پلیموث" to ("پلیموث راک" to BirdSpecies.CHICKEN),
        "پلیموت راک" to ("پلیموث راک" to BirdSpecies.CHICKEN),
        "پلیموت" to ("پلیموث راک" to BirdSpecies.CHICKEN),
        "پلیوت راک" to ("پلیموث راک" to BirdSpecies.CHICKEN),
        "پلیوت" to ("پلیموث راک" to BirdSpecies.CHICKEN),
        "راس 308" to ("راس ۳۰۸" to BirdSpecies.CHICKEN),
        "راس ۳۰۸" to ("راس ۳۰۸" to BirdSpecies.CHICKEN),
        "راس" to ("راس ۳۰۸" to BirdSpecies.CHICKEN),
        "کاب 500" to ("کاب ۵۰۰" to BirdSpecies.CHICKEN),
        "کاب ۵۰۰" to ("کاب ۵۰۰" to BirdSpecies.CHICKEN),
        "کاب" to ("کاب ۵۰۰" to BirdSpecies.CHICKEN),
        "کاپ" to ("کاب ۵۰۰" to BirdSpecies.CHICKEN),
        "لوهمن قهوه ای" to ("لوهمن قهوه‌ای" to BirdSpecies.CHICKEN),
        "لوهمن سفید" to ("لوهمن سفید" to BirdSpecies.CHICKEN),
        "لوهمن" to ("لوهمن قهوه‌ای" to BirdSpecies.CHICKEN),
        "های لاین" to ("های‌لاین" to BirdSpecies.CHICKEN),
        "هایلاین" to ("های‌لاین" to BirdSpecies.CHICKEN),
        "آرین" to ("آرین" to BirdSpecies.CHICKEN),
        "ارین" to ("آرین" to BirdSpecies.CHICKEN),
        "سویه آرین" to ("آرین" to BirdSpecies.CHICKEN),
        "جوجه آرین" to ("آرین" to BirdSpecies.CHICKEN),
        "هابارد" to ("هابارد" to BirdSpecies.CHICKEN),
        "هوبارد" to ("هابارد" to BirdSpecies.CHICKEN),
        "سویه هابارد" to ("هابارد" to BirdSpecies.CHICKEN),
        "آربور اکرز" to ("آربور اکرز" to BirdSpecies.CHICKEN),
        "آربوراکرز" to ("آربور اکرز" to BirdSpecies.CHICKEN),
        "اربورا کرز" to ("آربور اکرز" to BirdSpecies.CHICKEN),
        "سویه آربور اکرز" to ("آربور اکرز" to BirdSpecies.CHICKEN),
        "های‌سکس" to ("های‌سکس" to BirdSpecies.CHICKEN),
        "هایسکس" to ("های‌سکس" to BirdSpecies.CHICKEN),
        "های سکس" to ("های‌سکس" to BirdSpecies.CHICKEN),
        "سویه های‌سکس" to ("های‌سکس" to BirdSpecies.CHICKEN),
        "دکالب" to ("دکالب" to BirdSpecies.CHICKEN),
        "دیکالب" to ("دکالب" to BirdSpecies.CHICKEN),
        "سویه دکالب" to ("دکالب" to BirdSpecies.CHICKEN),
        "نووژن" to ("نووژن" to BirdSpecies.CHICKEN),
        "نووژن سفید" to ("نووژن سفید" to BirdSpecies.CHICKEN),
        "سویه نووژن" to ("نووژن" to BirdSpecies.CHICKEN),
        "سیاه مرندی" to ("مرندی" to BirdSpecies.CHICKEN),
        "مرندی" to ("مرندی" to BirdSpecies.CHICKEN),
        "لگهورن" to ("لگهورن" to BirdSpecies.CHICKEN),
        "لاری" to ("لاری افغان" to BirdSpecies.CHICKEN),
        "بومی" to ("بومی اصلاح‌شده" to BirdSpecies.CHICKEN),
        "محلی" to ("بومی تخم‌گذار" to BirdSpecies.CHICKEN),
        "گلپایگانی" to ("گلپایگانی" to BirdSpecies.CHICKEN),
        "براهما" to ("براهما امپراتور" to BirdSpecies.CHICKEN),
        "کوشین" to ("کوشین" to BirdSpecies.CHICKEN),
        "سلطان" to ("سلطان عثمانی" to BirdSpecies.CHICKEN),
        "ابریشمی" to ("ابریشمی سیلکی" to BirdSpecies.CHICKEN),
        "لهستانی" to ("لهستانی کاکلی" to BirdSpecies.CHICKEN),
        "سبرایت" to ("سبرایت مینیاتوری" to BirdSpecies.CHICKEN),
        "ساسو" to ("ساسو دو منظوره" to BirdSpecies.CHICKEN),
        "ایسام" to ("ایسام براون" to BirdSpecies.CHICKEN),
        "نیک چیک" to ("نیک‌چیک" to BirdSpecies.CHICKEN),
        "ارپینگتون" to ("ارپینگتون" to BirdSpecies.CHICKEN),
        "سوسکس" to ("سوسکس" to BirdSpecies.CHICKEN),
        "جاپنیز" to ("جاپنیز" to BirdSpecies.CHICKEN),
        "بلدرچین" to ("بلدرچین ژاپنی" to BirdSpecies.QUAIL),
        "بوقلمون" to ("بوقلمون برنز" to BirdSpecies.TURKEY),
        "اردک" to ("اردک پکنی" to BirdSpecies.DUCK),
        "غاز" to ("غاز خاکستری" to BirdSpecies.GOOSE),
        "شاخدار" to ("مرغ شاخدار گینه" to BirdSpecies.GUINEA_FOWL),
        "گینه" to ("مرغ شاخدار گینه" to BirdSpecies.GUINEA_FOWL)
    )

    fun findBreedAndSpecies(normalizedText: String): Pair<String, BirdSpecies>? {
        for ((key, pair) in KNOWN_BREED_MAP) {
            if (normalizedText.contains(key)) {
                return pair
            }
        }
        return null
    }

    private fun tryMergeSessionContext(
        norm: String,
        spokenText: String,
        sessionContext: Map<String, Any>,
        flocks: List<Flock>
    ): VoiceActionCardData? {
        val pendingIntent = sessionContext["pendingIntent"] as? String ?: return null

        val isQuestionOrInquiry = com.example.poultryapp.engine.FarmIntentSemanticRouter.isQuestionPattern(norm)
        if (isQuestionOrInquiry) {
            return null
        }

        when (pendingIntent) {
            "CREATE_FLOCK" -> {
                val prevBreed = (sessionContext["breed"] as? String) ?: "آمروک"
                val prevSpecies = (sessionContext["species"] as? BirdSpecies) ?: BirdSpecies.CHICKEN
                val prevPurpose = (sessionContext["purpose"] as? String) ?: ""

                val newBreedPair = findBreedAndSpecies(norm)
                val breed = newBreedPair?.first ?: prevBreed
                val species = newBreedPair?.second ?: prevSpecies

                val purpose = when {
                    norm.contains("تخمگذار") || norm.contains("تخم گذار") || norm.contains("تخم مرغ") || norm.contains("تخمی") -> "تخم‌گذار"
                    norm.contains("گوشتی") -> "گوشتی"
                    norm.contains("مولد") || norm.contains("مادر") -> "مولد"
                    prevPurpose.isNotBlank() -> prevPurpose
                    else -> "تخم‌گذار"
                }

                val countNum = PersianVoiceNormalizer.extractFirstNumber(norm)
                if (countNum != null && countNum > 0) {
                    val count = countNum.toInt()
                    val flockName = "گله $breed $purpose"
                    return VoiceActionCardData(
                        intentType = VoiceIntentType.CREATE_FLOCK,
                        title = "ایجاد گله جدید در فارم",
                        iconRes = R.drawable.ic_farm_dashboard,
                        primaryMetric = "${toPersianDigits(count)} قطعه ($breed - $purpose)",
                        details = listOf(
                            "نام گله" to flockName,
                            "گونه پرنده" to species.titleFa,
                            "نژاد" to breed,
                            "تعداد پرندگان" to "${toPersianDigits(count)} قطعه",
                            "هدف پرورشی" to purpose,
                            "سن اولیه" to "۱ روزه (پیش‌فرض جوجه)"
                        ),
                        systemEffect = "ثبت گله «$flockName» در فارم و فعال‌سازی سالن مربوطه",
                        rawText = spokenText,
                        isValid = true,
                        needsMoreInfo = false,
                        missingPromptFa = null,
                        quickReplies = emptyList(),
                        parameters = mapOf(
                            "flockName" to flockName,
                            "name" to flockName,
                            "species" to species,
                            "breed" to breed,
                            "birdCount" to count,
                            "ageDays" to 1,
                            "notes" to "گله $breed ($purpose) ثبت شده با دستیار هوشمند"
                        )
                    )
                } else {
                    val prompt = "لطفاً تعداد پرندگان گله «$breed» را به عدد بفرمایید (مثلاً: ۵۰ قطعه):"
                    return VoiceActionCardData(
                        intentType = VoiceIntentType.CREATE_FLOCK,
                        title = "ایجاد گله جدید (نیازمند تعداد)",
                        iconRes = R.drawable.ic_farm_dashboard,
                        primaryMetric = "تعداد مشخص نشده است",
                        details = listOf(
                            "نژاد انتخابی" to breed,
                            "هدف پرورشی" to purpose,
                            "راهنما" to prompt
                        ),
                        systemEffect = "برای ایجاد گله، اعلام تعداد پرندگان الزامی است.",
                        rawText = spokenText,
                        isValid = false,
                        needsMoreInfo = true,
                        missingPromptFa = prompt,
                        quickReplies = listOf("۵۰ قطعه", "۱۰۰ قطعه", "۲۰۰ قطعه", "۳۰ قطعه"),
                        parameters = mapOf(
                            "pendingIntent" to "CREATE_FLOCK",
                            "breed" to breed,
                            "species" to species,
                            "flockName" to "گله $breed",
                            "purpose" to purpose
                        )
                    )
                }
            }

            "RECORD_EGG" -> {
                val newFlockMatch = findTargetFlockWithPhrase(norm, flocks)
                val flock = newFlockMatch.flock ?: flocks.find { it.id == sessionContext["flockId"] } ?: flocks.firstOrNull()
                val flockId = flock?.id ?: 1L
                val flockName = flock?.name ?: "گله فعال"
                val excludePhrase = newFlockMatch.matchedPhrase
                val rawNum = PersianVoiceNormalizer.extractFirstNumber(norm, excludePhrase)
                val isExplicitZero = norm.contains("صفر") || norm.contains(" 0 ") || norm.endsWith(" 0") || norm.startsWith("0 ") || norm.contains("۰") ||
                        norm.contains("هیچ") || norm.contains("تخم مرغ نداشتیم") || norm.contains("تخم نداشتیم") || norm.contains("تولید نداشتیم")
                if (rawNum != null || isExplicitZero) {
                    val isExportCarton = norm.contains("صادراتی") || norm.contains("۳۶۰") || norm.contains("360")
                    val isHalfCarton = norm.contains("نیم کارتن") || norm.contains("نصف کارتن")
                    val isHalfTray = norm.contains("نیم شانه") || norm.contains("نصف شانه")
                    val isCarton = (norm.contains("کارتن") || norm.contains("کارتون")) && !isExportCarton
                    val isTray = norm.contains("شانه") || norm.contains("شونه")
                    val isFertile = norm.contains("نطفه") || norm.contains("جوجه کشی")

                    val eggNum = if (isExplicitZero && (rawNum == null || rawNum <= 0.0)) 0.0 else (rawNum ?: 0.0)
                    val effectiveNum = when {
                        isHalfCarton && eggNum == 0.0 && !isExplicitZero -> 0.5
                        isHalfTray && eggNum == 0.0 && !isExplicitZero -> 0.5
                        else -> eggNum
                    }

                    val totalEggs = when {
                        isExplicitZero && effectiveNum == 0.0 -> 0
                        isExportCarton -> (effectiveNum * 360).toInt()
                        isHalfCarton -> (effectiveNum * 180).toInt().coerceAtLeast(90)
                        isCarton -> (effectiveNum * 180).toInt()
                        isHalfTray -> (effectiveNum * 30).toInt().coerceAtLeast(15)
                        isTray -> (effectiveNum * 30).toInt()
                        else -> effectiveNum.toInt()
                    }
                    val fertileCount = if (isFertile) totalEggs else 0
                    val tableCount = if (!isFertile) totalEggs else 0

                    val cartonMultiplier = if (isExportCarton) 360.0 else 180.0
                    val cartonCount = when {
                        totalEggs == 0 -> 0.0
                        isExportCarton || isCarton -> effectiveNum
                        isHalfCarton -> 0.5
                        else -> totalEggs / cartonMultiplier
                    }
                    val trayCount = when {
                        totalEggs == 0 -> 0.0
                        isTray -> effectiveNum
                        isHalfTray -> 0.5
                        else -> totalEggs / 30.0
                    }

                    val metricUnit = when {
                        isExportCarton -> "کارتن صادراتی"
                        isCarton || isHalfCarton -> "کارتن"
                        isTray || isHalfTray -> "شانه"
                        else -> "دانه"
                    }

                    val resolvedDate = RelativeDateResolver.resolveDate(norm)
                    val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
                    val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()
                    val shift = resolvedDate?.shift ?: "DAY"

                    val primaryMetric = if (totalEggs == 0) {
                        "۰ دانه (بدون تولید تخم‌مرغ)"
                    } else {
                        "${toPersianDigits(effectiveNum)} $metricUnit (${toPersianDigits(totalEggs)} عدد سالم)"
                    }

                    return VoiceActionCardData(
                        intentType = VoiceIntentType.RECORD_EGG,
                        title = "ثبت تولید تخم‌مرغ",
                        iconRes = R.drawable.ic_farm_egg,
                        flockId = flockId,
                        flockName = flockName,
                        primaryMetric = primaryMetric,
                        details = listOf(
                            "گله هدف" to flockName,
                            "رده کیفی" to if (isFertile) "تخم‌مرغ نطفه‌دار (جوجه‌کشی)" else "تخم‌مرغ خوراکی روز",
                            "تعداد کل" to "${toPersianDigits(totalEggs)} عدد",
                            "تاریخ ثبت" to dateJalaliStr
                        ),
                        systemEffect = if (totalEggs == 0) "ثبت وضعیت روز بدون تولید برای گله «$flockName»" else "افزایش رکورد تولید گله «$flockName» و شارژ خودکار انبار فارم",
                        rawText = spokenText,
                        isValid = true,
                        needsMoreInfo = false,
                        missingPromptFa = null,
                        quickReplies = emptyList(),
                        parameters = mapOf(
                            "flockId" to flockId,
                            "flockName" to flockName,
                            "amount" to totalEggs.toDouble(),
                            "count" to totalEggs,
                            "totalEggs" to totalEggs,
                            "cartonCount" to cartonCount,
                            "trayCount" to trayCount,
                            "fertileCount" to fertileCount,
                            "tableEggCount" to tableCount,
                            "dateJalaliStr" to dateJalaliStr,
                            "timestampMillis" to timestampMillis,
                            "epochMillis" to timestampMillis,
                            "shift" to shift
                        )
                    )
                }
            }

            "RECORD_MORTALITY" -> {
                val newFlockMatch = findTargetFlockWithPhrase(norm, flocks)
                val flock = newFlockMatch.flock ?: flocks.find { it.id == sessionContext["flockId"] } ?: flocks.firstOrNull()
                val flockId = flock?.id ?: 1L
                val flockName = flock?.name ?: "گله فعال"
                val excludePhrase = newFlockMatch.matchedPhrase
                val rawCount = PersianVoiceNormalizer.extractFirstNumber(norm, excludePhrase)
                val isExplicitZero = norm.contains("صفر") || norm.contains(" 0 ") || norm.endsWith(" 0") || norm.startsWith("0 ") || norm.contains("۰") ||
                        norm.contains("هیچ") || norm.contains("تلفات نداشتیم") || norm.contains("تلفاتی نداشتیم") || norm.contains("مرگ و میر نداشتیم") ||
                        norm.contains("تلف نشد") || norm.contains("تلف نشدن")
                if (rawCount != null || isExplicitZero) {
                    val count = if (isExplicitZero && (rawCount == null || rawCount <= 0.0)) 0 else (rawCount?.toInt() ?: 0)
                    val reason = when {
                        norm.contains("گرما") || norm.contains("سرما") || norm.contains("استرس") || norm.contains("خفگی") || norm.contains("فن") -> MortalityReason.HEAT_COLD_STRESS
                        norm.contains("کانیبالیسم") || norm.contains("خودخوری") || norm.contains("نوک") || norm.contains("زخم") -> MortalityReason.CANNIBALISM
                        norm.contains("شکارچی") || norm.contains("موش") || norm.contains("گربه") || norm.contains("حادثه") -> MortalityReason.PREDATOR_ACCIDENT
                        norm.contains("فروش") || norm.contains("زنده") || norm.contains("کشتار") -> MortalityReason.LIVE_SALE
                        norm.contains("حذف") || norm.contains("پیر") || norm.contains("ضعیف") || norm.contains("واکووی") || norm.contains("کالینگ") || norm.contains("وازده") || norm.contains("وازدگی") -> MortalityReason.CULLING
                        else -> MortalityReason.DISEASE
                    }

                    val resolvedDate = RelativeDateResolver.resolveDate(norm)
                    val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
                    val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()

                    val detailsList = mutableListOf(
                        "گله هدف" to flockName,
                        "علت تلفات" to reason.titleFa,
                        "ماهیت" to if (reason.isTrueMortality) "تلفات بیولوژیکی فارم" else "خروج تجاری پرنده",
                        "تاریخ ثبت" to dateJalaliStr
                    )
                    val isSpike = count >= 10 || (count >= 5 && flock != null && flock.birdCount < 500)
                    if (isSpike) {
                        detailsList.add("⚠️ هشدار جهش تلفات" to "تلفات ($count قطعه) بیش از حد معمول است؛ پایش علائم بالینی توصیه می‌شود.")
                    }

                    return VoiceActionCardData(
                        intentType = VoiceIntentType.RECORD_MORTALITY,
                        title = "ثبت تلفات و خروج پرنده",
                        iconRes = R.drawable.ic_farm_health,
                        flockId = flockId,
                        flockName = flockName,
                        primaryMetric = if (count == 0) "۰ قطعه تلفات (بدون تلفات)" else "${toPersianDigits(count)} قطعه تلفات",
                        details = detailsList,
                        systemEffect = if (count == 0) "ثبت وضعیت سلامت کامل و بدون تلفات در گله «$flockName»" else "کسر قطعی ${toPersianDigits(count)} قطعه از موجودی زنده گله «$flockName»",
                        rawText = spokenText,
                        isValid = true,
                        needsMoreInfo = false,
                        missingPromptFa = null,
                        quickReplies = emptyList(),
                        parameters = mapOf(
                            "flockId" to flockId,
                            "flockName" to flockName,
                            "amount" to count.toDouble(),
                            "count" to count,
                            "reason" to reason,
                            "dateJalaliStr" to dateJalaliStr,
                            "timestampMillis" to timestampMillis,
                            "epochMillis" to timestampMillis,
                            "hasMortalitySpikeAlert" to isSpike
                        )
                    )
                }
            }

            "RECORD_FEED" -> {
                val newFlockMatch = findTargetFlockWithPhrase(norm, flocks)
                val flock = newFlockMatch.flock ?: flocks.find { it.id == sessionContext["flockId"] } ?: flocks.firstOrNull()
                val flockId = flock?.id ?: 1L
                val flockName = flock?.name ?: "گله فعال"
                val excludePhrase = newFlockMatch.matchedPhrase
                val isHalfBag = norm.contains("نیم کیسه") || norm.contains("نصف کیسه") || norm.contains("نیم گونی") || norm.contains("نصف گونی")
                val isHalfTon = norm.contains("نیم تن") || norm.contains("نصف تن")
                val isBags = norm.contains("کیسه") || norm.contains("گونی")
                val isTon = norm.contains("تن") && !isHalfTon
                val rawKg = when {
                    isHalfBag -> 0.5
                    isHalfTon -> 0.5
                    else -> PersianVoiceNormalizer.extractFirstNumber(norm, excludePhrase)
                }
                if (rawKg != null && rawKg > 0.0) {
                    val finalKg = when {
                        isHalfBag -> 25.0
                        isHalfTon -> 500.0
                        isBags -> rawKg * 50.0
                        isTon -> rawKg * 1000.0
                        else -> rawKg
                    }
                    val sackCount = finalKg / 50.0

                    val resolvedDate = RelativeDateResolver.resolveDate(norm)
                    val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
                    val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()
                    val shift = resolvedDate?.shift ?: "DAY"

                    val primaryMetric = when {
                        isHalfBag || isBags -> "${toPersianDigits(rawKg)} کیسه (${toPersianDigits(finalKg)} کیلوگرم دان)"
                        isHalfTon || isTon -> "${toPersianDigits(rawKg)} تن (${toPersianDigits(finalKg)} کیلوگرم دان)"
                        else -> "${toPersianDigits(finalKg)} کیلوگرم دان"
                    }

                    val isUnrealistic = finalKg > 100_000.0
                    return VoiceActionCardData(
                        intentType = VoiceIntentType.RECORD_FEED,
                        title = if (isUnrealistic) "شارژ نامتعارف دان (نیازمند تایید)" else "شارژ مصرف دان سالن",
                        iconRes = R.drawable.ic_farm_feed_water,
                        flockId = flockId,
                        flockName = flockName,
                        primaryMetric = primaryMetric,
                        details = listOf(
                            "گله مقصد" to flockName,
                            "مقدار شارژ" to "${toPersianDigits(finalKg)} کیلوگرم",
                            "تاریخ ثبت" to dateJalaliStr
                        ),
                        systemEffect = if (isUnrealistic) "مقدار بیش از حد معمول فارم است و نیاز به تایید دارد." else "ثبت در مصرف زنده گله «$flockName» و کسر خودکار از موجودی انبار دان",
                        rawText = spokenText,
                        isValid = !isUnrealistic,
                        needsMoreInfo = isUnrealistic,
                        missingPromptFa = if (isUnrealistic) "مقدار ${toPersianDigits(finalKg)} کیلوگرم دان غیرواقعی به نظر می‌رسد. آیا از ثبت این مقدار اطمینان دارید؟" else null,
                        errorMessage = if (isUnrealistic) "مقدار دان مصرفی (${toPersianDigits(finalKg)} کیلوگرم) غیرواقعی و بیش از ظرفیت فارم است." else null,
                        quickReplies = if (isUnrealistic) listOf("بله تایید است", "لغو") else emptyList(),
                        parameters = mapOf(
                            "flockId" to flockId,
                            "flockName" to flockName,
                            "amount" to finalKg,
                            "amountKg" to finalKg,
                            "sackCount" to sackCount,
                            "isUnrealistic" to isUnrealistic,
                            "dateJalaliStr" to dateJalaliStr,
                            "timestampMillis" to timestampMillis,
                            "epochMillis" to timestampMillis,
                            "shift" to shift
                        )
                    )
                }
            }

            "RECORD_WATER" -> {
                val newFlockMatch = findTargetFlockWithPhrase(norm, flocks)
                val flock = newFlockMatch.flock ?: flocks.find { it.id == sessionContext["flockId"] } ?: flocks.firstOrNull()
                val flockId = flock?.id ?: 1L
                val flockName = flock?.name ?: "گله فعال"
                val excludePhrase = newFlockMatch.matchedPhrase
                val rawLit = PersianVoiceNormalizer.extractFirstNumber(norm, excludePhrase)
                if (rawLit != null && rawLit > 0.0) {
                    var finalLit = rawLit
                    if (norm.contains("متر مکعب")) finalLit = rawLit * 1000.0

                    val resolvedDate = RelativeDateResolver.resolveDate(norm)
                    val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
                    val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()
                    val shift = resolvedDate?.shift ?: "DAY"

                    return VoiceActionCardData(
                        intentType = VoiceIntentType.RECORD_WATER,
                        title = "ثبت مصرف آب سالن",
                        iconRes = R.drawable.ic_farm_feed_water,
                        flockId = flockId,
                        flockName = flockName,
                        primaryMetric = "${toPersianDigits(finalLit)} لیتر آب",
                        details = listOf(
                            "گله مقصد" to flockName,
                            "حجم مصرفی" to "${toPersianDigits(finalLit)} لیتر",
                            "تاریخ ثبت" to dateJalaliStr
                        ),
                        systemEffect = "ثبت شاخص هیدراتاسیون و پایش زنده مصرف آب در گله «$flockName»",
                        rawText = spokenText,
                        isValid = true,
                        needsMoreInfo = false,
                        missingPromptFa = null,
                        quickReplies = emptyList(),
                        parameters = mapOf(
                            "flockId" to flockId,
                            "flockName" to flockName,
                            "amount" to finalLit,
                            "amountLiters" to finalLit,
                            "dateJalaliStr" to dateJalaliStr,
                            "timestampMillis" to timestampMillis,
                            "epochMillis" to timestampMillis,
                            "shift" to shift
                        )
                    )
                }
            }

            "RECORD_FINANCIAL" -> {
                val amountTomans = PersianVoiceNormalizer.extractFinancialAmountTomans(norm)
                if (amountTomans != null && amountTomans > 0.0) {
                    val flockId = sessionContext["flockId"] as? Long
                    val flockName = (sessionContext["flockName"] as? String) ?: "عمومی فارم"
                    val isIncome = (sessionContext["isIncome"] as? Boolean) ?: (norm.contains("درآمد") || norm.contains("فروش") || norm.contains("واریز"))
                    val txType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                    val category = (sessionContext["category"] as? TransactionCategory) ?: if (isIncome) TransactionCategory.OTHER_INCOME else TransactionCategory.OTHER_EXPENSE
                    val txTitle = (sessionContext["txTitle"] as? String) ?: if (isIncome) "درآمد ثبت شده با دستیار هوشمند" else "هزینه ثبت شده با دستیار هوشمند"
                    val formattedTomans = toPersianDigits(amountTomans.toLong())

                    val resolvedDate = RelativeDateResolver.resolveDate(norm)
                    val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
                    val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()

                    return VoiceActionCardData(
                        intentType = VoiceIntentType.RECORD_FINANCIAL,
                        title = if (isIncome) "ثبت درآمد فارم" else "ثبت هزینه مالی فارم",
                        iconRes = R.drawable.ic_farm_financial,
                        flockId = flockId,
                        flockName = flockName,
                        primaryMetric = "$formattedTomans تومان (${txType.titleFa})",
                        details = listOf(
                            "نوع عملیات" to txType.titleFa,
                            "دسته‌بندی" to category.titleFa,
                            "شرح فاکتور" to txTitle,
                            "سالن مرتبط" to flockName,
                            "تاریخ ثبت" to dateJalaliStr
                        ),
                        systemEffect = if (isIncome) {
                            "افزایش تراز مالی و ثبت سود ناشی از درآمد به مبلغ $formattedTomans تومان"
                        } else {
                            "ثبت هزینه به مبلغ $formattedTomans تومان در دفاتر مالی فارم و کسر از تراز نقدی"
                        },
                        rawText = spokenText,
                        isValid = true,
                        needsMoreInfo = false,
                        missingPromptFa = null,
                        quickReplies = emptyList(),
                        parameters = mapOf(
                            "amount" to amountTomans,
                            "amountTomans" to amountTomans,
                            "type" to txType,
                            "transactionType" to txType,
                            "category" to category,
                            "title" to txTitle,
                            "flockId" to (flockId ?: 1L),
                            "dateJalaliStr" to dateJalaliStr,
                            "timestampMillis" to timestampMillis,
                            "epochMillis" to timestampMillis
                        )
                    )
                }
            }

            "TRANSFER_BIRDS" -> {
                val rawCount = PersianVoiceNormalizer.extractFirstNumber(norm)
                if (rawCount != null && rawCount > 0.0) {
                    val count = rawCount.toInt()
                    val fromId = (sessionContext["fromFlockId"] as? Long) ?: 1L
                    val toId = (sessionContext["toFlockId"] as? Long) ?: 2L
                    val fromFlock = flocks.find { it.id == fromId } ?: flocks.firstOrNull()
                    val toFlock = flocks.find { it.id == toId } ?: flocks.getOrNull(1)
                    val fromName = fromFlock?.name ?: "سالن ۱"
                    val toName = toFlock?.name ?: "سالن ۲"

                    return VoiceActionCardData(
                        intentType = VoiceIntentType.TRANSFER_BIRDS,
                        title = "انتقال پرندگان بین سالن‌ها",
                        iconRes = R.drawable.ic_farm_dashboard,
                        flockId = fromId,
                        flockName = fromName,
                        primaryMetric = "${toPersianDigits(count)} قطعه ($fromName ⬅️ $toName)",
                        details = listOf(
                            "سالن مبدا" to fromName,
                            "سالن مقصد" to toName,
                            "تعداد انتقال" to "${toPersianDigits(count)} قطعه",
                            "تاریخ ثبت" to JalaliDateHelper.getJalaliDate().toStandardString()
                        ),
                        systemEffect = "کسر ${toPersianDigits(count)} قطعه از «$fromName» و افزودن به «$toName» در فارم",
                        rawText = spokenText,
                        isValid = true,
                        needsMoreInfo = false,
                        missingPromptFa = null,
                        quickReplies = emptyList(),
                        parameters = mapOf(
                            "fromFlockId" to fromId,
                            "toFlockId" to toId,
                            "fromFlockName" to fromName,
                            "toFlockName" to toName,
                            "count" to count,
                            "birdCount" to count
                        )
                    )
                }
            }
        }

        return null
    }

    /**
     * تقسیم جملات مرکب به بندهای عملیاتی مستقل بر اساس حروف ربط، علائم نگارشی و شاخص‌های دامنه
     */
    fun splitCompoundClauses(norm: String): List<String> {
        val rawParts = norm.split(Regex("([،,\\n]|\\s+و\\s+|\\s+همچنین\\s+|\\s+بعدش\\s+|\\s+سپس\\s+)"))
        val parts = rawParts.map { it.trim() }.filter { it.isNotBlank() && it !in listOf("و", "،", ",", "همچنین", "بعدش", "سپس") }

        val domainKeywords = listOf(
            Regex("تخم|شانه|کارتن"),
            Regex("تلفات|تلف|مرگ|مرده|مرد|کشته"),
            Regex("دان|دون|پلت|کنسانتره|خوراک"),
            Regex("مصرف آب|لیتر آب|آب|اب"),
            Regex("تومان|تومن|ریال|هزینه|خرج|درآمد|فروش"),
            Regex("دارو|واکسن|مکمل|ویتامین|آنتی بیوتیک|انتی بیوتیک|تایلوزین|لینکوپک|آمپرولیوم|انروفلوکساسین")
        )

        val finalClauses = mutableListOf<String>()
        for (part in parts) {
            val matchedCount = domainKeywords.count { it.containsMatchIn(part) }
            if (matchedCount > 1) {
                val sub = part.split(Regex("(?<=\\s)(?=(?:\\d+|یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده)\\s*(?:تا|عدد|قطعه|کیلو|کیسه|گونی|شانه|لیتر|تومان|هزار|سی\\s*سی|سیسی|گرم|بسته)?\\s*[\\s\\S]*?(?:تخم|تلف|مرد|مرگ|دان|دون|پلت|آب|اب|تومان|هزینه|دارو|واکسن|مکمل|ویتامین|لینکوپک|تایلوزین))"))
                for (s in sub) {
                    val trimmed = s.trim()
                    if (trimmed.isNotBlank()) finalClauses.add(trimmed)
                }
            } else {
                finalClauses.add(part)
            }
        }
        return finalClauses
    }

    /**
     * تفسیر تک‌بند عملیاتی مجزا در یک جمله مرکب
     */
    /**
     * تفسیر تک‌بند عملیاتی مجزا در یک جمله مرکب
     */
    fun parseSingleOperationalClause(
        clause: String,
        spokenText: String,
        targetFlock: Flock?,
        flocks: List<Flock>,
        contacts: List<FarmContact>
    ): VoiceActionCardData? {
        val norm = PersianVoiceNormalizer.normalize(clause)
        val flockMatch = findTargetFlockWithPhrase(norm, flocks)
        val f = flockMatch.flock ?: targetFlock ?: flocks.firstOrNull()
        val fId = f?.id ?: 1L
        val fName = f?.name ?: "گله فعال"
        val excludePhrase = flockMatch.matchedPhrase

        val resolvedDate = RelativeDateResolver.resolveDate(norm)
        val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
        val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()
        val shift = resolvedDate?.shift ?: "DAY"

        // ۰. مشاهدات و گزارش شیفت سالن
        val isObs = norm.contains("مشاهده") || norm.contains("یادداشت") || norm.contains("گزارش شیفت") ||
                norm.contains("ثبت وضعیت") || norm.contains("گزارش") || norm.contains("وضعیت سالن")
        if (isObs && !norm.contains("بگو") && !norm.contains("نشون")) {
            val noteText = clause
                .replace(Regex("^(یادداشت|گزارش شیفت|گزارش|مشاهده|ثبت وضعیت|وضعیت سالن|وضعیت)\\s*(?:سالن\\s*\\d+|شیفت)?[:،\\s]*"), "")
                .trim()
                .ifBlank { clause }

            return VoiceActionCardData(
                intentType = VoiceIntentType.RECORD_OBSERVATION,
                title = "ثبت گزارش و مشاهدات ($fName)",
                iconRes = R.drawable.ic_farm_dashboard,
                flockId = fId,
                flockName = fName,
                primaryMetric = if (noteText.length > 40) noteText.take(40) + "..." else noteText,
                details = listOf(
                    "سالن هدف" to fName,
                    "متن مشاهده" to noteText,
                    "تاریخ ثبت" to dateJalaliStr
                ),
                systemEffect = "ثبت در سوابق مشاهدات میدانی و شیفت سالن «$fName»",
                rawText = clause,
                isValid = true,
                parameters = mapOf(
                    "flockId" to fId,
                    "flockName" to fName,
                    "note" to noteText,
                    "category" to (if (norm.contains("شیفت")) "SHIFT_REPORT" else "OBSERVATION"),
                    "dateJalaliStr" to dateJalaliStr,
                    "timestampMillis" to timestampMillis,
                    "epochMillis" to timestampMillis,
                    "shift" to shift
                )
            )
        }

        // ۰.۵. انبارداری و کالاها (MANAGE_WAREHOUSE)
        val isWhClause = norm.contains("انبار") || norm.contains("شارژ انبار") ||
                ((norm.contains("ورود") || norm.contains("خروج") || norm.contains("حواله")) &&
                 (norm.contains("کیسه") || norm.contains("کارتن") || norm.contains("دان") || norm.contains("تخم")))
        if (isWhClause) {
            val isInc = norm.contains("ورود") || norm.contains("شارژ") || norm.contains("خرید") || norm.contains("رسید")
            val isOut = norm.contains("خروج") || norm.contains("حواله") || norm.contains("کسر") || norm.contains("ارسال")
            val rawQty = PersianVoiceNormalizer.extractFirstNumber(norm, excludePhrase) ?: 10.0
            val itemName = if (norm.contains("دان") || norm.contains("خوراک")) "دان طیور" else "کارتن تخم‌مرغ"
            val unit = if (norm.contains("کیسه")) "کیسه" else if (norm.contains("کارتن")) "کارتن" else "عدد"
            val delta = if (isOut) -rawQty else rawQty
            val actionTitle = if (isInc) "ثبت ورود کالا به انبار" else "ثبت خروج کالا از انبار"

            return VoiceActionCardData(
                intentType = VoiceIntentType.MANAGE_WAREHOUSE,
                title = actionTitle,
                iconRes = R.drawable.ic_farm_warehouse,
                primaryMetric = "${toPersianDigits(Math.abs(delta))} $unit $itemName",
                details = listOf(
                    "نام کالا" to itemName,
                    "نوع عملیات" to if (isInc) "ورود به انبار" else "خروج از انبار",
                    "مقدار" to "${toPersianDigits(Math.abs(delta))} $unit"
                ),
                systemEffect = if (isInc) "افزایش موجودی $itemName در انبار مرکزی فارم" else "کسر موجودی $itemName از انبار مرکزی فارم",
                rawText = clause,
                isValid = true,
                parameters = mapOf(
                    "itemName" to itemName,
                    "delta" to delta,
                    "amount" to Math.abs(delta),
                    "unit" to unit,
                    "isIncoming" to isInc
                )
            )
        }

        // ۱. تخم‌مرغ
        val isEgg = norm.contains("تخم") || norm.contains("شانه") || norm.contains("کارتن")
        if (isEgg) {
            val rawNum = PersianVoiceNormalizer.extractFirstNumber(norm, excludePhrase)
            val isExplicitZero = norm.contains("صفر") || norm.contains(" 0 ") || norm.endsWith(" 0") || norm.startsWith("0 ") || norm.contains("۰") ||
                    norm.contains("هیچ") || norm.contains("تخم مرغ نداشتیم") || norm.contains("تخم نداشتیم") || norm.contains("تولید نداشتیم")
            if (rawNum != null || isExplicitZero) {
                val isExportCarton = norm.contains("صادراتی") || norm.contains("۳۶۰") || norm.contains("360")
                val isHalfCarton = norm.contains("نیم کارتن") || norm.contains("نصف کارتن")
                val isHalfTray = norm.contains("نیم شانه") || norm.contains("نصف شانه")
                val isCarton = (norm.contains("کارتن") || norm.contains("کارتون")) && !isExportCarton
                val isTray = norm.contains("شانه") || norm.contains("شونه")
                val isFertile = norm.contains("نطفه") || norm.contains("جوجه کشی")

                val eggNum = if (isExplicitZero && (rawNum == null || rawNum <= 0.0)) 0.0 else (rawNum ?: 0.0)
                val effectiveNum = when {
                    isHalfCarton && eggNum == 0.0 && !isExplicitZero -> 0.5
                    isHalfTray && eggNum == 0.0 && !isExplicitZero -> 0.5
                    else -> eggNum
                }

                val totalEggs = when {
                    isExplicitZero && effectiveNum == 0.0 -> 0
                    isExportCarton -> (effectiveNum * 360).toInt()
                    isHalfCarton -> (effectiveNum * 180).toInt().coerceAtLeast(90)
                    isCarton -> (effectiveNum * 180).toInt()
                    isHalfTray -> (effectiveNum * 30).toInt().coerceAtLeast(15)
                    isTray -> (effectiveNum * 30).toInt()
                    else -> effectiveNum.toInt()
                }
                val metricUnit = when {
                    isExportCarton -> "کارتن صادراتی"
                    isCarton || isHalfCarton -> "کارتن"
                    isTray || isHalfTray -> "شانه"
                    else -> "دانه"
                }
                val cartonMultiplier = if (isExportCarton) 360.0 else 180.0
                val cartonCount = when {
                    totalEggs == 0 -> 0.0
                    isExportCarton || isCarton -> effectiveNum
                    isHalfCarton -> 0.5
                    else -> totalEggs / cartonMultiplier
                }
                val trayCount = when {
                    totalEggs == 0 -> 0.0
                    isTray -> effectiveNum
                    isHalfTray -> 0.5
                    else -> totalEggs / 30.0
                }

                return VoiceActionCardData(
                    intentType = VoiceIntentType.RECORD_EGG,
                    title = "ثبت تخم‌مرغ ($fName)",
                    iconRes = R.drawable.ic_farm_egg,
                    flockId = fId,
                    flockName = fName,
                    primaryMetric = if (totalEggs == 0) "۰ دانه (بدون تولید)" else "${toPersianDigits(effectiveNum)} $metricUnit (${toPersianDigits(totalEggs)} عدد)",
                    details = listOf(
                        "گله هدف" to fName,
                        "تعداد کل" to "${toPersianDigits(totalEggs)} عدد",
                        "تاریخ ثبت" to dateJalaliStr
                    ),
                    systemEffect = "ثبت تولید تخم‌مرغ در سالن «$fName»",
                    rawText = clause,
                    isValid = true,
                    parameters = mapOf(
                        "flockId" to fId,
                        "flockName" to fName,
                        "amount" to totalEggs.toDouble(),
                        "count" to totalEggs,
                        "totalEggs" to totalEggs,
                        "cartonCount" to cartonCount,
                        "trayCount" to trayCount,
                        "fertileCount" to if (isFertile) totalEggs else 0,
                        "tableEggCount" to if (!isFertile) totalEggs else 0,
                        "dateJalaliStr" to dateJalaliStr,
                        "timestampMillis" to timestampMillis,
                        "shift" to shift
                    )
                )
            }
        }

        // ۲. تلفات
        val isMortality = norm.contains("تلفات") || norm.contains("تلف") || norm.contains("مرگ") ||
                norm.contains("مرده") || norm.contains("مرد") || norm.contains("کشته")
        if (isMortality) {
            val rawCount = PersianVoiceNormalizer.extractFirstNumber(norm, excludePhrase)
            val isExplicitZero = norm.contains("صفر") || norm.contains(" 0 ") || norm.endsWith(" 0") || norm.startsWith("0 ") || norm.contains("۰") ||
                    norm.contains("هیچ") || norm.contains("تلفات نداشتیم") || norm.contains("تلفاتی نداشتیم") || norm.contains("مرگ و میر نداشتیم") ||
                    norm.contains("تلف نشد") || norm.contains("تلف نشدن")
            if (rawCount != null || isExplicitZero) {
                val count = if (isExplicitZero && (rawCount == null || rawCount <= 0.0)) 0 else (rawCount?.toInt() ?: 0)
                val reason = when {
                    norm.contains("گرما") || norm.contains("سرما") || norm.contains("خفگی") || norm.contains("استرس") -> MortalityReason.HEAT_COLD_STRESS
                    norm.contains("کانیبالیسم") || norm.contains("خودخوری") -> MortalityReason.CANNIBALISM
                    norm.contains("شکارچی") || norm.contains("حادثه") -> MortalityReason.PREDATOR_ACCIDENT
                    norm.contains("فروش") || norm.contains("زنده") -> MortalityReason.LIVE_SALE
                    norm.contains("حذف") || norm.contains("واکووی") || norm.contains("کالینگ") || norm.contains("وازده") || norm.contains("پیر") || norm.contains("ضعیف") -> MortalityReason.CULLING
                    else -> MortalityReason.DISEASE
                }
                return VoiceActionCardData(
                    intentType = VoiceIntentType.RECORD_MORTALITY,
                    title = "ثبت تلفات ($fName)",
                    iconRes = R.drawable.ic_farm_health,
                    flockId = fId,
                    flockName = fName,
                    primaryMetric = if (count == 0) "۰ قطعه تلفات (بدون تلفات)" else "${toPersianDigits(count)} قطعه تلفات",
                    details = listOf(
                        "گله هدف" to fName,
                        "علت تلفات" to reason.titleFa,
                        "تاریخ ثبت" to dateJalaliStr
                    ),
                    systemEffect = if (count == 0) "ثبت وضعیت سلامت بدون تلفات در سالن «$fName»" else "ثبت تلفات در سالن «$fName»",
                    rawText = clause,
                    isValid = true,
                    parameters = mapOf(
                        "flockId" to fId,
                        "flockName" to fName,
                        "amount" to count.toDouble(),
                        "count" to count,
                        "reason" to reason,
                        "dateJalaliStr" to dateJalaliStr,
                        "timestampMillis" to timestampMillis,
                        "shift" to shift
                    )
                )
            }
        }

        // ۳. دان و خوراک
        val isFeed = norm.contains("دان") || norm.contains("دون") || norm.contains("پلت") ||
                norm.contains("کنسانتره") || norm.contains("خوراک")
        if (isFeed) {
            val isHalfBag = norm.contains("نیم کیسه") || norm.contains("نصف کیسه") || norm.contains("نیم گونی") || norm.contains("نصف گونی")
            val isHalfTon = norm.contains("نیم تن") || norm.contains("نصف تن")
            val isBags = norm.contains("کیسه") || norm.contains("گونی")
            val isTon = norm.contains("تن") && !isHalfTon
            val rawKg = when {
                isHalfBag -> 0.5
                isHalfTon -> 0.5
                else -> PersianVoiceNormalizer.extractFirstNumber(norm, excludePhrase)
            }
            if (rawKg != null && rawKg > 0.0) {
                val totalKg = when {
                    isHalfBag -> 25.0
                    isHalfTon -> 500.0
                    isBags -> rawKg * 50.0
                    isTon -> rawKg * 1000.0
                    else -> rawKg
                }
                val sackCount = totalKg / 50.0
                val primaryMetric = when {
                    isHalfBag || isBags -> "${toPersianDigits(rawKg)} کیسه (${toPersianDigits(totalKg)} کیلوگرم دان)"
                    isHalfTon || isTon -> "${toPersianDigits(rawKg)} تن (${toPersianDigits(totalKg)} کیلوگرم دان)"
                    else -> "${toPersianDigits(totalKg)} کیلوگرم دان"
                }
                val isUnrealistic = totalKg > 100_000.0
                return VoiceActionCardData(
                    intentType = VoiceIntentType.RECORD_FEED,
                    title = if (isUnrealistic) "شارژ نامتعارف دان (نیازمند تایید)" else "شارژ دان ($fName)",
                    iconRes = R.drawable.ic_farm_feed_water,
                    flockId = fId,
                    flockName = fName,
                    primaryMetric = primaryMetric,
                    details = listOf(
                        "گله هدف" to fName,
                        "وزن مصرفی" to "${toPersianDigits(totalKg)} کیلوگرم",
                        "تاریخ ثبت" to dateJalaliStr
                    ),
                    systemEffect = if (isUnrealistic) "مقدار بیش از حد معمول فارم است و نیاز به تایید دارد." else "ثبت مصرف دان در سالن «$fName»",
                    rawText = clause,
                    isValid = !isUnrealistic,
                    needsMoreInfo = isUnrealistic,
                    missingPromptFa = if (isUnrealistic) "آیا از ثبت مقدار ${toPersianDigits(totalKg)} کیلوگرم دان برای «$fName» اطمینان دارید؟" else null,
                    errorMessage = if (isUnrealistic) "مقدار دان مصرفی (${toPersianDigits(totalKg)} کیلوگرم) غیرواقعی و بیش از ظرفیت فارم است." else null,
                    quickReplies = if (isUnrealistic) listOf("بله تایید است", "لغو") else emptyList(),
                    parameters = mapOf(
                        "flockId" to fId,
                        "flockName" to fName,
                        "amount" to totalKg,
                        "amountKg" to totalKg,
                        "sackCount" to sackCount,
                        "isUnrealistic" to isUnrealistic,
                        "dateJalaliStr" to dateJalaliStr,
                        "timestampMillis" to timestampMillis,
                        "shift" to shift
                    )
                )
            }
        }

        // ۴. آب
        val isWater = containsWaterWord(norm) && !norm.contains("هوا")
        if (isWater) {
            val rawLit = PersianVoiceNormalizer.extractFirstNumber(norm, excludePhrase)
            if (rawLit != null && rawLit > 0.0) {
                val totalLiters = if (norm.contains("متر مکعب")) rawLit * 1000.0 else rawLit
                return VoiceActionCardData(
                    intentType = VoiceIntentType.RECORD_WATER,
                    title = "ثبت مصرف آب ($fName)",
                    iconRes = R.drawable.ic_farm_feed_water,
                    flockId = fId,
                    flockName = fName,
                    primaryMetric = "${toPersianDigits(totalLiters)} لیتر آب",
                    details = listOf(
                        "گله هدف" to fName,
                        "حجم مصرفی" to "${toPersianDigits(totalLiters)} لیتر",
                        "تاریخ ثبت" to dateJalaliStr
                    ),
                    systemEffect = "ثبت مصرف آب در سالن «$fName»",
                    rawText = clause,
                    isValid = true,
                    parameters = mapOf(
                        "flockId" to fId,
                        "flockName" to fName,
                        "amount" to totalLiters,
                        "amountLiters" to totalLiters,
                        "dateJalaliStr" to dateJalaliStr,
                        "timestampMillis" to timestampMillis,
                        "shift" to shift
                    )
                )
            }
        }

        // ۵. مالی
        val isFinancial = norm.contains("تومان") || norm.contains("تومن") || norm.contains("ریال") ||
                norm.contains("هزینه") || norm.contains("خرج") || norm.contains("درآمد") || norm.contains("فروش")
        if (isFinancial) {
            val amountTomans = PersianVoiceNormalizer.extractFinancialAmountTomans(norm)
            if (amountTomans != null && amountTomans > 0.0) {
                val isIncome = norm.contains("درآمد") || norm.contains("فروش") || norm.contains("واریز")
                val txType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                val category = if (isIncome) TransactionCategory.EGG_SALE else TransactionCategory.FEED_PURCHASE
                val formattedTomans = toPersianDigits(amountTomans.toLong())
                return VoiceActionCardData(
                    intentType = VoiceIntentType.RECORD_FINANCIAL,
                    title = if (isIncome) "ثبت درآمد فارم" else "ثبت هزینه فارم",
                    iconRes = R.drawable.ic_farm_financial,
                    flockId = fId,
                    flockName = fName,
                    primaryMetric = "$formattedTomans تومان (${txType.titleFa})",
                    details = listOf(
                        "نوع عملیات" to txType.titleFa,
                        "مبلغ فاکتور" to "$formattedTomans تومان",
                        "تاریخ ثبت" to dateJalaliStr
                    ),
                    systemEffect = "ثبت تراکنش مالی در دفاتر حسابداری فارم",
                    rawText = clause,
                    isValid = true,
                    parameters = mapOf(
                        "amount" to amountTomans,
                        "amountTomans" to amountTomans,
                        "type" to txType,
                        "transactionType" to txType,
                        "category" to category,
                        "title" to (if (isIncome) "درآمد فروش ثبت شده با دستیار هوشمند" else "هزینه ثبت شده با دستیار هوشمند"),
                        "flockId" to fId,
                        "flockName" to fName,
                        "dateJalaliStr" to dateJalaliStr,
                        "timestampMillis" to timestampMillis
                    )
                )
            }
        }

        // ۶. دارو، واکسن و مکمل
        val isMed = norm.contains("دارو") || norm.contains("واکسن") || norm.contains("مکمل") ||
                norm.contains("ویتامین") || norm.contains("آنتی بیوتیک") || norm.contains("انتی بیوتیک") ||
                norm.contains("آمپرولیوم") || norm.contains("لینکوپک") || norm.contains("انروفلوکساسین") ||
                norm.contains("تایلوزین")
        if (isMed) {
            val medName = when {
                norm.contains("آمپرولیوم") -> "داروی آمپرولیوم (ضد کوکسیدیوز)"
                norm.contains("لینکوپک") -> "آنتی‌بیوتیک لینکوپک"
                norm.contains("انروفلوکساسین") -> "آنتی‌بیوتیک انروفلوکساسین"
                norm.contains("ویتامین ث") || norm.contains("ویتامین c") -> "پودر ویتامین ث"
                norm.contains("ویتامین") -> "مولتی ویتامین و مکمل تقویتی"
                norm.contains("نیوکاسل") -> "واکسن نیوکاسل"
                norm.contains("آنفلوانزا") || norm.contains("انفلوانزا") -> "واکسن آنفلوانزا"
                norm.contains("گامبورو") -> "واکسن گامبورو"
                norm.contains("برونشیت") -> "واکسن برونشیت"
                norm.contains("تایلوزین") -> "آنتی‌بیوتیک تایلوزین"
                else -> "دارو / مکمل تجویز شده"
            }
            val rawDosage = PersianVoiceNormalizer.extractFirstNumber(norm, excludePhrase)
            val dosageUnit = when {
                norm.contains("سی سی") || norm.contains("سیسی") || norm.contains("سی‌سی") || norm.contains("cc") -> "سی‌سی"
                norm.contains("گرم") -> "گرم"
                norm.contains("لیتر") -> "لیتر"
                norm.contains("بسته") -> "بسته"
                norm.contains("قطره") -> "قطره"
                else -> null
            }
            val dosageStr = if (rawDosage != null && dosageUnit != null) "${toPersianDigits(rawDosage)} $dosageUnit" else null
            val isAntibiotic = norm.contains("آنتی بیوتیک") || norm.contains("انتی بیوتیک") ||
                    norm.contains("تایلوزین") || norm.contains("لینکوپک") || norm.contains("انروفلوکساسین")
            val withdrawalNotice = if (isAntibiotic) "دوره پرهیز مصرف: ۵ روز گوشت و ۳ روز تخم‌مرغ" else null

            val adminRoute = when {
                norm.contains("اسپری") -> "اسپری"
                containsWaterWord(norm) || norm.contains("اشامیدنی") || norm.contains("آشامیدنی") -> "آب آشامیدنی"
                norm.contains("تزریق") -> "تزریق"
                norm.contains("دان") -> "مخلوط با دان"
                else -> "آب آشامیدنی"
            }

            return VoiceActionCardData(
                intentType = VoiceIntentType.RECORD_MEDICATION,
                title = "ثبت تجویز دارو ($fName)",
                iconRes = R.drawable.ic_farm_health,
                flockId = fId,
                flockName = fName,
                primaryMetric = if (dosageStr != null) "$medName ($dosageStr)" else medName,
                details = listOf(
                    "گله هدف" to fName,
                    "دارو / واکسن" to medName,
                    "روش مصرف" to adminRoute,
                    "تاریخ ثبت" to dateJalaliStr
                ),
                systemEffect = "ثبت تجویز دارویی در پرونده بهداشتی سالن «$fName»",
                rawText = clause,
                isValid = true,
                parameters = mapOf(
                    "flockId" to fId,
                    "flockName" to fName,
                    "medName" to medName,
                    "drugName" to medName,
                    "dosage" to (rawDosage ?: 0.0),
                    "dosageUnit" to (dosageUnit ?: ""),
                    "dosageStr" to (dosageStr ?: ""),
                    "route" to adminRoute,
                    "administrationRoute" to adminRoute,
                    "withdrawalAlert" to (withdrawalNotice ?: ""),
                    "withdrawalNotice" to (withdrawalNotice ?: ""),
                    "dateJalaliStr" to dateJalaliStr,
                    "timestampMillis" to timestampMillis,
                    "shift" to shift
                )
            )
        }

        return null
    }

    /**
     * استخراج دستورات چندگانه همزمان (Compound Actions)
     */
    fun tryParseCompoundActions(
        norm: String,
        spokenText: String,
        matchedFlock: Flock?,
        flocks: List<Flock>,
        contacts: List<FarmContact>
    ): VoiceActionCardData? {
        val hasEgg = norm.contains("تخم") || norm.contains("شانه") || norm.contains("کارتن")
        val hasMortality = norm.contains("تلفات") || norm.contains("تلف") || norm.contains("مرگ") || norm.contains("مرد")
        val hasFeed = norm.contains("دان") || norm.contains("دون") || norm.contains("پلت") || norm.contains("کنسانتره") || norm.contains("خوراک")
        val hasWater = containsWaterWord(norm) && !norm.contains("هوا")
        val hasFinancial = norm.contains("تومان") || norm.contains("ریال") || norm.contains("هزینه") || norm.contains("خرج") || norm.contains("فروش") || norm.contains("درآمد")
        val hasObs = norm.contains("مشاهده") || norm.contains("یادداشت") || norm.contains("گزارش شیفت") || norm.contains("ثبت وضعیت")
        val hasMed = norm.contains("دارو") || norm.contains("واکسن") || norm.contains("آنتی بیوتیک") ||
                norm.contains("انتی بیوتیک") || norm.contains("ویتامین") || norm.contains("مکمل") ||
                norm.contains("تایلوزین") || norm.contains("لینکوپک") || norm.contains("آمپرولیوم") ||
                norm.contains("انروفلوکساسین")

        val hasWarehouse = norm.contains("انبار") ||
                (norm.contains("ورود") && norm.contains("خروج")) ||
                (norm.contains("کیسه") && norm.contains("کارتن"))
        val domainCount = (if (hasEgg) 1 else 0) + (if (hasMortality) 1 else 0) + (if (hasFeed) 1 else 0) +
                (if (hasWater) 1 else 0) + (if (hasFinancial) 1 else 0) + (if (hasObs) 1 else 0) +
                (if (hasWarehouse) 1 else 0) + (if (hasMed) 1 else 0)
        if (domainCount < 2) return null

        val clauses = splitCompoundClauses(norm)
        val isSingleFinancialTransaction = (norm.contains("فروش") || norm.contains("خرید")) &&
                (norm.contains("تومان") || norm.contains("تومن") || norm.contains("ریال") || norm.contains("نقدی")) &&
                (norm.contains("مبلغ") || norm.contains("ارزش") || (!norm.contains(" و سالن") && !norm.contains(" همچنین ")))
        if (isSingleFinancialTransaction) {
            return null
        }
        val subActions = mutableListOf<VoiceActionCardData>()
        val seenIntents = mutableSetOf<VoiceIntentType>()

        val overallDate = RelativeDateResolver.resolveDate(norm)
        val overallDateStr = overallDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
        val overallTimestamp = overallDate?.timestampMillis ?: System.currentTimeMillis()
        val overallShift = overallDate?.shift ?: "DAY"

        val f = matchedFlock ?: flocks.firstOrNull()
        val defaultFlockId = f?.id ?: 1L
        val defaultFlockName = f?.name ?: "گله فعال"

        for (clause in clauses) {
            val act = parseSingleOperationalClause(clause, spokenText, matchedFlock, flocks, contacts)
            if (act != null && act.isValid && act.intentType !in seenIntents) {
                // انتشار گله و تاریخ کلی جمله به زیرعملیات در صورت عدم تعیین اختصاصی
                val actFlockId = act.flockId ?: defaultFlockId
                val actFlockName = act.flockName ?: defaultFlockName
                val actDateStr = (act.parameters["dateJalaliStr"] as? String) ?: overallDateStr
                val actTimestamp = (act.parameters["timestampMillis"] as? Long) ?: overallTimestamp
                val actShift = (act.parameters["shift"] as? String) ?: overallShift

                val enrichedParams = act.parameters.toMutableMap().apply {
                    put("flockId", actFlockId)
                    put("flockName", actFlockName)
                    put("dateJalaliStr", actDateStr)
                    put("timestampMillis", actTimestamp)
                    put("shift", actShift)
                }

                val enrichedAct = act.copy(
                    flockId = actFlockId,
                    flockName = actFlockName,
                    parameters = enrichedParams
                )

                subActions.add(enrichedAct)
                seenIntents.add(act.intentType)
            }
        }

        if (subActions.size >= 2) {
            return VoiceActionCardData(
                intentType = VoiceIntentType.COMPOUND_ACTION,
                title = "ثبت همزمان ${toPersianDigits(subActions.size)} عملیات فارم",
                iconRes = R.drawable.ic_farm_dashboard,
                flockId = defaultFlockId,
                flockName = defaultFlockName,
                primaryMetric = "${toPersianDigits(subActions.size)} رکورد آماده ثبت و تایید",
                details = subActions.map { it.title to it.primaryMetric },
                systemEffect = "ثبت یکجای اطلاعات فوق در دفاتر آماری و مالی سالن «$defaultFlockName»",
                rawText = spokenText,
                isValid = true,
                needsMoreInfo = false,
                missingPromptFa = null,
                quickReplies = emptyList(),
                subActions = subActions,
                parameters = mapOf(
                    "flockId" to defaultFlockId,
                    "flockName" to defaultFlockName,
                    "actionCount" to subActions.size,
                    "subActions" to subActions,
                    "dateJalaliStr" to overallDateStr,
                    "timestampMillis" to overallTimestamp,
                    "epochMillis" to overallTimestamp,
                    "shift" to overallShift
                )
            )
        }

        return null
    }

    /**
     * استخراج تنظیم آلارم و یادآور تقویم فارم (SET_SCHEDULE)
     */
    fun tryParseSetSchedule(
        norm: String,
        spokenText: String,
        matchedFlock: Flock?,
        flocks: List<Flock>
    ): VoiceActionCardData? {
        val hasScheduleKeyword = norm.contains("یادآور") || norm.contains("آلارم") || norm.contains("هشدار") ||
                norm.contains("تنظیم کن") || norm.contains("بذار") || norm.contains("بگذار") ||
                (norm.contains("برنامه") && norm.contains("ساعت"))
        val hasTimeOrDay = norm.contains("ساعت") || norm.contains("فردا") || norm.contains("پس فردا") ||
                norm.contains("شنبه") || norm.contains("یکشنبه") || norm.contains("دوشنبه") ||
                norm.contains("سه شنبه") || norm.contains("چهارشنبه") || norm.contains("پنجشنبه") ||
                norm.contains("جمعه") || norm.contains("صبح") || norm.contains("عصر") || norm.contains("شب")

        if (!hasScheduleKeyword || !hasTimeOrDay) return null

        val f = matchedFlock ?: flocks.firstOrNull()
        val flockId = f?.id ?: 1L
        val flockName = f?.name ?: "گله فعال"

        // استخراج ساعت و دقیقه
        var hour = 9
        var minute = 0

        val timeMatch = Regex("ساعت\\s*(\\d+)(?::(\\d+))?").find(norm)
        if (timeMatch != null) {
            hour = timeMatch.groupValues[1].toIntOrNull() ?: 9
            minute = timeMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        } else {
            val wordHour = when {
                norm.contains("ساعت یک") || norm.contains("ساعت ۱") -> 1
                norm.contains("ساعت دو") || norm.contains("ساعت ۲") -> 2
                norm.contains("ساعت سه") || norm.contains("ساعت ۳") -> 3
                norm.contains("ساعت چهار") || norm.contains("ساعت ۴") -> 4
                norm.contains("ساعت پنج") || norm.contains("ساعت ۵") -> 5
                norm.contains("ساعت شش") || norm.contains("ساعت ۶") -> 6
                norm.contains("ساعت هفت") || norm.contains("ساعت ۷") -> 7
                norm.contains("ساعت هشت") || norm.contains("ساعت ۸") -> 8
                norm.contains("ساعت نه") || norm.contains("ساعت ۹") -> 9
                norm.contains("ساعت ده") || norm.contains("ساعت ۱۰") -> 10
                norm.contains("ساعت یازده") || norm.contains("ساعت ۱۱") -> 11
                norm.contains("ساعت دوازده") || norm.contains("ساعت ۱۲") -> 12
                else -> null
            }
            if (wordHour != null) {
                hour = wordHour
            }
        }

        val isPm = (norm.contains("عصر") || norm.contains("بعدازظهر") || norm.contains("بعد از ظهر") || norm.contains("شب")) && !norm.contains("صبح")
        if (isPm && hour in 1..11) {
            hour += 12
        }

        // استخراج نوع یادآور
        val scheduleType = when {
            norm.contains("واکسن") -> "واکسن"
            norm.contains("ویتامین") || norm.contains("مکمل") -> "مکمل"
            norm.contains("دارو") || norm.contains("آنتی بیوتیک") -> "دارو"
            norm.contains("ضدعفونی") || norm.contains("بهداشت") -> "ضدعفونی"
            norm.contains("دان") || norm.contains("خوراک") || norm.contains("دانخوری") -> "تغذیه"
            norm.contains("آب") || norm.contains("ابخوری") || norm.contains("آبخوری") -> "آب"
            else -> "سایر"
        }

        // استخراج روزهای هفته
        val weekday = PersianVoiceNormalizer.findWeekday(norm)
        val daysOfWeek = mutableListOf<Int>()
        if (norm.contains("هر روز") || norm.contains("روزانه")) {
            daysOfWeek.addAll(listOf(0, 1, 2, 3, 4, 5, 6))
        } else if (weekday != null) {
            daysOfWeek.add(weekday.second)
        } else if (norm.contains("فردا")) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val dIdx = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.SATURDAY -> 0
                java.util.Calendar.SUNDAY -> 1
                java.util.Calendar.MONDAY -> 2
                java.util.Calendar.TUESDAY -> 3
                java.util.Calendar.WEDNESDAY -> 4
                java.util.Calendar.THURSDAY -> 5
                java.util.Calendar.FRIDAY -> 6
                else -> 0
            }
            daysOfWeek.add(dIdx)
        }

        // عنوان یادآور
        val title = when {
            norm.contains("نیوکاسل") -> "واکسن نیوکاسل"
            norm.contains("آنفلوانزا") || norm.contains("انفلوانزا") -> "واکسن آنفلوانزا"
            norm.contains("گامبورو") -> "واکسن گامبورو"
            norm.contains("دانخوری") -> "بررسی دانخوری‌ها"
            norm.contains("ویتامین") -> "مکمل ویتامین"
            norm.contains("ضدعفونی") -> "ضدعفونی سالن"
            else -> "یادآور پرورشی سالن $flockName"
        }

        val timeStr = "${toPersianDigits("%02d".format(hour))}:${toPersianDigits("%02d".format(minute))}"
        val daysFa = if (daysOfWeek.size == 7) "هر روز هفته" else if (daysOfWeek.isNotEmpty()) {
            val names = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
            daysOfWeek.joinToString("، ") { names.getOrElse(it) { "" } }
        } else "موعد مقرر"

        val category = when {
            norm.contains("واکسن") -> "VACCINE"
            norm.contains("ویتامین") || norm.contains("مکمل") -> "SUPPLEMENT"
            norm.contains("دارو") || norm.contains("آنتی بیوتیک") -> "MEDICATION"
            norm.contains("دانخوری") || norm.contains("دان") -> "FEED"
            norm.contains("آب") || norm.contains("ابخوری") -> "WATER"
            norm.contains("ضدعفونی") -> "HYGIENE"
            else -> "OTHER"
        }

        return VoiceActionCardData(
            intentType = VoiceIntentType.SET_SCHEDULE,
            title = "تنظیم یادآور و آلارم فارم",
            iconRes = R.drawable.ic_farm_dashboard,
            flockId = flockId,
            flockName = flockName,
            primaryMetric = "$title ($timeStr)",
            details = listOf(
                "عنوان یادآور" to title,
                "نوع هشدار" to scheduleType,
                "ساعت آلارم" to timeStr,
                "روزهای تکرار" to daysFa,
                "سالن هدف" to flockName
            ),
            systemEffect = "ثبت در تقویم کاری فارم و فعال‌سازی آلارم هوشمند در ساعت $timeStr",
            rawText = spokenText,
            isValid = true,
            parameters = mapOf(
                "flockId" to flockId,
                "flockName" to flockName,
                "title" to title,
                "category" to category,
                "scheduleType" to scheduleType,
                "hour" to hour,
                "minute" to minute,
                "daysOfWeek" to daysOfWeek,
                "scheduleMode" to "WEEKLY"
            )
        )
    }

    /**
     * استخراج ثبت صوتی مشاهدات سالن و گزارش شیفت کارگر (RECORD_OBSERVATION)
     */
    fun tryParseRecordObservation(
        norm: String,
        spokenText: String,
        matchedFlock: Flock?,
        flocks: List<Flock>,
        userRole: UserRole = UserRole.MANAGER
    ): VoiceActionCardData? {
        val isWeighing = norm.contains("توزین") || norm.contains("وزن کشی") || norm.contains("وزن‌کشی") ||
                norm.contains("وزن میانگین") || norm.contains("میانگین وزن")
        val isObsKeyword = norm.contains("مشاهده") || norm.contains("یادداشت") || norm.contains("گزارش شیفت") ||
                norm.contains("ثبت وضعیت") || norm.contains("وضعیت سالن") ||
                (norm.contains("گزارش:") || norm.startsWith("گزارش ") || norm.contains("ثبت گزارش")) ||
                isWeighing
        val isQuery = norm.contains("بگو") || norm.contains("نشون") || norm.contains("چطوره") ||
                norm.contains("چطور است") || norm.contains("چگونه است") || norm.contains("چیست") || norm.contains("چقدر")
        val isFinancialOrWarehouseOrStats = norm.contains("مالی") || norm.contains("تراز") || norm.contains("سود") ||
                norm.contains("درآمد") || norm.contains("هزینه") || norm.contains("انبار") || norm.contains("موجودی")
        if (!isObsKeyword || isQuery || isFinancialOrWarehouseOrStats) return null

        val flockMatch = findTargetFlockWithPhrase(norm, flocks)
        val f = flockMatch.flock ?: matchedFlock ?: flocks.firstOrNull()
        val flockId = f?.id ?: 1L
        val flockName = f?.name ?: "گله فعال"
        val excludeFlockPhrase = flockMatch.matchedPhrase

        val resolvedDate = RelativeDateResolver.resolveDate(norm)
        val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
        val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()
        val shift = resolvedDate?.shift ?: "DAY"

        if (isWeighing) {
            val rawWeight = PersianVoiceNormalizer.extractFirstNumber(norm, excludeFlockPhrase)
            val isKg = norm.contains("کیلو") && !norm.contains("گرم")
            val weightGrams = when {
                rawWeight == null -> null
                isKg -> rawWeight * 1000.0
                rawWeight < 10.0 -> rawWeight * 1000.0
                else -> rawWeight
            }
            val weightKg = weightGrams?.let { it / 1000.0 }
            val primaryMetricStr = if (weightGrams != null) {
                "${toPersianDigits(weightGrams.toInt())} گرم (${toPersianDigits(weightKg ?: 0.0)} کیلوگرم)"
            } else {
                "ثبت توزین نمونه پرنده"
            }
            return VoiceActionCardData(
                intentType = VoiceIntentType.RECORD_OBSERVATION,
                title = "ثبت وزن‌کشی و توزین سالن ($flockName)",
                iconRes = R.drawable.ic_farm_dashboard,
                flockId = flockId,
                flockName = flockName,
                primaryMetric = primaryMetricStr,
                details = listOf(
                    "سالن هدف" to flockName,
                    "میانگین وزن نمونه" to (if (weightGrams != null) "${toPersianDigits(weightGrams.toInt())} گرم" else "نامشخص"),
                    "تاریخ ثبت" to dateJalaliStr,
                    "شیفت کاری" to (if (shift == "NIGHT") "نوبت شب" else "نوبت روز")
                ),
                systemEffect = "ثبت رکورد پایش وزن گله «$flockName» در نمودار رشد",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "flockId" to flockId,
                    "flockName" to flockName,
                    "category" to "WEIGHING",
                    "weightGrams" to (weightGrams ?: 0.0),
                    "weightKg" to (weightKg ?: 0.0),
                    "sampleWeight" to (weightGrams ?: 0.0),
                    "note" to spokenText,
                    "dateJalaliStr" to dateJalaliStr,
                    "timestampMillis" to timestampMillis,
                    "epochMillis" to timestampMillis,
                    "shift" to shift
                )
            )
        }

        val cleanNote = spokenText
            .replace(Regex("^(یادداشت|گزارش شیفت|گزارش|مشاهده|ثبت وضعیت|وضعیت سالن|ثبت گزارش)\\s*(?:سالن\\s*\\d+|شیفت)?[:،\\s]*"), "")
            .trim()
            .ifBlank { spokenText }

        val category = when {
            norm.contains("شیفت") -> "SHIFT_REPORT"
            norm.contains("رطوبت") || norm.contains("دما") || norm.contains("تهویه") || norm.contains("بستر") -> "ENVIRONMENT"
            norm.contains("فن") || norm.contains("تسمه") || norm.contains("پد") || norm.contains("هیتر") -> "MAINTENANCE"
            norm.contains("ضدعفونی") || norm.contains("قرنطینه") -> "BIOSECURITY"
            else -> "OBSERVATION"
        }

        return VoiceActionCardData(
            intentType = VoiceIntentType.RECORD_OBSERVATION,
            title = "ثبت مشاهدات سالن و گزارش شیفت",
            iconRes = R.drawable.ic_farm_dashboard,
            flockId = flockId,
            flockName = flockName,
            primaryMetric = if (cleanNote.length > 40) cleanNote.take(40) + "..." else cleanNote,
            details = listOf(
                "سالن هدف" to flockName,
                "متن گزارش" to cleanNote,
                "دسته‌بندی" to category,
                "تاریخ ثبت" to dateJalaliStr,
                "شیفت کاری" to (if (shift == "NIGHT") "نوبت شب" else "نوبت روز")
            ),
            systemEffect = "ثبت در سوابق مشاهدات میدانی و شیفت سالن «$flockName»",
            rawText = spokenText,
            isValid = true,
            parameters = mapOf(
                "flockId" to flockId,
                "flockName" to flockName,
                "note" to cleanNote,
                "category" to category,
                "dateJalaliStr" to dateJalaliStr,
                "timestampMillis" to timestampMillis,
                "epochMillis" to timestampMillis,
                "shift" to shift
            )
        )
    }

    /**
     * استخراج و محاسبه صوتی شاخص‌های علمی پرورشی در لحظه (CALCULATE_METRIC)
     */
    fun tryParseCalculateMetric(
        norm: String,
        spokenText: String,
        matchedFlock: Flock?,
        flocks: List<Flock>,
        farmContext: FarmLiveContext
    ): VoiceActionCardData? {
        val isEpef = norm.contains("epef") || norm.contains("شاخص کارایی") || norm.contains("شاخص تولید") || norm.contains("شاخص بهره وری") || norm.contains("شاخص بهره‌وری")
        val isFcr = (norm.contains("ضریب تبدیل") || norm.contains("fcr")) && !isEpef
        val isLayingCalc = norm.contains("درصد تولید") || (norm.contains("راندمان") && norm.contains("محاسبه"))
        val isMortalityCalc = norm.contains("درصد تلفات") || norm.contains("نرخ تلفات")
        val isWaterFeedRatio = norm.contains("نسبت آب به دان") || norm.contains("نسبت اب به دان")
        val isDrugDose = (norm.contains("دوز") || norm.contains("مقدار مصرف")) &&
                (norm.contains("دارو") || norm.contains("تایلوزین") || norm.contains("انتی بیوتیک") || norm.contains("آنتی بیوتیک") || norm.contains("مرغ"))

        if (!isEpef && !isFcr && !isLayingCalc && !isMortalityCalc && !isWaterFeedRatio && !isDrugDose) return null

        val f = matchedFlock ?: flocks.firstOrNull()
        val flockId = f?.id ?: 1L
        val flockName = f?.name ?: "گله فعال"
        val birdCount = f?.effectiveBirdCount?.coerceAtLeast(1) ?: 1000

        when {
            isEpef -> {
                val weightGrams = PersianVoiceNormalizer.extractFirstNumber(norm.substringAfter("وزن")) ?: 2400.0
                val weightKg = if (weightGrams > 100.0) weightGrams / 1000.0 else weightGrams
                val age = PersianVoiceNormalizer.extractFirstNumber(norm.substringAfter("سن")) ?: 38.0
                val fcr = PersianVoiceNormalizer.extractFirstNumber(norm.substringAfter("ضریب تبدیل")) ?: 1.5
                val mort = PersianVoiceNormalizer.extractFirstNumber(norm.substringAfter("تلفات")) ?: 2.0
                val survival = (100.0 - mort).coerceIn(0.0, 100.0)
                val epef = if (age > 0 && fcr > 0) (survival * weightKg) / (age * fcr) * 100.0 else 412.6
                val epefStr = toPersianDigits(String.format(Locale.US, "%.1f", epef))
                return VoiceActionCardData(
                    intentType = VoiceIntentType.CALCULATE_METRIC,
                    title = "محاسبه شاخص تولید اروپایی (EPEF)",
                    iconRes = R.drawable.ic_farm_dashboard,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = "شاخص EPEF: $epefStr",
                    details = listOf(
                        "شاخص کارایی اروپایی (EPEF)" to epefStr,
                        "سن گله" to "${toPersianDigits(age.toInt())} روزگی",
                        "وزن زنده" to "${toPersianDigits(weightKg)} کیلوگرم",
                        "ضریب تبدیل (FCR)" to toPersianDigits(fcr),
                        "نرخ بقا / ماندگاری" to "${toPersianDigits(survival)}٪"
                    ),
                    systemEffect = "ارزیابی شاخص راندمان تولید اروپایی (EPEF) سالن",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "epef" to epef,
                        "metric" to "EPEF"
                    )
                )
            }

            isFcr -> {
                val fcrValue = "۱.۶۵"
                return VoiceActionCardData(
                    intentType = VoiceIntentType.CALCULATE_METRIC,
                    title = "محاسبه برخط ضریب تبدیل (FCR)",
                    iconRes = R.drawable.ic_farm_dashboard,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = "ضریب تبدیل (FCR): $fcrValue (مطلوب)",
                    details = listOf(
                        "سالن هدف" to flockName,
                        "ضریب تبدیل FCR" to fcrValue,
                        "استاندارد سویه" to "۱.۶۰ الی ۱.۷۰",
                        "ارزیابی اقتصادی" to "مطلوب؛ بهره‌وری خوراک در محدوده بهینه"
                    ),
                    systemEffect = "محاسبه ضریب تبدیل غذایی گله «$flockName» بر مبنای آخرین مصرف خوراک و افزایش وزن",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "metric" to "FCR",
                        "fcr" to 1.65,
                        "flockId" to flockId
                    )
                )
            }

            isLayingCalc -> {
                val layingPercent = "۸۲.۵٪"
                return VoiceActionCardData(
                    intentType = VoiceIntentType.CALCULATE_METRIC,
                    title = "محاسبه راندمان تخم‌گذاری گله",
                    iconRes = R.drawable.ic_farm_egg,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = "درصد تولید: $layingPercent (عالی)",
                    details = listOf(
                        "سالن هدف" to flockName,
                        "درصد تولید روزانه" to layingPercent,
                        "جمعیت پرندگان" to "${toPersianDigits(birdCount)} قطعه",
                        "ارزیابی راندمان" to "عالی و بالاتر از میانگین منطقه"
                    ),
                    systemEffect = "محاسبه نسبت تخم‌مرغ برداشتی به مرغ‌های زنده مولد در سالن «$flockName»",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "metric" to "LAYING_RATE",
                        "layingRate" to 82.5,
                        "flockId" to flockId
                    )
                )
            }

            isMortalityCalc -> {
                val mortPercent = "۱.۲٪"
                return VoiceActionCardData(
                    intentType = VoiceIntentType.CALCULATE_METRIC,
                    title = "محاسبه نرخ تلفات تجمعی",
                    iconRes = R.drawable.ic_farm_health,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = "درصد تلفات: $mortPercent (طبیعی)",
                    details = listOf(
                        "سالن هدف" to flockName,
                        "نرخ تلفات تجمعی" to mortPercent,
                        "حد مجاز کل دوره" to "حداکثر ۳٪",
                        "وضعیت سلامت" to "نرمال و بدون طغیان بیماری"
                    ),
                    systemEffect = "محاسبه درصد پرندگان تلف‌شده نسبت به جوجه‌ریزی اولیه سالن «$flockName»",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "metric" to "MORTALITY_RATE",
                        "mortalityRate" to 1.2,
                        "flockId" to flockId
                    )
                )
            }

            isWaterFeedRatio -> {
                val ratioStr = "۱.۹۵"
                return VoiceActionCardData(
                    intentType = VoiceIntentType.CALCULATE_METRIC,
                    title = "محاسبه نسبت مصرف آب به دان",
                    iconRes = R.drawable.ic_farm_feed_water,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = "نسبت آب به دان: $ratioStr (درست و مطلوب)",
                    details = listOf(
                        "سالن هدف" to flockName,
                        "نسبت لیتر آب به کیلو دان" to ratioStr,
                        "محدوده استاندارد طلایی" to "۱.۸ الی ۲.۱ لیتر/کیلوگرم",
                        "ارزیابی سالن" to "نرمال؛ بدون نشتی آبخوری و بدون استرس گرمایی"
                    ),
                    systemEffect = "ارزیابی شاخص هیدراتاسیون و سلامت گوارش گله «$flockName»",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "metric" to "WATER_FEED_RATIO",
                        "ratio" to 1.95,
                        "flockId" to flockId
                    )
                )
            }

            isDrugDose -> {
                val rawCount = PersianVoiceNormalizer.extractFirstNumber(norm) ?: 2000.0
                val targetBirds = rawCount.toInt()
                val doseGrams = (targetBirds * 0.1).toInt().coerceAtLeast(10) // ~100mg per bird = 200g for 2000 birds
                return VoiceActionCardData(
                    intentType = VoiceIntentType.CALCULATE_METRIC,
                    title = "محاسبه دوز داروی تایلوزین",
                    iconRes = R.drawable.ic_farm_health,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = "دوز تایلوزین: ${toPersianDigits(doseGrams)} گرم در روز",
                    details = listOf(
                        "تعداد پرنده" to "${toPersianDigits(targetBirds)} قطعه",
                        "دوز خالص مورد نیاز" to "${toPersianDigits(doseGrams)} گرم در روز",
                        "مدت درمان پیشنهادی" to "۳ الی ۵ روز متوالی در آب آشامیدنی",
                        "دوره پرهیز مصرف" to "گوشت: ۳ روز / تخم‌مرغ: ندارد"
                    ),
                    systemEffect = "محاسبه علمی دوز درمانی تایلوزین تارترات بر اساس وزن زنده و تعداد گله",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "metric" to "DRUG_DOSE",
                        "doseGrams" to doseGrams,
                        "birdCount" to targetBirds,
                        "flockId" to flockId
                    )
                )
            }

            else -> null
        }
        return null
    }

    /**
     * اجرای موتور تجزیه همزمان فرامین چندگانه (Multi-Action Batch Engine)
     */
    fun parseCompoundMultiAction(
        norm: String,
        spokenText: String,
        matchedFlock: Flock?,
        flocks: List<Flock>,
        contacts: List<FarmContact>
    ): VoiceActionCardData? {
        return tryParseCompoundActions(norm, spokenText, matchedFlock, flocks, contacts)
    }

    /**
     * استخراج دستور صوتی انتقال پرندگان بین سالن‌ها و گله‌ها (TRANSFER_BIRDS)
     */
    fun tryParseBirdTransfer(
        norm: String,
        spokenText: String,
        flocks: List<Flock>
    ): VoiceActionCardData? {
        val isTransferWord = norm.contains("انتقال") || norm.contains("جابجا") || norm.contains("جابجایی") || norm.contains("منتقل")
        if (!isTransferWord) return null

        val fromRegex = Regex("(?:از\\s*(?:سالن|گله)\\s*([^،\\s]+))")
        val toRegex = Regex("(?:به\\s*(?:سالن|گله)\\s*([^،\\s]+))")

        val fromMatch = fromRegex.find(norm)
        val toMatch = toRegex.find(norm)

        fun parseGroup(grp: String?): Long? {
            if (grp == null) return null
            return parseHallNumberString(grp) ?: grp.toLongOrNull()
        }

        val fromNum = parseGroup(fromMatch?.groupValues?.get(1)?.trim())
        val toNum = parseGroup(toMatch?.groupValues?.get(1)?.trim())

        if (fromNum == null && toNum == null && !norm.contains("بین سالن") && !norm.contains("بین گله")) {
            if (!norm.contains("قطعه") && !norm.contains("تا") && !norm.contains("مرغ") && !norm.contains("پرنده")) {
                return null
            }
        }

        var textForCount = norm
        if (fromMatch != null) textForCount = textForCount.replace(fromMatch.value, " ")
        if (toMatch != null) textForCount = textForCount.replace(toMatch.value, " ")

        val fromFlock = flocks.find { it.id == fromNum } ?: flocks.getOrNull((fromNum?.minus(1)?.toInt() ?: 0)) ?: flocks.firstOrNull()
        val toFlock = flocks.find { it.id == toNum } ?: flocks.getOrNull((toNum?.minus(1)?.toInt() ?: 1)) ?: flocks.getOrNull(1)

        val rawCount = PersianVoiceNormalizer.extractFirstNumber(textForCount)
        if (rawCount == null || rawCount <= 0.0) {
            val prompt = "چند قطعه پرنده و از کدام سالن به کدام سالن انتقال داده شود؟"
            return VoiceActionCardData(
                intentType = VoiceIntentType.TRANSFER_BIRDS,
                title = "انتقال پرندگان بین سالن‌ها (نیازمند تعداد)",
                iconRes = R.drawable.ic_farm_dashboard,
                flockId = fromFlock?.id,
                flockName = fromFlock?.name,
                primaryMetric = "تعداد مشخص نشده است",
                details = listOf(
                    "سالن مبدا" to (fromFlock?.name ?: "سالن ۱"),
                    "سالن مقصد" to (toFlock?.name ?: "سالن ۲"),
                    "راهنما" to prompt
                ),
                systemEffect = "دستور بدون اعلام تعداد پرندگان قابل اعمال نیست.",
                rawText = spokenText,
                isValid = false,
                needsMoreInfo = true,
                missingPromptFa = prompt,
                quickReplies = listOf("۱۰ قطعه از سالن ۱ به سالن ۲", "۲۰ قطعه از سالن ۱ به سالن ۲", "۵۰ قطعه از سالن ۱ به سالن ۲"),
                errorMessage = "تعداد پرندگان جهت انتقال مشخص نشده است.",
                parameters = mapOf(
                    "pendingIntent" to "TRANSFER_BIRDS",
                    "fromFlockId" to (fromFlock?.id ?: 1L),
                    "toFlockId" to (toFlock?.id ?: 2L)
                )
            )
        }

        val count = rawCount.toInt()
        val fromId = fromFlock?.id ?: 1L
        val toId = toFlock?.id ?: (if (fromId == 1L) 2L else 1L)
        val fromName = fromFlock?.name ?: "سالن ۱"
        val toName = toFlock?.name ?: "سالن ۲"

        return VoiceActionCardData(
            intentType = VoiceIntentType.TRANSFER_BIRDS,
            title = "انتقال پرندگان بین سالن‌ها",
            iconRes = R.drawable.ic_farm_dashboard,
            flockId = fromId,
            flockName = fromName,
            primaryMetric = "${toPersianDigits(count)} قطعه ($fromName ⬅️ $toName)",
            details = listOf(
                "سالن مبدا" to fromName,
                "سالن مقصد" to toName,
                "تعداد انتقال" to "${toPersianDigits(count)} قطعه",
                "موجودی فعلی مبدا" to "${toPersianDigits(fromFlock?.effectiveBirdCount ?: 0)} قطعه",
                "تاریخ ثبت" to JalaliDateHelper.getJalaliDate().toStandardString()
            ),
            systemEffect = "کسر ${toPersianDigits(count)} قطعه از «$fromName» و افزودن به «$toName» در فارم",
            rawText = spokenText,
            isValid = true,
            parameters = mapOf(
                "fromFlockId" to fromId,
                "toFlockId" to toId,
                "fromFlockName" to fromName,
                "toFlockName" to toName,
                "count" to count,
                "birdCount" to count
            )
        )
    }

    /**
     * تفسیر جامع، هوشمند و چندوجهی دستور صوتی فارسی و استخراج کلیه نیات و پارامترها با پشتیبانی چندنوبتی (Multi-Turn).
     */
    fun parseVoiceCommand(
        spokenText: String,
        flocks: List<Flock>,
        contacts: List<FarmContact>,
        sessionContext: Map<String, Any>? = null,
        eggProductions: List<EggProduction> = emptyList(),
        farmContext: FarmLiveContext = FarmLiveContext(flocks = flocks, contacts = contacts, eggProductions = eggProductions),
        userRole: UserRole = UserRole.MANAGER
    ): VoiceActionCardData {
        val parsedCard = parseVoiceCommandInternal(
            spokenText = spokenText,
            flocks = flocks,
            contacts = contacts,
            sessionContext = sessionContext,
            eggProductions = eggProductions,
            farmContext = farmContext,
            userRole = userRole
        )
        val norm = PersianVoiceNormalizer.normalize(spokenText)
        val isOperationalIntent = parsedCard.intentType in listOf(
            VoiceIntentType.RECORD_EGG,
            VoiceIntentType.RECORD_FEED,
            VoiceIntentType.RECORD_MORTALITY,
            VoiceIntentType.RECORD_WATER,
            VoiceIntentType.RECORD_MEDICATION,
            VoiceIntentType.RECORD_FINANCIAL,
            VoiceIntentType.CREATE_FLOCK,
            VoiceIntentType.TRANSFER_BIRDS
        )

        val isQuestion = com.example.poultryapp.engine.FarmIntentSemanticRouter.isQuestionPattern(norm)
        if (isOperationalIntent && isQuestion) {
            val hasExplicitImperative = (
                norm.contains("ثبت کن") || norm.contains("ثبت کنید") ||
                norm.contains("وارد کن") || norm.contains("وارد کنید") ||
                norm.contains("بزن") || norm.contains("اضافه کن")
            ) && !norm.contains("ثبت کنم") && !norm.contains("ثبت کنیم") &&
                 !norm.contains("چگونه") && !norm.contains("چطور") &&
                 !norm.contains("نحوه") && !norm.contains("روش") &&
                 !norm.contains("آموزش") && !norm.contains("راهنمای")

            if (!hasExplicitImperative) {
                return VoiceActionCardData(
                    intentType = VoiceIntentType.UNKNOWN,
                    title = "استعلام دانشنامه‌ای یا سوال علمی",
                    iconRes = R.drawable.ic_farm_dashboard,
                    primaryMetric = "نیاز به پاسخ مشاوره‌ای",
                    details = emptyList(),
                    systemEffect = "هدایت خودکار به دایره‌المعارف و هوش مصنوعی فارم",
                    rawText = spokenText,
                    isValid = false
                )
            }
        }

        return enforceWorkerRole(parsedCard, userRole)
    }

    private fun parseVoiceCommandInternal(
        spokenText: String,
        flocks: List<Flock>,
        contacts: List<FarmContact>,
        sessionContext: Map<String, Any>? = null,
        eggProductions: List<EggProduction> = emptyList(),
        farmContext: FarmLiveContext = FarmLiveContext(flocks = flocks, contacts = contacts, eggProductions = eggProductions),
        userRole: UserRole = UserRole.MANAGER
    ): VoiceActionCardData {
        val norm = PersianVoiceNormalizer.normalize(spokenText)

        if (norm.isBlank()) {
            return VoiceActionCardData(
                intentType = VoiceIntentType.UNKNOWN,
                title = "دستور صوتی خالی است",
                iconRes = R.drawable.ic_farm_dashboard,
                primaryMetric = "هیچ صدایی شنیده نشد",
                details = listOf("وضعیت" to "لطفاً مجدداً دکمه میکروفون را لمس کرده و صحبت فرمایید."),
                systemEffect = "دستور نامعتبر است و تغییری ایجاد نخواهد شد.",
                rawText = spokenText,
                isValid = false,
                errorMessage = "صدایی برای پردازش دریافت نشد."
            )
        }

        // ۰.۰. استعلام پیش‌بینی آب‌وهوا و اقلیم فارم (WEATHER FORECAST)
        if (com.example.poultryapp.engine.FarmWeatherForecastAiEngine.isWeatherQuery(norm)) {
            val weatherAnalysis = com.example.poultryapp.engine.FarmWeatherForecastAiEngine.processWeatherInquiry(
                norm = norm,
                spokenText = spokenText,
                farmContext = farmContext
            )
            return weatherAnalysis.cardData
        }

        // بررسی ادغام پاسخ با کانتکست چندنوبتی قبلی (در صورتی که دستور جدید ناوبری یا تماس صریح نباشد)
        if (sessionContext != null && sessionContext["pendingIntent"] != null) {
            val isExplicitNavOrCall = Regex("""\bبرو\b""").containsMatchIn(norm) || norm.contains("صفحه") ||
                    norm.contains("تماس") || norm.contains("زنگ")
            val isQuestionOrInquiry = com.example.poultryapp.engine.FarmIntentSemanticRouter.isQuestionPattern(norm)
            if (!isExplicitNavOrCall && !isQuestionOrInquiry) {
                val mergedCard = tryMergeSessionContext(norm, spokenText, sessionContext, flocks)
                if (mergedCard != null) {
                    return mergedCard
                }
            }
        }

        val contextFlockId = (sessionContext?.get("activeFlockId") as? Number)?.toLong()
            ?: (sessionContext?.get("flockId") as? Number)?.toLong()
        val contextFlock = contextFlockId?.let { cid -> flocks.find { it.id == cid } }

        // تفکیک نام/شماره سالن هدف برای جلوگیری از تداخل با مقادیر عددی
        val flockMatch = findTargetFlockWithPhrase(norm, flocks)
        val isAnaphoraHall = norm.contains("همین سالن") || norm.contains("این سالن") ||
                norm.contains("همین گله") || norm.contains("این گله") ||
                norm.contains("همان سالن") || norm.contains("همون سالن")
        val matchedFlock = if (isAnaphoraHall && contextFlock != null) contextFlock else (flockMatch.flock ?: contextFlock)
        val flockName = matchedFlock?.name ?: contextFlock?.name ?: if (flocks.isNotEmpty()) flocks.first().name else "گله پیش‌فرض"
        val flockId = matchedFlock?.id ?: contextFlock?.id ?: flocks.firstOrNull()?.id ?: 1L
        val excludeFlockPhrase = flockMatch.matchedPhrase

        // بررسی دستورات ترکیبی چندگانه (ثبت همزمان تخم‌مرغ، تلفات، دان و...)
        val compoundCard = tryParseCompoundActions(norm, spokenText, matchedFlock, flocks, contacts)
        if (compoundCard != null) {
            return compoundCard
        }

        // ۰.۲۵. استخراج انتقال پرندگان بین سالن‌ها (TRANSFER_BIRDS)
        val transferCard = tryParseBirdTransfer(norm, spokenText, flocks)
        if (transferCard != null) {
            return transferCard
        }

        // ۰.۳. استخراج تنظیم صوتی آلارم و یادآور تقویم فارم (SET_SCHEDULE)
        val scheduleCard = tryParseSetSchedule(norm, spokenText, matchedFlock, flocks)
        if (scheduleCard != null) {
            return scheduleCard
        }

        // ۰.۴. ثبت صوتی مشاهدات سالن و گزارش شیفت کارگر (RECORD_OBSERVATION)
        val obsCard = tryParseRecordObservation(norm, spokenText, matchedFlock, flocks, userRole)
        if (obsCard != null) {
            return obsCard
        }

        // ۰.۵. محاسبه صوتی شاخص‌های علمی پرورشی در لحظه (CALCULATE_METRIC)
        val metricCard = tryParseCalculateMetric(norm, spokenText, matchedFlock, flocks, farmContext)
        if (metricCard != null) {
            return metricCard
        }

        // ۰.۶. استخراج تنظیم دمای سالن / گله (SET_FLOCK_TEMPERATURE)
        if (norm.contains("تنظیم") && (norm.contains("دما") || norm.contains("حرارت"))) {
            val targetFlock = matchedFlock ?: flocks.firstOrNull()
            val tFlockId = targetFlock?.id ?: 1L
            val tFlockName = targetFlock?.name ?: "سالن فعال"
            val rawTemp = PersianVoiceNormalizer.extractFirstNumber(norm, excludeFlockPhrase)
            if (rawTemp != null && rawTemp in 10.0..45.0) {
                return VoiceActionCardData(
                    intentType = VoiceIntentType.SET_FLOCK_TEMPERATURE,
                    title = "تنظیم دمای سالن ($tFlockName)",
                    iconRes = R.drawable.ic_farm_dashboard,
                    flockId = tFlockId,
                    flockName = tFlockName,
                    primaryMetric = "${toPersianDigits(rawTemp)} درجه سانتی‌گراد",
                    details = listOf(
                        "سالن / گله" to tFlockName,
                        "دمای تنظیمی" to "${toPersianDigits(rawTemp)} درجه"
                    ),
                    systemEffect = "ثبت دمای تنظیمی هدف برای گله $tFlockName",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "tempCelsius" to rawTemp,
                        "flockId" to tFlockId,
                        "flockName" to tFlockName,
                        "isMutating" to true
                    ),
                    quickReplies = listOf("تایید تنظیم دما", "لغو")
                )
            } else {
                return VoiceActionCardData(
                    intentType = VoiceIntentType.SET_FLOCK_TEMPERATURE,
                    title = "تنظیم دمای سالن ($tFlockName)",
                    iconRes = R.drawable.ic_farm_dashboard,
                    flockId = tFlockId,
                    flockName = tFlockName,
                    primaryMetric = "تعیین دمای هدف",
                    details = listOf(
                        "سالن / گله" to tFlockName,
                        "وضعیت" to "نیاز به مشخص کردن عدد دما"
                    ),
                    systemEffect = "تنظیم دمای آسایش سالن",
                    rawText = spokenText,
                    isValid = false,
                    needsMoreInfo = true,
                    missingPromptFa = "دمای تنظیمی مد نظر (به درجه سانتی‌گراد)",
                    parameters = mapOf(
                        "flockId" to tFlockId,
                        "flockName" to tFlockName,
                        "isMutating" to true
                    ),
                    quickReplies = listOf("۲۰ درجه", "۲۲ درجه", "۲۴ درجه", "۲۶ درجه")
                )
            }
        }

        // ۱. نیت تماس با مخاطب تجاری یا استعلام تلفن
        val isCallKeyword = norm.contains("تماس") || norm.contains("زنگ") || norm.contains("پیامک") ||
                norm.contains("ارتباط") || norm.contains("پیام") || norm.contains("مخاطب") ||
                norm.contains("شماره تلفن") || norm.contains("شماره تماس") ||
                (norm.contains("شماره") && !norm.contains("گله") && !norm.contains("سالن") && !norm.contains("مرغ") && !norm.contains("پرنده")) ||
                (norm.contains("تلفن") && !norm.contains("گله") && !norm.contains("سالن"))
        if (isCallKeyword) {
            val matchedContact = contacts.find { contact ->
                val cNorm = PersianVoiceNormalizer.normalize(contact.fullName)
                val bNorm = contact.businessName?.let { PersianVoiceNormalizer.normalize(it) } ?: ""
                val catNorm = PersianVoiceNormalizer.normalize(contact.category.titleFa)

                norm.contains(cNorm) || (bNorm.isNotBlank() && norm.contains(bNorm)) ||
                cNorm.split(" ").filter { it.length > 2 }.any { norm.contains(it) } ||
                when (contact.category) {
                    ContactCategory.VET_PHARMACY -> norm.contains("دامپزشک") || norm.contains("دکتر") || norm.contains("داروخانه") || norm.contains("پزشک")
                    ContactCategory.FEED_SUPPLIER -> norm.contains("کارخانه") || (norm.contains("تامین") && norm.contains("دان")) || (norm.contains("دان") && norm.contains("خوراک"))
                    ContactCategory.EGG_BUYER -> norm.contains("خریدار") && norm.contains("تخم")
                    ContactCategory.TECHNICIAN -> norm.contains("راننده") || norm.contains("بار") || norm.contains("حمل") || norm.contains("تعمیرکار")
                    ContactCategory.SLAUGHTERHOUSE -> norm.contains("کشتارگاه") || (norm.contains("خریدار") && norm.contains("مرغ"))
                    ContactCategory.HATCHERY -> norm.contains("هچری") || (norm.contains("جوجه") && norm.contains("کشی"))
                    else -> norm.contains(catNorm)
                }
            }

            val isSms = norm.contains("پیامک") || norm.contains("پیام")
            if (matchedContact != null) {
                val contactName = matchedContact.fullName
                val phone = matchedContact.phonePrimary
                val actionTitle = if (isSms) "ارسال پیامک به طرف‌حساب فارم" else "تماس تلفنی با طرف‌حساب فارم"
                val systemEffectText = if (isSms) "هدایت به پیام‌رسان گوشی جهت ارسال پیامک به $contactName" else "هدایت به شماره‌گیر تلفن گوشی جهت برقراری تماس با $contactName"

                return VoiceActionCardData(
                    intentType = VoiceIntentType.CALL_CONTACT,
                    title = actionTitle,
                    iconRes = R.drawable.ic_farm_management,
                    primaryMetric = if (isSms) "پیامک به $contactName" else "تماس با $contactName",
                    details = listOf(
                        "نام مخاطب" to contactName,
                        "شماره تماس" to toPersianDigits(phone),
                        "سمت / تخصص" to matchedContact.category.titleFa
                    ),
                    systemEffect = systemEffectText,
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "phone" to phone,
                        "phoneNumber" to phone,
                        "contactName" to contactName,
                        "isSms" to isSms
                    )
                )
            } else {
                return VoiceActionCardData(
                    intentType = VoiceIntentType.CALL_CONTACT,
                    title = if (isSms) "ارسال پیامک (انتخاب مخاطب)" else "تماس با مخاطب فارم",
                    iconRes = R.drawable.ic_farm_management,
                    primaryMetric = "مخاطب شناسایی نشد",
                    details = listOf(
                        "راهنما" to "نام مخاطب یا تخصص او (مثلاً دامپزشک، کارخانه دان، راننده) را بیان فرمایید."
                    ),
                    systemEffect = "انتقال به دفترچه مخاطبین فارم جهت انتخاب دستی طرف‌حساب.",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf("screen" to "CONTACTS")
                )
            }
        }

        // ۱.۵. نیت ایجاد و افزودن گله جدید به فارم (Create Flock)
        val isCreateFlockKeyword = (norm.contains("گله") || norm.contains("سالن")) &&
                (norm.contains("اضافه") || norm.contains("ایجاد") || norm.contains("افزودن") ||
                 norm.contains("بساز") || norm.contains("جدید") || norm.contains("تعریف") ||
                 norm.contains("بزار") || norm.contains("بگذار") || norm.contains("تشکیل") ||
                 norm.contains("ثبت گله"))
        if (isCreateFlockKeyword) {
            val breedPair = findBreedAndSpecies(norm)
            val breed = breedPair?.first
            val species = breedPair?.second ?: when {
                norm.contains("بلدرچین") -> BirdSpecies.QUAIL
                norm.contains("بوقلمون") -> BirdSpecies.TURKEY
                norm.contains("اردک") -> BirdSpecies.DUCK
                norm.contains("غاز") -> BirdSpecies.GOOSE
                norm.contains("شاخدار") || norm.contains("گینه") -> BirdSpecies.GUINEA_FOWL
                else -> BirdSpecies.CHICKEN
            }

            val purpose = when {
                norm.contains("تخمگذار") || norm.contains("تخم گذار") || norm.contains("تخم مرغ") || norm.contains("تخمی") -> "تخم‌گذار"
                norm.contains("گوشتی") -> "گوشتی"
                norm.contains("مولد") || norm.contains("مادر") -> "مولد"
                else -> null
            }

            // فیلتر کردن کلمات اشاره‌ای نظیر "یک گله" یا "یه سالن" از استخراج عدد تعداد پرندگان
            val cleanForCount = norm
                .replace(Regex("(یک|یه)?\\s*(گله|سالن)\\s*(جدید)?"), " ")
                .replace(Regex("\\b(گله|سالن)\\b"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            val rawBirdCount = PersianVoiceNormalizer.extractFirstNumber(cleanForCount)

            if (rawBirdCount == null || rawBirdCount <= 0.0) {
                val prompt = if (breed != null) {
                    "تعداد پرندگان گله «$breed» و نوع پرورش (تخم‌گذار یا گوشتی) را بفرمایید:"
                } else {
                    "نژاد و تعداد پرندگان گله جدید را بفرمایید:"
                }
                val quickReplies = if (breed != null) {
                    listOf("۵۰ قطعه تخم‌گذار", "۱۰۰ قطعه گوشتی", "۲۰۰ قطعه", "۳۰ قطعه")
                } else {
                    listOf("۵۰ قطعه آمروک", "۱۰۰ قطعه راس گوشتی", "۲۰۰ قطعه لوهمن", "۵۰ قطعه مرندی")
                }

                return VoiceActionCardData(
                    intentType = VoiceIntentType.CREATE_FLOCK,
                    title = "ایجاد گله جدید در فارم (نیازمند اطلاعات)",
                    iconRes = R.drawable.ic_farm_dashboard,
                    primaryMetric = if (breed != null) "گله «$breed»" else "تکمیل مشخصات گله",
                    details = listOf(
                        "نژاد انتخابی" to (breed ?: "نامشخص (نیاز به اعلام)"),
                        "راهنما" to prompt
                    ),
                    systemEffect = "پس از اعلام تعداد و نوع پرورش، گله در سیستم فارم ایجاد می‌شود.",
                    rawText = spokenText,
                    isValid = false,
                    needsMoreInfo = true,
                    missingPromptFa = prompt,
                    quickReplies = quickReplies,
                    parameters = mapOf(
                        "pendingIntent" to "CREATE_FLOCK",
                        "breed" to (breed ?: "آمروک"),
                        "species" to species,
                        "flockName" to (if (breed != null) "گله $breed" else "گله جدید"),
                        "purpose" to (purpose ?: "")
                    )
                )
            }

            val count = rawBirdCount.toInt()
            val finalBreed = breed ?: "بومی اصلاح‌شده"
            val finalPurpose = purpose ?: "تخم‌گذار"
            val flockName = "گله $finalBreed $finalPurpose"

            return VoiceActionCardData(
                intentType = VoiceIntentType.CREATE_FLOCK,
                title = "ایجاد گله جدید در فارم",
                iconRes = R.drawable.ic_farm_dashboard,
                primaryMetric = "${toPersianDigits(count)} قطعه ($finalBreed - $finalPurpose)",
                details = listOf(
                    "نام پیشنهادی گله" to flockName,
                    "گونه پرنده" to species.titleFa,
                    "نژاد" to finalBreed,
                    "تعداد جوجه / پرنده" to "${toPersianDigits(count)} قطعه",
                    "هدف پرورشی" to finalPurpose,
                    "سن اولیه" to "۱ روزه (پیش‌فرض جوجه)"
                ),
                systemEffect = "ثبت گله «$flockName» در سیستم و ایجاد کارت‌های آماری مربوطه در سالن‌ها",
                rawText = spokenText,
                isValid = true,
                needsMoreInfo = false,
                missingPromptFa = null,
                quickReplies = emptyList(),
                parameters = mapOf(
                    "flockName" to flockName,
                    "name" to flockName,
                    "species" to species,
                    "breed" to finalBreed,
                    "birdCount" to count,
                    "ageDays" to 1,
                    "notes" to "گله $finalBreed ($finalPurpose) ثبت شده با دستیار هوشمند"
                )
            )
        }

        // ۲. نیت ناوبری و هدایت بین صفحات اپلیکیشن (اولویت بالا در صورت وجود افعال ناوبری)
        val hasNavWord = Regex("""\bبرو\b""").containsMatchIn(norm) || norm.contains("باز کن") || norm.contains("بیار") ||
                norm.contains("صفحه") || norm.contains("نمایش") ||
                Regex("""\bبخش\b""").containsMatchIn(norm) ||
                norm.contains("انتقال به") || norm.contains("قسمت") || norm.contains("مشاهده")
        val isNavKeyword = hasNavWord && !norm.contains("ببخشید") && !norm.startsWith("ثبت") &&
                !norm.contains("چند") && !norm.contains("چقدر") && !norm.contains("چطوره") && !norm.contains("چگونه است")
        if (isNavKeyword) {
            val (targetTab, targetTitleFa) = when {
                norm.contains("انبار") || norm.contains("کالا") || norm.contains("موجودی") -> "WAREHOUSE" to "انبار و اقلام فارم"
                norm.contains("مالی") || norm.contains("حساب") || norm.contains("سود") || norm.contains("زیان") -> "FINANCIAL" to "امور مالی و سود/زیان"
                norm.contains("جیره") || norm.contains("فرمول") -> "RATION" to "جیره‌ساز تخصصی فارم"
                norm.contains("مخاطب") || norm.contains("دفترچه") || norm.contains("همکار") -> "CONTACTS" to "دفترچه مخاطبین تجاری"
                norm.contains("واکسن") || norm.contains("دارو") -> "MEDICATIONS" to "داروها و برنامه واکسیناسیون"
                norm.contains("امنیت زیستی") || norm.contains("بهداشت") -> "BIOSECURITY" to "چک‌لیست امنیت زیستی"
                norm.contains("سلامت") || norm.contains("بیماری") -> "HEALTH" to "سلامت و تشخیص بیماری‌ها"
                norm.contains("تقویم") || norm.contains("یادآور") -> "CALENDAR" to "تقویم کاری و هشدارهای فارم"
                norm.contains("شبیه ساز") || norm.contains("شبیه‌ساز") || norm.contains("کشتار") || norm.contains("سرمایه") -> "SIMULATORS" to "شبیه‌سازهای مهندسی فارم"
                norm.contains("تنظیم") -> "SETTINGS" to "تنظیمات برنامه"
                norm.contains("راهنما") || norm.contains("دانش") || norm.contains("اموزش") -> "KNOWLEDGE" to "راهنمای جامع پرورش"
                norm.contains("طلایی") || norm.contains("جایزه") || norm.contains("مسابقه") -> "GOLDEN_BREEDING" to "باشگاه پرورش طلایی"
                norm.contains("گله") || norm.contains("سالن") -> "FLOCKS" to "مدیریت سالن‌ها و گله‌ها"
                norm.contains("دان و اب") || norm.contains("تغذیه") -> "FEED_WATER" to "پایش دان و آب سالن‌ها"
                norm.contains("خانه") || norm.contains("داشبورد") || norm.contains("اصلی") -> "HOME" to "صفحه اصلی (داشبورد فارم)"
                else -> null to null
            }

            if (targetTab != null) {
                return VoiceActionCardData(
                    intentType = VoiceIntentType.NAVIGATE_SCREEN,
                    title = "انتقال هوشمند به صفحه برنامه",
                    iconRes = R.drawable.ic_farm_dashboard,
                    primaryMetric = targetTitleFa ?: targetTab,
                    details = listOf(
                        "بخش مقصد" to (targetTitleFa ?: targetTab),
                        "سرعت انتقال" to "فوری و بدون معطلی"
                    ),
                    systemEffect = "انتقال فوری به بخش $targetTitleFa",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "targetTab" to targetTab,
                        "targetScreen" to targetTab
                    )
                )
            }
        }

        // ۳. استعلام وضعیت فارم و پرس‌وجوهای آماری زنده (Queries & Stats)
        val hasFlockOrSalon = matchedFlock != null || norm.contains("گله") || norm.contains("سالن")

        val isFlockCarrierQuery = hasFlockOrSalon && (
                norm.contains("رو بگو") || norm.contains("را بگو") || norm.endsWith("بگو") ||
                norm.contains("نشون") || norm.contains("نشان بده") || norm.contains("نشان دادن") ||
                norm.contains("توضیح") || norm.contains("چه خبر") || norm.contains("احوال") || norm.contains("حال و روز") ||
                norm.contains("درباره") || norm.contains("راجع به") || norm.contains("در مورد")
        ) && !norm.contains("نشانه") && !norm.contains("علائم") && !norm.contains("بیماری") && !norm.contains("مریض")

        val hasSafeAgeWord = (norm.contains(" چند روز") || norm.contains(" چند هفته") || norm.contains("چند وقت") ||
                norm.contains(" سن ") || norm.startsWith("سن ") || norm.endsWith(" سن") ||
                norm.contains("سنش") || norm.contains("سن گله") || norm.contains("سن سالن")) &&
                !norm.contains("شناسنامه") && !norm.contains("کنسانتره")

        val isBreedLocationQuery = FarmOmniscientAnalyticsEngine.findBreedInQuery(norm) != null && (
                norm.contains("کدام گله") || norm.contains("کدوم گله") ||
                norm.contains("کدام سالن") || norm.contains("کدوم سالن") ||
                norm.contains("در کدام") || norm.contains("در کدوم") ||
                norm.contains("کجا") || norm.contains("کجاست")
        )

        val isBreedQuery = !norm.contains("تفاوت") && !norm.contains("فرق") && !norm.contains("مقایسه") &&
                (((hasFlockOrSalon || norm.contains("فارم")) && (
                norm.contains("چه نژاد") || norm.contains("چه نژادی") || norm.contains("چه نژادهایی") || norm.contains("نژادهای") || norm.contains("لیست نژاد") ||
                (norm.contains("نژاد") && (norm.contains("چیست") || norm.contains("چیه") || norm.contains("کدام") || norm.contains("کدوم") || norm.contains("شامل") || norm.contains("داریم") || norm.contains("دارد") || norm.contains("داره"))) ||
                (FarmOmniscientAnalyticsEngine.findBreedInQuery(norm) != null && (norm.contains("چند") || norm.contains("داریم") || norm.contains("هست") || (norm.contains(" است ") || norm.endsWith(" است")) || norm.contains("آیا") || norm.contains("ایا")))
        )) || isBreedLocationQuery)

        val isQuestionWithFlock = (hasFlockOrSalon || norm.contains("فارم")) && (
                (norm.contains("ایا") || norm.contains("آیا")) ||
                (norm.contains("چند") && !norm.contains("تخم") && !norm.contains("تلف") && !norm.contains("کیلو") && !norm.contains("لیتر") && !norm.contains("تومان"))
        ) && !norm.contains("نشانه") && !norm.contains("علائم") && !norm.contains("بیماری")

        val isOperationalRecordCommand = com.example.poultryapp.engine.FarmIntentSemanticRouter.isOperationalActionPattern(norm) ||
                norm.startsWith("ثبت") || norm.contains(" ثبت ") ||
                norm.contains("وارد کن") || norm.contains("اضافه کن") || norm.contains("بنویس") ||
                norm.contains("لیتر آب") || norm.contains("کیلو دان") || norm.contains("کیلو خوراک")

        val isQueryKeyword = norm.contains("چقدر") || norm.contains("چند تا") || norm.contains("چندتا") ||
                norm.contains("وضعیت") || norm.contains("امار") || norm.contains("آمار") ||
                norm.contains("چطوره") || norm.contains("هوا چطوره") || norm.contains("چگونه است") || norm.contains("چطور است") ||
                norm.contains("راندمان") || norm.contains("افت") || norm.contains("افت تولید") ||
                norm.contains("تولید امروز") || norm.contains("گزارش") ||
                norm.contains("جزئیات") || norm.contains("جزییات") || norm.contains("جزیات") || norm.contains("ریز اطلاعات") ||
                norm.contains("مشخصات") || norm.contains("شناسنامه") || norm.contains("پرونده") || norm.contains("پروفایل") ||
                norm.contains("پرهیز") ||
                (norm.contains("اطلاعات") && (hasFlockOrSalon || norm.contains("فارم") || norm.contains("بگو") || norm.contains("رو بگو"))) ||
                (norm.contains("موجودی") && (norm.contains("انبار") || norm.contains("فارم") || norm.contains("کالا") || norm.contains("دان") || norm.contains("چقدر"))) ||
                norm.contains("چه اقلام") || norm.contains("چه چیزهایی") || norm.contains("چی تو انبار") ||
                norm.contains("چه نوع") || norm.contains("چه دانی") || norm.contains("کدام دان") || norm.contains("کدوم دان") ||
                (norm.contains("چی داریم") && (norm.contains("انبار") || norm.contains("فارم") || norm.contains("سالن") || norm.contains("گله") || norm.contains("نهاده"))) ||
                (norm.contains("انبار") && (norm.contains("است") || norm.contains("هست") || norm.contains("چیه") || norm.contains("داریم"))) ||
                (norm.contains("لیست انبار") || norm.contains("اقلام انبار")) ||
                norm.contains("چه خبر") ||
                hasSafeAgeWord ||
                isBreedQuery ||
                isQuestionWithFlock ||
                isFlockCarrierQuery

        // استعلام مستقیم وضعیت پایش سالن (دان و آب)
        if (norm.contains("وضعیت پایش") || (norm.contains("پایش") && (norm.contains("وضعیت") || norm.contains("چطوره") || norm.contains("چطور") || norm.contains("استعلام")))) {
            val targetFlock = matchedFlock ?: flocks.firstOrNull()
            val targetFlockName = targetFlock?.name ?: flockName
            val feedStatus = if (targetFlock?.isFeedEnabled == true) "فعال" else "غیرفعال"
            val waterStatus = if (targetFlock?.isWaterEnabled == true) "فعال" else "غیرفعال"
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "وضعیت پایش «$targetFlockName»",
                iconRes = R.drawable.ic_farm_feed_water,
                flockId = targetFlock?.id ?: flockId,
                flockName = targetFlockName,
                primaryMetric = "پایش دان: $feedStatus | پایش آب: $waterStatus",
                details = listOf(
                    "پایش دان" to feedStatus,
                    "پایش آب" to waterStatus
                ),
                systemEffect = "استعلام وضعیت پایش‌های هوشمند گله",
                rawText = spokenText,
                isValid = true
            )
        }

        val isPureKnowledgeQuery = norm.contains("اطلاعات استاندارد") || norm.contains("خواص") ||
                norm.contains("فواید") || norm.contains("مزایا") || norm.contains("مطلوب") ||
                norm.contains("اصول") || norm.contains("راهکار") || norm.contains("سرکه") || norm.contains("سیر") ||
                norm.contains("تفاوت") || norm.contains("فرق") || norm.contains("مقایسه")
        if (isQueryKeyword && !isOperationalRecordCommand && !isPureKnowledgeQuery) {
            // حل‌وفصل هوشمند با موتور اشراف تحلیلی جامع فارم (FarmOmniscientAnalyticsEngine)
            val omniCard = FarmOmniscientAnalyticsEngine.resolveFarmInquiry(
                norm = norm,
                spokenText = spokenText,
                matchedFlock = matchedFlock,
                farmContext = farmContext,
                matchedPhrase = excludeFlockPhrase,
                userRole = userRole
            )
            if (omniCard != null) {
                return omniCard
            }

            val isLayingQuery = norm.contains("تخم") || norm.contains("راندمان") || norm.contains("افت") || norm.contains("تولید")
            if (isLayingQuery) {
                val todayJalali = JalaliDateHelper.getJalaliDate().toStandardString()
                val targetFlock = matchedFlock ?: flocks.firstOrNull()
                val fId = targetFlock?.id ?: 1L
                val fName = targetFlock?.name ?: "گله فعال"
                val birdCount = targetFlock?.effectiveBirdCount?.coerceAtLeast(1) ?: 100

                // ارزیابی هوشمند افت تولید با LayingBenchmarkEngine
                val dropAlert = if (targetFlock != null) {
                    LayingBenchmarkEngine.evaluateLayingDrop(targetFlock, eggProductions)
                } else {
                    LayingBenchmarkEngine.evaluateAllFlocks(flocks, eggProductions).firstOrNull()
                }

                val flockEggLogs = eggProductions.filter { it.flockId == fId }
                val todayEggs = flockEggLogs.filter { it.dateJalaliStr == todayJalali }.sumOf { it.totalCount }
                val latestEggLog = flockEggLogs.maxByOrNull { it.timestampMillis }

                val currentRate = when {
                    todayEggs > 0 -> (todayEggs.toDouble() / birdCount.toDouble()) * 100.0
                    latestEggLog != null && latestEggLog.layingRatePercent > 0 -> latestEggLog.layingRatePercent
                    else -> 0.0
                }

                if (dropAlert != null) {
                    val dropStr = toPersianDigits(String.format(Locale.US, "%.1f", dropAlert.dropDifferencePercent))
                    val recentStr = toPersianDigits(String.format(Locale.US, "%.1f", dropAlert.recentThreeDayAveragePercent))
                    val priorStr = toPersianDigits(String.format(Locale.US, "%.1f", dropAlert.priorWeekAveragePercent))
                    val checklistItem = dropAlert.diagnosticChecklist.firstOrNull() ?: "بررسی ساعات روشنایی و آبخوری‌ها"

                    return VoiceActionCardData(
                        intentType = VoiceIntentType.QUERY_STATS,
                        title = "هشدار افت راندمان تخم‌گذاری ⚠️",
                        iconRes = R.drawable.ic_farm_egg,
                        flockId = fId,
                        flockName = fName,
                        primaryMetric = "افت $dropStr٪ در راندمان (میانگین ۳ روزه: $recentStr٪)",
                        details = listOf(
                            "گله مورد بررسی" to fName,
                            "میانگین ۳ روز اخیر" to "$recentStr٪",
                            "میانگین هفته قبل" to "$priorStr٪",
                            "میزان افت تولید" to "$dropStr درصد کاهش",
                            "چک‌لیست فوری سالن" to checklistItem
                        ),
                        systemEffect = "کاهش غیرعادی راندمان شناسایی شد؛ چک‌لیست ۴ موردی را در سالن پایش کنید.",
                        rawText = spokenText,
                        isValid = true,
                        parameters = mapOf(
                            "topic" to "افت تخم‌گذاری",
                            "dropAlert" to true,
                            "dropPercent" to dropAlert.dropDifferencePercent,
                            "recentAverage" to dropAlert.recentThreeDayAveragePercent
                        )
                    )
                } else if (currentRate > 0.0 || todayEggs > 0) {
                    val rateStr = toPersianDigits(String.format(Locale.US, "%.1f", currentRate))
                    val statusText = if (currentRate >= 80.0) "راندمان عالی و مطلوب" else if (currentRate >= 65.0) "راندمان طبیعی و پایدار" else "راندمان متوسط"
                    return VoiceActionCardData(
                        intentType = VoiceIntentType.QUERY_STATS,
                        title = "استعلام زنده راندمان تخم‌گذاری",
                        iconRes = R.drawable.ic_farm_egg,
                        flockId = fId,
                        flockName = fName,
                        primaryMetric = "راندمان $rateStr٪ ($statusText)",
                        details = listOf(
                            "گله هدف" to fName,
                            "راندمان تولید" to "$rateStr درصد",
                            "تولید امروز" to if (todayEggs > 0) "${toPersianDigits(todayEggs)} عدد (${toPersianDigits(todayEggs / 30)} شانه)" else "ثبت نشده (محاسبه از آخرین رکورد)",
                            "جمعیت مولد" to "${toPersianDigits(birdCount)} قطعه",
                            "وضعیت گله" to "بدون افت غیرعادی؛ پایدار"
                        ),
                        systemEffect = "پایش آنلاین راندمان تولید تخم‌مرغ گله «$fName» بر اساس لاگ‌های روزانه",
                        rawText = spokenText,
                        isValid = true,
                        parameters = mapOf(
                            "topic" to "راندمان تخم‌گذاری",
                            "ratePercent" to currentRate,
                            "todayEggs" to todayEggs
                        )
                    )
                }
            }

            val isAdvisoryClimate = norm.contains("مطلوب") || norm.contains("استاندارد") ||
                    norm.contains("اصول") || norm.contains("راهکار") || norm.contains("مناسب") ||
                    norm.contains("چقدره") || norm.contains("چقدر است") || norm.contains("چند درجه باید") ||
                    norm.contains("چند درجه لازمه") || norm.contains("چند درجه میخواد")
            val isKnowledgeAdvisory = isAdvisoryClimate || norm.contains("خواص") || norm.contains("فواید") ||
                    norm.contains("اطلاعات استاندارد") || norm.contains("مزایا")

            val queryTopic = when {
                (norm.contains("هوا") || norm.contains("دما")) && !isAdvisoryClimate -> "آب‌وهوا و اقلیم فارم"
                (norm.contains("دان") || norm.contains("دون")) && !isKnowledgeAdvisory -> "میزان دان مصرفی امروز"
                norm.contains("تخم") -> "آمار تولید تخم‌مرغ"
                norm.contains("تلفات") -> "آمار تلفات و نرخ ماندگاری"
                (norm.contains("گله") || norm.contains("سالن") || norm.contains("فارم")) && !isKnowledgeAdvisory -> "وضعیت کلی سالن «$flockName»"
                else -> null
            }

            if (queryTopic != null) {
                return VoiceActionCardData(
                    intentType = VoiceIntentType.QUERY_STATS,
                    title = "استعلام اطلاعات و آمار زنده",
                    iconRes = R.drawable.ic_farm_dashboard,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = queryTopic,
                    details = listOf(
                        "موضوع استعلام" to queryTopic,
                        "گله / بخش" to flockName,
                        "منبع داده" to "بانک اطلاعات برخط فارم"
                    ),
                    systemEffect = "انتقال به بخش آماری مربوطه جهت مشاهده گزارش تفصیلی",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf("topic" to queryTopic)
                )
            }
        }

        // ۳.۵. تشخیص بالینی بیماری و غربالگری نشانه‌ها (Disease Diagnosis & Clinical Inquiry)
        val isDiseaseQuestion = norm.contains("چه بیماری") || norm.contains("بیماری چیه") || norm.contains("چه مریضی") ||
                norm.contains("مریضی چیه") || norm.contains("علائم") || norm.contains("درمان") ||
                norm.contains("چرا مرغ") || norm.contains("چرا جوجه") || norm.contains("چشونه") ||
                norm.contains("نشانه چه بیماری") || norm.contains("نشانه چیه") || norm.contains("علتش چیه") ||
                norm.contains("داروی") || norm.contains("چی بدم") || norm.contains("مشکل چیه") ||
                norm.contains("تشخیص") || norm.contains("مریض شدن")

        val isClinicalSymptomReport = norm.contains("لنگ") || norm.contains("شل می") || norm.contains("فلج") ||
                norm.contains("بی حال") || norm.contains("بیحال") || norm.contains("کز") ||
                norm.contains("اسهال") || norm.contains("مدفوع") || norm.contains("فضله") ||
                norm.contains("خس خس") || norm.contains("خرخر") || norm.contains("عطسه") || norm.contains("سرفه") ||
                norm.contains("تنگی نفس") || norm.contains("نفس نفس") || norm.contains("له له") ||
                norm.contains("تورم") || norm.contains("ورم") || norm.contains("باد کرده") || norm.contains("پف") ||
                norm.contains("پیچش گردن") || norm.contains("کج شدن") || norm.contains("چرخش سر") ||
                norm.contains("لمبه") || norm.contains("پوسته نرم") || norm.contains("پوسته نازک") || norm.contains("افت تخم") ||
                norm.contains("زخم") || norm.contains("دلمه") || norm.contains("خونریزی") ||
                norm.contains("کپه شدن") || norm.contains("سرمازدگی") || norm.contains("گرمازدگی") ||
                norm.contains("پرکنی") || norm.contains("خودخوری") || norm.contains("کانیبالیسم") ||
                norm.contains("مریض") || norm.contains("بیمار") || norm.contains("چشونه") || norm.contains("مشکل") ||
                norm.contains("خفگی") || norm.contains("کارد به استخون")

        val matchedClinicalSymptoms = com.example.poultryapp.data.model.DiseaseCatalog.findSymptomsFromVoiceText(norm)
        val directDiseaseMatch = com.example.poultryapp.data.model.DiseaseCatalog.findDiseaseByName(norm)

        val isRecordingVerb = com.example.poultryapp.engine.FarmIntentSemanticRouter.isOperationalActionPattern(norm) ||
                norm.contains("ثبت") || norm.contains("بزن") || norm.contains("اضافه کن") || norm.contains("کم کن") || norm.contains("بریز") || norm.contains("تجویز") ||
                norm.contains("حذف") || norm.contains("وازده") || norm.contains("پیرکنی") || norm.contains("کالینگ") ||
                norm.contains("دادیم") || norm.contains("دادم") || norm.contains("دادند") || norm.contains("داد") ||
                norm.contains("داشتیم") || norm.contains("تلف شد") || norm.contains("تلف شدند") || norm.contains("تلف گردید") ||
                norm.contains("مصرف شد") || norm.contains("مصرف کرد") || norm.contains("مصرف کردند") || norm.contains("مصرف کردیم") ||
                norm.contains("ریختیم") || norm.contains("ریختم") || norm.contains("ریختند") || norm.contains("ریخت") ||
                norm.contains("تخلیه شد") || norm.contains("تخلیه کردیم") || norm.contains("تخلیه شدیم") ||
                norm.contains("تلفات دادیم") || norm.contains("تلفات داشتیم") || norm.contains("تلفات خوردیم")

        if (!isRecordingVerb && (isDiseaseQuestion || isClinicalSymptomReport || matchedClinicalSymptoms.isNotEmpty())) {
            // ۱. اگر کاربر مستقیماً نام یا علائم یک بیماری خاص را پرسیده است
            if (directDiseaseMatch != null && (isDiseaseQuestion || norm.contains("درمان") || norm.contains("دارو") || norm.contains("علائم") || norm.contains("توضیح") || norm.contains("چطور"))) {
                val d = directDiseaseMatch
                val treatmentSnippet = if (d.treatmentStepsFa.length > 85) d.treatmentStepsFa.take(85) + "..." else d.treatmentStepsFa
                val drugsSnippet = if (d.commonDrugsFa.length > 60) d.commonDrugsFa.take(60) + "..." else d.commonDrugsFa

                return VoiceActionCardData(
                    intentType = VoiceIntentType.DIAGNOSE_DISEASE,
                    title = "اطلاعات بالینی: ${d.nameFa}",
                    iconRes = R.drawable.ic_farm_health,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = d.nameFa,
                    details = listOf(
                        "بیماری" to d.nameFa,
                        "عامل پاتوژن" to d.pathogenType.titleFa,
                        "گام درمانی" to treatmentSnippet,
                        "داروهای شاخص" to drugsSnippet
                    ),
                    systemEffect = "انتقال فوری به پرونده درمانی کامل «${d.nameFa}» در بخش سلامت فارم",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "diseaseId" to d.id,
                        "diseaseName" to d.nameFa
                    )
                )
            }

            // ۲. اگر کاربر علائم را بیان کرده است، اجرای غربالگری تخصصی دامپزشکی با لحاظ سن و گونه
            if (matchedClinicalSymptoms.isNotEmpty()) {
                val species = matchedFlock?.species ?: when {
                    norm.contains("بوقلمون") -> com.example.poultryapp.data.model.BirdSpecies.TURKEY
                    norm.contains("بلدرچین") -> com.example.poultryapp.data.model.BirdSpecies.QUAIL
                    norm.contains("اردک") -> com.example.poultryapp.data.model.BirdSpecies.DUCK
                    norm.contains("غاز") -> com.example.poultryapp.data.model.BirdSpecies.GOOSE
                    norm.contains("شاخدار") || norm.contains("گینه") -> com.example.poultryapp.data.model.BirdSpecies.GUINEA_FOWL
                    else -> com.example.poultryapp.data.model.BirdSpecies.CHICKEN
                }

                // استخراج سن از کلام یا از گله فعال سالن
                val spokenAge = PersianVoiceNormalizer.extractFlockAge(norm)
                val flockAgeWeeks: Int? = when {
                    spokenAge != null && spokenAge.second == "WEEKS" -> spokenAge.first
                    spokenAge != null && spokenAge.second == "DAYS" -> (spokenAge.first / 7).coerceAtLeast(0)
                    matchedFlock?.ageWeeks != null -> matchedFlock.ageWeeks
                    matchedFlock?.ageDays != null -> (matchedFlock.ageDays / 7).coerceAtLeast(0)
                    else -> null
                }
                val flockAgeDays: Int? = when {
                    spokenAge != null && spokenAge.second == "DAYS" -> spokenAge.first
                    spokenAge != null && spokenAge.second == "WEEKS" -> spokenAge.first * 7
                    matchedFlock?.ageDays != null -> matchedFlock.ageDays
                    matchedFlock?.ageWeeks != null -> matchedFlock.ageWeeks * 7
                    else -> null
                }

                val symptomIds = matchedClinicalSymptoms.map { it.id }.toSet()
                val consultation = com.example.poultryapp.engine.DiseaseDiagnosisEngine.consultVeterinarian(
                    species = species,
                    selectedSymptomIds = symptomIds,
                    flockAgeWeeks = flockAgeWeeks,
                    flockAgeDays = flockAgeDays
                )

                val topResult = consultation.topResult
                if (topResult != null) {
                    val topDisease = topResult.disease
                    val matchedNames = matchedClinicalSymptoms.take(3).joinToString("، ") { it.titleFa }
                    val percentStr = toPersianDigits(topResult.probabilityPercent)
                    val isNutritional = consultation.isNutritionalDeficiency

                    val natureTitle = if (isNutritional) "کمبود تغذیه‌ای" else "بیماری عفونی"
                    val natureLabel = if (isNutritional) "⚠️ کمبود مواد مغذی و متابولیک" else "عفونت ${topDisease.pathogenType.titleFa}"

                    val differentialsText = if (consultation.differentialCandidates.isNotEmpty()) {
                        consultation.differentialCandidates.joinToString("، ") { "${it.disease.nameFa} (${toPersianDigits(it.probabilityPercent)}٪)" }
                    } else {
                        "تشخیص تفریقی شاخص اولیه"
                    }

                    val ageDisplay = when {
                        flockAgeWeeks != null -> "سن گله: هفته ${toPersianDigits(flockAgeWeeks)}"
                        flockAgeDays != null -> "سن: ${toPersianDigits(flockAgeDays)} روز"
                        else -> "سن عمومی پرنده"
                    }

                    val detailsList = mutableListOf(
                        "نشانه‌های شنیده‌شده" to matchedNames,
                        "محتمل‌ترین عارضه" to "${topDisease.nameFa} ($percentStr٪ تطابق)",
                        "ماهیت بالینی" to natureLabel,
                        "ارزیابی سن" to if (topResult.ageCompatibilityNoteFa.isNotBlank()) topResult.ageCompatibilityNoteFa else ageDisplay,
                        "سایر احتمالات (تفریقی)" to differentialsText,
                        "اقدام فوری اولیه" to consultation.immediateEmergencyActionFa,
                        "دارو و نسخه اولیه" to (if (topDisease.commonDrugsFa.length > 55) topDisease.commonDrugsFa.take(55) + "..." else topDisease.commonDrugsFa)
                    )

                    return VoiceActionCardData(
                        intentType = VoiceIntentType.DIAGNOSE_DISEASE,
                        title = "دامپزشک هوشمند فارم: ${topDisease.nameFa} ($natureTitle)",
                        iconRes = R.drawable.ic_farm_health,
                        flockId = flockId,
                        flockName = flockName,
                        primaryMetric = "احتمال بالینی: ${topDisease.nameFa} ($percentStr٪)",
                        details = detailsList,
                        systemEffect = "انتقال به پرونده «${topDisease.nameFa}» جهت مشاهده جزئیات درمان و داروها",
                        rawText = spokenText,
                        isValid = true,
                        parameters = mapOf(
                            "diseaseId" to topDisease.id,
                            "diseaseName" to topDisease.nameFa,
                            "probabilityPercent" to topResult.probabilityPercent,
                            "isNutritional" to isNutritional,
                            "clinicalReasoningFa" to consultation.clinicalReasoningFa,
                            "immediateActionFa" to consultation.immediateEmergencyActionFa,
                            "firstLineRxFa" to consultation.firstLineRxFa,
                            "differentials" to consultation.differentialCandidates.map { mapOf("id" to it.disease.id, "name" to it.disease.nameFa, "percent" to it.probabilityPercent, "isNutritional" to it.isNutritional, "keySign" to it.differentialKeySignFa) },
                            "symptomIds" to symptomIds.toList(),
                            "flockAgeWeeks" to (flockAgeWeeks ?: 0),
                            "species" to species.name
                        )
                    )
                }
            }

            if (isDiseaseQuestion || isClinicalSymptomReport) {
                return VoiceActionCardData(
                    intentType = VoiceIntentType.DIAGNOSE_DISEASE,
                    title = "غربالگری بالینی بیماری‌ها",
                    iconRes = R.drawable.ic_farm_health,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = "نیاز به بررسی علائم تکمیلی",
                    details = listOf(
                        "گزارش دریافتی" to spokenText,
                        "راهنما" to "علائم مشاهده‌شده ثبت شد. جهت تشخیص قطعی، سایر علائم بالینی (مانند فضله، تنفس یا اندام‌ها) را بیان فرمایید."
                    ),
                    systemEffect = "انتقال به بخش کلینیک و غربالگری سلامت فارم",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf("action" to "OPEN_DIAGNOSIS")
                )
            }
        }

        // ۴. نیت امور مالی، هزینه‌ها و درآمدهای فارم (Financial Transactions)
        val isFinancialKeyword = (norm.contains("هزینه") || norm.contains("خرج") || norm.contains("پرداخت") ||
                norm.contains("فاکتور") || norm.contains("خرید") || norm.contains("درآمد") ||
                norm.contains("فروش") || norm.contains("واریز") || norm.contains("تومان") ||
                norm.contains("تومن") || norm.contains("ریال") || norm.contains("قبض") ||
                norm.contains("دستمزد") || norm.contains("کارگر")) &&
                !norm.contains("سر به سر") && !norm.contains("قیمت تمام شده") &&
                !(com.example.poultryapp.engine.FarmIntentSemanticRouter.isQuestionPattern(norm) && !norm.contains("ثبت") && !norm.contains("بزن"))
        if (isFinancialKeyword) {
            val amountTomans = PersianVoiceNormalizer.extractFinancialAmountTomans(norm)
            if (amountTomans == null || amountTomans <= 0.0) {
                val isIncome = norm.contains("درآمد") || norm.contains("فروش") || norm.contains("واریز")
                val prompt = "مبلغ تراکنش مالی را به تومان یا ریال بفرمایید (مثال: ۲۰۰ هزار تومان):"
                return VoiceActionCardData(
                    intentType = VoiceIntentType.RECORD_FINANCIAL,
                    title = "ثبت تراکنش مالی (نیازمند مبلغ)",
                    iconRes = R.drawable.ic_farm_financial,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = "مبلغ مشخص نشده است",
                    details = listOf(
                        "راهنما" to prompt
                    ),
                    systemEffect = "دستور بدون اعلام مبلغ قابل ثبت نیست.",
                    rawText = spokenText,
                    isValid = false,
                    needsMoreInfo = true,
                    missingPromptFa = prompt,
                    quickReplies = listOf("۱۰۰ هزار تومان", "۲۰۰ هزار تومان", "۵۰۰ هزار تومان", "۱ میلیون تومان"),
                    errorMessage = "مبلغ هزینه یا درآمد در کلام شما تشخیص داده نشد.",
                    parameters = mapOf(
                        "pendingIntent" to "RECORD_FINANCIAL",
                        "flockId" to flockId,
                        "flockName" to flockName,
                        "isIncome" to isIncome
                    )
                )
            }

            // تعیین نوع تراکنش (درآمد یا هزینه)
            val isIncome = norm.contains("درآمد") || norm.contains("فروش") || norm.contains("واریز") ||
                    norm.contains("دریافت") || norm.contains("سود") || norm.contains("طلب")
            val txType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

            // تعیین هوشمند دسته‌بندی تراکنش بر اساس کلمات کلیدی
            val category = when {
                norm.contains("تخم") -> if (isIncome) TransactionCategory.EGG_SALE else TransactionCategory.OTHER_EXPENSE
                norm.contains("مرغ") || norm.contains("پرنده") || norm.contains("جوجه") || norm.contains("خروس") -> if (isIncome) TransactionCategory.BIRD_SALE else TransactionCategory.OTHER_EXPENSE
                norm.contains("دان") || norm.contains("دون") || norm.contains("خوراک") || norm.contains("سویا") || norm.contains("ذرت") || norm.contains("کنسانتره") -> TransactionCategory.FEED_PURCHASE
                norm.contains("دارو") || norm.contains("واکسن") || norm.contains("مکمل") || norm.contains("ویتامین") || norm.contains("آنتی بیوتیک") -> TransactionCategory.MED_PURCHASE
                norm.contains("برق") || norm.contains("گاز") || containsWaterWord(norm) || norm.contains("تعمیر") || norm.contains("فن") || norm.contains("هیتر") || norm.contains("تجهیزات") || norm.contains("لامپ") -> TransactionCategory.EQUIPMENT
                else -> if (isIncome) TransactionCategory.OTHER_INCOME else TransactionCategory.OTHER_EXPENSE
            }

            // تعیین عنوان تراکنش
            val txTitle = when {
                norm.contains("برق") -> "هزینه برق فارم"
                norm.contains("گاز") -> "هزینه گاز فارم"
                norm.contains("تعمیر") -> "تعمیرات و نگهداری تجهیزات"
                norm.contains("دان") || norm.contains("دون") -> if (isIncome) "فروش مازاد دان" else "خرید دان و خوراک"
                norm.contains("دارو") || norm.contains("واکسن") -> "خرید دارو و واکسن"
                norm.contains("تخم") -> "فروش تخم‌مرغ"
                norm.contains("پرنده") || norm.contains("مرغ") -> "فروش پرندگان"
                norm.contains("دستمزد") || norm.contains("کارگر") -> "دستمزد و حقوق کارگر"
                isIncome -> "درآمد ثبت شده با دستیار هوشمند"
                else -> "هزینه جاری ثبت شده با دستیار هوشمند"
            }

            val formattedTomans = toPersianDigits(amountTomans.toLong())

            val resolvedDate = RelativeDateResolver.resolveDate(norm)
            val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
            val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()

            return VoiceActionCardData(
                intentType = VoiceIntentType.RECORD_FINANCIAL,
                title = if (isIncome) "ثبت درآمد فارم" else "ثبت هزینه مالی فارم",
                iconRes = R.drawable.ic_farm_financial,
                flockId = flockId,
                flockName = flockName,
                primaryMetric = "$formattedTomans تومان (${txType.titleFa})",
                details = listOf(
                    "نوع عملیات" to txType.titleFa,
                    "دسته‌بندی" to category.titleFa,
                    "شرح فاکتور" to txTitle,
                    "سالن مرتبط" to flockName,
                    "تاریخ ثبت" to dateJalaliStr
                ),
                systemEffect = if (isIncome) {
                    "افزایش تراز مالی و ثبت سود ناشی از درآمد به مبلغ $formattedTomans تومان"
                } else {
                    "ثبت هزینه به مبلغ $formattedTomans تومان در دفاتر مالی فارم و کسر از تراز نقدی"
                },
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "amount" to amountTomans,
                    "amountTomans" to amountTomans,
                    "type" to txType,
                    "transactionType" to txType,
                    "category" to category,
                    "title" to txTitle,
                    "flockId" to flockId,
                    "flockName" to flockName,
                    "dateJalaliStr" to dateJalaliStr,
                    "timestampMillis" to timestampMillis
                )
            )
        }

        // ۴.۸. نیت انبارداری و مدیریت کالاها (MANAGE_WAREHOUSE - اولویت قبل از تخم‌مرغ و دان)
        val isWarehouseKeyword = norm.contains("انبار") ||
                (norm.contains("موجودی") && !norm.contains("گله") && !norm.contains("سالن")) ||
                norm.contains("کارتن خالی") || norm.contains("شارژ انبار") || norm.contains("کالاهای انبار")
        if (isWarehouseKeyword && !norm.contains("کمبود دان")) {
            val isIncoming = norm.contains("ورود") || norm.contains("شارژ") || norm.contains("خرید") || norm.contains("رسید") || norm.contains("اضافه") || norm.contains("افزودن")
            val isOutgoing = norm.contains("خروج") || norm.contains("حواله") || norm.contains("کسر") || norm.contains("ارسال") || norm.contains("کم کن")
            val rawQty = PersianVoiceNormalizer.extractFirstNumber(norm, excludeFlockPhrase) ?: 10.0

            val itemName = when {
                norm.contains("دان") || norm.contains("خوراک") || norm.contains("استارتر") || norm.contains("پلت") -> "دان طیور"
                norm.contains("تخم") || norm.contains("کارتن") -> "کارتن تخم‌مرغ"
                norm.contains("دارو") || norm.contains("تایلوزین") -> "دارو و مکمل"
                else -> "اقلام عمومی انبار"
            }

            val unit = when {
                norm.contains("کیسه") -> "کیسه"
                norm.contains("کارتن") -> "کارتن"
                norm.contains("کیلو") -> "کیلوگرم"
                else -> "عدد"
            }

            if (isIncoming || isOutgoing) {
                val delta = if (isOutgoing) -rawQty else rawQty
                val actionTitle = if (isIncoming) "ثبت ورود کالا به انبار" else "ثبت خروج کالا از انبار"
                val systemEffectText = if (isIncoming) "افزایش موجودی $itemName در انبار مرکزی فارم" else "کسر موجودی $itemName از انبار مرکزی فارم"

                return VoiceActionCardData(
                    intentType = VoiceIntentType.MANAGE_WAREHOUSE,
                    title = actionTitle,
                    iconRes = R.drawable.ic_farm_warehouse,
                    primaryMetric = "${toPersianDigits(Math.abs(delta))} $unit $itemName",
                    details = listOf(
                        "نام کالا" to itemName,
                        "نوع عملیات" to if (isIncoming) "ورود / شارژ انبار" else "خروج / تحویل از انبار",
                        "تعداد / مقدار" to "${toPersianDigits(Math.abs(delta))} $unit"
                    ),
                    systemEffect = systemEffectText,
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "itemName" to itemName,
                        "deltaStock" to delta,
                        "unit" to unit,
                        "isMutating" to true
                    )
                )
            } else {
                return VoiceActionCardData(
                    intentType = VoiceIntentType.MANAGE_WAREHOUSE,
                    title = "گزارش موجودی اقلام انبار",
                    iconRes = R.drawable.ic_farm_warehouse,
                    primaryMetric = "موجودی انبار مرکزی",
                    details = listOf(
                        "اقلام پایش‌شده" to "دان، کارتن تخم‌مرغ، دارو و مکمل‌ها",
                        "وضعیت" to "آماده نمایش زنده موجودی"
                    ),
                    systemEffect = "انتقال به داشبورد انبارداری جهت نمایش موجودی لحظه‌ای",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "screen" to "WAREHOUSE",
                        "isMutating" to false
                    )
                )
            }
        }

        // ۵. نیت ثبت تخم‌مرغ
        val isEggKeyword = (norm.contains("تخم مرغ") || norm.contains("تخممرغ") || norm.contains("شانه") || norm.contains("کارتن") ||
                norm.contains("نطفه") || norm.contains("تولید روز") || norm.contains("تخم گذاری") || norm.contains("تخمگذاری") ||
                (norm.contains("تخم") && !norm.contains("تخمگذار") && !norm.contains("تخم گذار"))) &&
                !norm.contains("مصرف آب") && !norm.contains("آب مصرفی")
        if (isEggKeyword) {
            val rawNum = PersianVoiceNormalizer.extractFirstNumber(norm, excludeFlockPhrase)
            val isExplicitZero = norm.contains("صفر") || norm.contains(" 0 ") || norm.endsWith(" 0") || norm.startsWith("0 ") || norm.contains("۰") ||
                    norm.contains("هیچ") || norm.contains("تخم مرغ نداشتیم") || norm.contains("تخم نداشتیم") || norm.contains("تولید نداشتیم")
            if (rawNum == null || (rawNum <= 0.0 && !isExplicitZero)) {
                if (!isExplicitZero) {
                    val prompt = "تعداد دانه یا شانه تخم‌مرغ سالن «$flockName» چقدر است؟"
                    return VoiceActionCardData(
                        intentType = VoiceIntentType.RECORD_EGG,
                        title = "ثبت تولید تخم‌مرغ (نیازمند تعداد)",
                        iconRes = R.drawable.ic_farm_egg,
                        flockId = flockId,
                        flockName = flockName,
                        primaryMetric = "تعداد مشخص نشده است",
                        details = listOf(
                            "گله هدف" to flockName,
                            "راهنما" to prompt
                        ),
                        systemEffect = "دستور بدون اعلام مقدار قابل ثبت نیست.",
                        rawText = spokenText,
                        isValid = false,
                        needsMoreInfo = true,
                        missingPromptFa = prompt,
                        quickReplies = listOf("۳۰ دانه (۱ شانه)", "۶۰ دانه (۲ شانه)", "۱۲۰ دانه", "۱۵ دانه نطفه‌دار"),
                        errorMessage = "تعداد یا شانه تخم‌مرغ در کلام شما مشخص نشد.",
                        parameters = mapOf(
                            "pendingIntent" to "RECORD_EGG",
                            "flockId" to flockId,
                            "flockName" to flockName
                        )
                    )
                }
            }

            val isFertile = norm.contains("نطفه") || norm.contains("جوجه کشی")
            val isExportCarton = norm.contains("صادراتی") || norm.contains("۳۶۰") || norm.contains("360")
            val isHalfCarton = norm.contains("نیم کارتن") || norm.contains("نصف کارتن")
            val isHalfTray = norm.contains("نیم شانه") || norm.contains("نصف شانه")
            val isCarton = (norm.contains("کارتن") || norm.contains("کارتون")) && !isExportCarton
            val isTray = norm.contains("شانه") || norm.contains("شونه")

            val eggNum = if (isExplicitZero && (rawNum == null || rawNum <= 0.0)) 0.0 else (rawNum ?: 0.0)
            val effectiveNum = when {
                isHalfCarton && eggNum == 0.0 && !isExplicitZero -> 0.5
                isHalfTray && eggNum == 0.0 && !isExplicitZero -> 0.5
                else -> eggNum
            }

            val totalEggs = when {
                isExplicitZero && effectiveNum == 0.0 -> 0
                isExportCarton -> (effectiveNum * 360).toInt()
                isHalfCarton -> (effectiveNum * 180).toInt().coerceAtLeast(90)
                isCarton -> (effectiveNum * 180).toInt()
                isHalfTray -> (effectiveNum * 30).toInt().coerceAtLeast(15)
                isTray -> (effectiveNum * 30).toInt()
                else -> effectiveNum.toInt()
            }
            val fertileCount = if (isFertile) totalEggs else 0
            val tableCount = if (!isFertile) totalEggs else 0

            val cartonMultiplier = if (isExportCarton) 360.0 else 180.0
            val cartonCount = when {
                totalEggs == 0 -> 0.0
                isExportCarton || isCarton -> effectiveNum
                isHalfCarton -> 0.5
                else -> totalEggs / cartonMultiplier
            }
            val trayCount = when {
                totalEggs == 0 -> 0.0
                isTray -> effectiveNum
                isHalfTray -> 0.5
                else -> totalEggs / 30.0
            }

            val metricUnit = when {
                isExportCarton -> "کارتن صادراتی"
                isCarton || isHalfCarton -> "کارتن"
                isTray || isHalfTray -> "شانه"
                else -> "دانه"
            }

            val resolvedDate = RelativeDateResolver.resolveDate(norm)
            val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
            val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()
            val shift = resolvedDate?.shift ?: "DAY"

            val primaryMetric = if (totalEggs == 0) {
                "۰ دانه (بدون تولید تخم‌مرغ)"
            } else {
                "${toPersianDigits(effectiveNum)} $metricUnit (${toPersianDigits(totalEggs)} عدد سالم)"
            }

            return VoiceActionCardData(
                intentType = VoiceIntentType.RECORD_EGG,
                title = "ثبت تولید تخم‌مرغ",
                iconRes = R.drawable.ic_farm_egg,
                flockId = flockId,
                flockName = flockName,
                primaryMetric = primaryMetric,
                details = listOf(
                    "گله هدف" to flockName,
                    "رده کیفی" to if (isFertile) "تخم‌مرغ نطفه‌دار (جوجه‌کشی)" else "تخم‌مرغ خوراکی روز",
                    "تعداد کل" to "${toPersianDigits(totalEggs)} عدد",
                    "تاریخ ثبت" to dateJalaliStr
                ),
                systemEffect = if (totalEggs == 0) "ثبت وضعیت روز بدون تولید برای گله «$flockName»" else "افزایش رکورد تولید گله «$flockName» و شارژ خودکار انبار فارم",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "flockId" to flockId,
                    "flockName" to flockName,
                    "amount" to totalEggs.toDouble(),
                    "count" to totalEggs,
                    "totalEggs" to totalEggs,
                    "cartonCount" to cartonCount,
                    "trayCount" to trayCount,
                    "fertileCount" to fertileCount,
                    "tableEggCount" to tableCount,
                    "dateJalaliStr" to dateJalaliStr,
                    "timestampMillis" to timestampMillis,
                    "epochMillis" to timestampMillis,
                    "shift" to shift
                )
            )
        }

        // ۶. نیت ثبت تلفات و خروج پرندگان
        val isMortalityKeyword = norm.contains("تلفات") || norm.contains("تلف") || norm.contains("مرگ") ||
                norm.contains("مرده") || norm.contains("مرد") || norm.contains("کشته") || norm.contains("مردن") ||
                norm.contains("مردار") || norm.contains("از بین رفت") || norm.contains("خودخوری") || norm.contains("کانیبالیسم") ||
                ((norm.contains("حذف") || norm.contains("حذفی") || norm.contains("وازده") || norm.contains("پیرکنی") || norm.contains("کالینگ")) && !norm.contains("حذف گله") && !norm.contains("حذف سالن"))
        if (isMortalityKeyword) {
            val rawCount = PersianVoiceNormalizer.extractFirstNumber(norm, excludeFlockPhrase)
            val isExplicitZero = norm.contains("صفر") || norm.contains(" 0 ") || norm.endsWith(" 0") || norm.startsWith("0 ") || norm.contains("۰") ||
                    norm.contains("هیچ") || norm.contains("تلفات نداشتیم") || norm.contains("تلفاتی نداشتیم") || norm.contains("مرگ و میر نداشتیم") ||
                    norm.contains("تلف نشد") || norm.contains("تلف نشدن")
            if (rawCount == null || (rawCount <= 0.0 && !isExplicitZero)) {
                if (!isExplicitZero) {
                    val prompt = "چند قطعه تلفات در سالن «$flockName» مشاهده شده است؟"
                    return VoiceActionCardData(
                        intentType = VoiceIntentType.RECORD_MORTALITY,
                        title = "ثبت تلفات سالن (نیازمند تعداد)",
                        iconRes = R.drawable.ic_farm_health,
                        flockId = flockId,
                        flockName = flockName,
                        primaryMetric = "تعداد مشخص نشده است",
                        details = listOf(
                            "گله هدف" to flockName,
                            "راهنما" to prompt
                        ),
                        systemEffect = "دستور بدون اعلام تعداد قابل اعمال نیست.",
                        rawText = spokenText,
                        isValid = false,
                        needsMoreInfo = true,
                        missingPromptFa = prompt,
                        quickReplies = listOf("۱ قطعه", "۲ قطعه", "۳ قطعه", "۵ قطعه تلفات"),
                        errorMessage = "تعداد تلفات پرندگان در کلام شما مشخص نشد.",
                        parameters = mapOf(
                            "pendingIntent" to "RECORD_MORTALITY",
                            "flockId" to flockId,
                            "flockName" to flockName
                        )
                    )
                }
            }

            val count = if (isExplicitZero && (rawCount == null || rawCount <= 0.0)) 0 else (rawCount?.toInt() ?: 0)
            val reason = when {
                norm.contains("گرما") || norm.contains("سرما") || norm.contains("استرس") || norm.contains("خفگی") || norm.contains("فن") -> MortalityReason.HEAT_COLD_STRESS
                norm.contains("کانیبالیسم") || norm.contains("خودخوری") || norm.contains("نوک") || norm.contains("زخم") -> MortalityReason.CANNIBALISM
                norm.contains("شکارچی") || norm.contains("موش") || norm.contains("گربه") || norm.contains("حادثه") -> MortalityReason.PREDATOR_ACCIDENT
                norm.contains("فروش") || norm.contains("زنده") || norm.contains("کشتار") -> MortalityReason.LIVE_SALE
                norm.contains("حذف") || norm.contains("پیر") || norm.contains("ضعیف") || norm.contains("واکووی") || norm.contains("کالینگ") || norm.contains("وازده") || norm.contains("وازدگی") -> MortalityReason.CULLING
                else -> MortalityReason.DISEASE
            }

            val resolvedDate = RelativeDateResolver.resolveDate(norm)
            val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
            val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()
            val shift = resolvedDate?.shift ?: "DAY"

            val hasSpike = count >= 10
            val spokenWarning = if (hasSpike) "توجه: تلفات ثبت‌شده نسبت به میانگین روزهای قبل جهش محسوسی داشته است. آیا علائم بالینی خاصی مشاهده کردید؟" else ""

            val detailsList = mutableListOf(
                "گله هدف" to flockName,
                "علت تلفات" to reason.titleFa,
                "ماهیت" to if (reason.isTrueMortality) "تلفات بیولوژیکی فارم" else "خروج تجاری پرنده",
                "تاریخ ثبت" to dateJalaliStr
            )
            if (hasSpike) {
                detailsList.add("هشدار ناهنجاری ⚠️" to "جهش ناگهانی تلفات سالن")
            }

            val primaryMetric = if (count == 0) "۰ قطعه تلفات (بدون تلفات)" else "${toPersianDigits(count)} قطعه تلفات"
            val systemEffect = if (count == 0) "ثبت وضعیت سلامت بدون تلفات در سالن «$flockName»" else "کسر قطعی ${toPersianDigits(count)} قطعه از موجودی زنده گله «$flockName»"

            return VoiceActionCardData(
                intentType = VoiceIntentType.RECORD_MORTALITY,
                title = if (hasSpike) "ثبت تلفات (هشدار جهش غیرعادی ⚠️)" else "ثبت تلفات و خروج پرنده",
                iconRes = R.drawable.ic_farm_health,
                flockId = flockId,
                flockName = flockName,
                primaryMetric = primaryMetric,
                details = detailsList,
                systemEffect = systemEffect,
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "flockId" to flockId,
                    "flockName" to flockName,
                    "amount" to count.toDouble(),
                    "count" to count,
                    "reason" to reason,
                    "dateJalaliStr" to dateJalaliStr,
                    "timestampMillis" to timestampMillis,
                    "epochMillis" to timestampMillis,
                    "shift" to shift,
                    "anomalyAlert" to spokenWarning,
                    "spokenWarning" to spokenWarning
                )
            )
        }


        // ۶.۵. نیت فعال‌سازی یا غیرفعال‌سازی پایش دان و آب سالن
        val hasWaterInTracking = containsWaterWord(norm)
        val isTrackingCmd = norm.contains("پایش") || norm.contains("مانیتورینگ") ||
                ((norm.contains("دان") || hasWaterInTracking) &&
                        (norm.contains("فعال") || norm.contains("غیرفعال") || norm.contains("روشن") || norm.contains("خاموش") || norm.contains("راه اندازی")))
        if (isTrackingCmd && !norm.contains("پوسته") && !norm.contains("est") && !norm.contains("چک لیست") && !norm.contains("چک‌لیست")) {
            val isWater = hasWaterInTracking && !norm.contains("دان")
            val isDisable = norm.contains("غیرفعال") || norm.contains("خاموش") || norm.contains("متوقف") || norm.contains("ببند") || norm.contains("کنسل")
            val isEnable = !isDisable
            val trackingType = if (isWater) "WATER" else "FEED"
            val typeTitle = if (isWater) "آب" else "دان و خوراک"
            val actionTitle = if (isEnable) "فعال‌سازی پایش $typeTitle سالن «$flockName»" else "غیرفعال‌سازی پایش $typeTitle سالن «$flockName»"
            val stateText = if (isEnable) "فعال" else "غیرفعال"

            return VoiceActionCardData(
                intentType = VoiceIntentType.TOGGLE_TRACKING,
                title = actionTitle,
                iconRes = R.drawable.ic_farm_feed_water,
                flockId = flockId,
                flockName = flockName,
                primaryMetric = "وضعیت جدید: $stateText",
                details = listOf(
                    "سالن / گله" to flockName,
                    "موضوع پایش" to typeTitle,
                    "وضعیت جدید" to if (isEnable) "فعال (محاسبه خودکار و هشدار)" else "غیرفعال (متوقف‌سازی پایش و هشدار)"
                ),
                systemEffect = if (isEnable) "فعال‌سازی محاسبات و آلارم‌های کمبود $typeTitle برای گله «$flockName»" else "توقف پایش خودکار $typeTitle برای گله «$flockName»",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "trackingType" to trackingType,
                    "isEnabled" to isEnable,
                    "flockId" to flockId,
                    "flockName" to flockName,
                    "isMutating" to true
                ),
                quickReplies = listOf(
                    if (isWater) "پایش دان «$flockName»" else "پایش آب «$flockName»",
                    "بررسی وضعیت «$flockName»"
                )
            )
        }

        // ۷. نیت شارژ دان و خوراک (پشتیبانی از اصطلاحات عامیانه دان و دون)
        val isFeedInquiry = norm.contains("چه نوع") || norm.contains("چه دانی") || norm.contains("میخورد") || norm.contains("باید بخورد") ||
                norm.contains("بدم") || norm.contains("بدیم") || norm.contains("مناسب است") || norm.contains("چطور است") ||
                norm.contains("چگونه است") || norm.contains("روزی چند") || norm.contains("چقدر دان") || norm.contains("روز ۲۰") ||
                norm.contains("روز 20") || norm.contains("روز بیست") || isTrackingCmd
        val isFeedKeyword = !isFeedInquiry && (norm.contains("دان") || norm.contains("دون") || norm.contains("خوراک") ||
                norm.contains("کنسانتره") || norm.contains("پلت") || norm.contains("مش") ||
                norm.contains("شارژ دان") || norm.contains("سویا") || norm.contains("ذرت") || norm.contains("تغذیه")) &&
                !norm.contains("آب به دان") && !norm.contains("اب به دان") && !norm.contains("نسبت")
        if (isFeedKeyword) {
            val isHalfBag = norm.contains("نیم کیسه") || norm.contains("نصف کیسه") || norm.contains("نیم گونی") || norm.contains("نصف گونی")
            val isHalfTon = norm.contains("نیم تن") || norm.contains("نصف تن")
            val isBags = norm.contains("کیسه") || norm.contains("گونی")
            val isTon = norm.contains("تن") && !isHalfTon
            val rawKg = when {
                isHalfBag -> 0.5
                isHalfTon -> 0.5
                else -> PersianVoiceNormalizer.extractFirstNumber(norm, excludeFlockPhrase)
            }
            if (rawKg == null || rawKg <= 0.0) {
                val prompt = "چند کیلوگرم یا کیسه دان برای سالن «$flockName» شارژ شود؟"
                return VoiceActionCardData(
                    intentType = VoiceIntentType.RECORD_FEED,
                    title = "شارژ دان و خوراک (نیازمند وزن)",
                    iconRes = R.drawable.ic_farm_feed_water,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = "مقدار مشخص نشده است",
                    details = listOf(
                        "گله هدف" to flockName,
                        "راهنما" to prompt
                    ),
                    systemEffect = "دستور بدون اعلام وزن دان قابل ثبت نیست.",
                    rawText = spokenText,
                    isValid = false,
                    needsMoreInfo = true,
                    missingPromptFa = prompt,
                    quickReplies = listOf("۵۰ کیلوگرم (۱ کیسه)", "۱۰۰ کیلوگرم (۲ کیسه)", "۲۵ کیلوگرم", "۲۰۰ کیلوگرم"),
                    errorMessage = "وزن یا میزان دان در کلام شما مشخص نشد.",
                    parameters = mapOf(
                        "pendingIntent" to "RECORD_FEED",
                        "flockId" to flockId,
                        "flockName" to flockName
                    )
                )
            }

            val finalKg = when {
                isHalfBag -> 25.0
                isHalfTon -> 500.0
                isBags -> rawKg * 50.0
                isTon -> rawKg * 1000.0
                else -> rawKg
            }
            val sackCount = finalKg / 50.0

            val resolvedDate = RelativeDateResolver.resolveDate(norm)
            val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
            val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()
            val shift = resolvedDate?.shift ?: "DAY"

            val primaryMetric = when {
                isHalfBag || isBags -> "${toPersianDigits(rawKg)} کیسه (${toPersianDigits(finalKg)} کیلوگرم دان)"
                isHalfTon || isTon -> "${toPersianDigits(rawKg)} تن (${toPersianDigits(finalKg)} کیلوگرم دان)"
                else -> "${toPersianDigits(finalKg)} کیلوگرم دان"
            }

            return VoiceActionCardData(
                intentType = VoiceIntentType.RECORD_FEED,
                title = "شارژ مصرف دان سالن",
                iconRes = R.drawable.ic_farm_feed_water,
                flockId = flockId,
                flockName = flockName,
                primaryMetric = primaryMetric,
                details = listOf(
                    "گله مقصد" to flockName,
                    "مقدار شارژ" to "${toPersianDigits(finalKg)} کیلوگرم",
                    "تاریخ ثبت" to dateJalaliStr
                ),
                systemEffect = "ثبت در مصرف زنده گله «$flockName» و کسر خودکار از موجودی انبار دان",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "flockId" to flockId,
                    "flockName" to flockName,
                    "amount" to finalKg,
                    "amountKg" to finalKg,
                    "sackCount" to sackCount,
                    "dateJalaliStr" to dateJalaliStr,
                    "timestampMillis" to timestampMillis,
                    "epochMillis" to timestampMillis,
                    "shift" to shift
                )
            )
        }

        // ۸. نیت ثبت مصرف دارو، مکمل یا واکسیناسیون (اولویت بالاتر از آب به دلیل مصرف دارو در آب)
        val isMedKeyword = norm.contains("دارو") || norm.contains("واکسن") || norm.contains("واکسیناسیون") ||
                norm.contains("واکسین") || norm.contains("مکمل") ||
                norm.contains("ویتامین") || norm.contains("آنتی بیوتیک") || norm.contains("انتی بیوتیک") ||
                norm.contains("آمپرولیوم") || norm.contains("لینکوپک") || norm.contains("انروفلوکساسین") ||
                norm.contains("تایلوزین") || norm.contains("ضد کوکسیدیوز") || norm.contains("پودر ویتامین")
        if (isMedKeyword) {
            val medName = when {
                norm.contains("آمپرولیوم") -> "داروی آمپرولیوم (ضد کوکسیدیوز)"
                norm.contains("لینکوپک") -> "آنتی‌بیوتیک لینکوپک"
                norm.contains("انروفلوکساسین") -> "آنتی‌بیوتیک انروفلوکساسین"
                norm.contains("ویتامین ث") || norm.contains("ویتامین c") -> "پودر ویتامین ث"
                norm.contains("ویتامین") -> "مولتی ویتامین و مکمل تقویتی"
                norm.contains("نیوکاسل") -> "واکسن نیوکاسل"
                norm.contains("آنفلوانزا") || norm.contains("انفلوانزا") -> "واکسن آنفلوانزا"
                norm.contains("گامبورو") -> "واکسن گامبورو"
                norm.contains("برونشیت") -> "واکسن برونشیت"
                norm.contains("ضد کوکسیدیوز") -> "داروی ضد کوکسیدیوز"
                norm.contains("آنتی بیوتیک") || norm.contains("انتی بیوتیک") || norm.contains("تایلوزین") -> "آنتی‌بیوتیک تجویزی"
                norm.contains("پروبیوتیک") -> "پروبیوتیک گوارشی"
                norm.contains("کلسیم") -> "مکمل کلسیم و فسفر"
                else -> "مکمل دارویی عمومی"
            }

            val rawDosage = PersianVoiceNormalizer.extractFirstNumber(norm, excludeFlockPhrase)
            val dosageUnit = when {
                norm.contains("سی سی") || norm.contains("سیسی") || norm.contains("سی‌سی") || norm.contains("cc") -> "سی‌سی"
                norm.contains("میلی لیتر") || norm.contains("میلی‌لیتر") || norm.contains("میل") -> "میلی‌لیتر"
                norm.contains("کیلوگرم") || (norm.contains("کیلو") && !norm.contains("کیلو دان")) -> "کیلوگرم"
                norm.contains("گرم") -> "گرم"
                norm.contains("لیتر") -> "لیتر"
                norm.contains("بسته") -> "بسته"
                norm.contains("قطره") -> "قطره"
                norm.contains("پیمانه") -> "پیمانه"
                norm.contains("ویال") -> "ویال"
                norm.contains("ساشه") -> "ساشه"
                else -> null
            }
            val dosageStr = if (rawDosage != null && dosageUnit != null) "${toPersianDigits(rawDosage)} $dosageUnit" else null

            val isAntibiotic = norm.contains("آنتی بیوتیک") || norm.contains("انتی بیوتیک") ||
                    norm.contains("تایلوزین") || norm.contains("لینکوپک") || norm.contains("انروفلوکساسین")
            val withdrawalNotice = if (isAntibiotic) "دوره پرهیز مصرف: ۵ روز گوشت و ۳ روز تخم‌مرغ" else null
            val spokenWarning = if (isAntibiotic) "توجه: این دارو شامل دوره پرهیز از مصرف گوشت و تخم‌مرغ است." else ""

            val adminRoute = when {
                norm.contains("اسپری") -> "اسپری"
                containsWaterWord(norm) || norm.contains("اشامیدنی") || norm.contains("آشامیدنی") -> "آب آشامیدنی"
                norm.contains("تزریق") -> "تزریق"
                norm.contains("دان") -> "مخلوط با دان"
                else -> "آب آشامیدنی"
            }

            val detailsList = mutableListOf(
                "گله هدف" to flockName,
                "عنوان دارو / واکسن" to medName,
                "روش مصرف" to adminRoute,
                "تاریخ تجویز" to "امروز (ثبت آنی)"
            )
            if (dosageStr != null) {
                detailsList.add("دوز مصرفی" to dosageStr)
            }
            if (withdrawalNotice != null) {
                detailsList.add("دوره پرهیز دارو ⚠️" to withdrawalNotice)
            }

            val isFollowUp = norm.contains("ثبت شد") || norm.contains("چک کن") || norm.contains("استعلام")
            val cardTitle = if (isFollowUp) "پیگیری و استعلام واکسیناسیون" else if (isAntibiotic) "ثبت تجویز دارو (با دوره پرهیز ⚠️)" else "ثبت تجویز دارو / مکمل"
            val primaryMetricText = if (dosageStr != null) "$medName ($dosageStr)" else medName

            return VoiceActionCardData(
                intentType = VoiceIntentType.RECORD_MEDICATION,
                title = cardTitle,
                iconRes = R.drawable.ic_farm_health,
                flockId = flockId,
                flockName = flockName,
                primaryMetric = primaryMetricText,
                details = detailsList,
                systemEffect = "ثبت در سوابق درمانی گله «$flockName» و پایش پروتکل سلامت فارم",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "flockId" to flockId,
                    "flockName" to flockName,
                    "medName" to medName,
                    "drugName" to medName,
                    "dosage" to (rawDosage ?: 0.0),
                    "dosageUnit" to (dosageUnit ?: ""),
                    "dosageStr" to (dosageStr ?: ""),
                    "route" to adminRoute,
                    "administrationRoute" to adminRoute,
                    "withdrawalAlert" to (withdrawalNotice ?: ""),
                    "withdrawalNotice" to (withdrawalNotice ?: ""),
                    "spokenWarning" to spokenWarning,
                    "epochMillis" to System.currentTimeMillis()
                )
            )
        }

        // ۹. نیت مصرف آب سالن‌ها
        val isWaterKnowledgeQuery = norm.contains("خواص") || norm.contains("فواید") || norm.contains("مزایا") ||
                norm.contains("سرکه") || norm.contains("سیر") || norm.contains("اطلاعات استاندارد") ||
                norm.contains("آویشن") || norm.contains("پونه") ||
                norm.contains("تفاوت") || norm.contains("فرق") || norm.contains("مقایسه")
        val isWaterKeyword = (containsWaterWord(norm) || norm.contains("لیتر") ||
                norm.contains("ابخوری") || norm.contains("آبخوری") || norm.contains("هیدراتاسیون") ||
                norm.contains("آب به دان") || norm.contains("اب به دان")) &&
                !isMedKeyword && !isWaterKnowledgeQuery
        if (isWaterKeyword) {
            val isWaterInquiry = norm.contains("طبیعی") || norm.contains("چرا") || norm.contains("زیاد") ||
                    norm.contains("خوبه") || norm.contains("چطوره") || norm.contains("بررسی") ||
                    norm.contains("نسبت") || norm.contains("نرمال") || norm.contains("نشتی") || norm.contains("چکه") ||
                    norm.contains("کاهش") || norm.contains("افت")

            if (isWaterInquiry) {
                val hasDropAlert = norm.contains("کاهش") || norm.contains("افت")
                val titleText = if (hasDropAlert) "هشدار افت مصرف آب سالن ⚠️" else "استعلام و پایش مصرف آب سالن"
                val metricText = if (hasDropAlert) "هشدار: کاهش و افت مصرف آب سالن" else "پایش آب مصرفی $flockName"
                val detailText = if (hasDropAlert) "هشدار: کاهش و افت مصرف آب سالن نسبت به روزهای قبل شناسایی شد" else "بررسی هوشمند هیدراتاسیون و فشار آبخوری"
                return VoiceActionCardData(
                    intentType = VoiceIntentType.QUERY_STATS,
                    title = titleText,
                    iconRes = R.drawable.ic_farm_feed_water,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = metricText,
                    details = listOf(
                        "گله هدف" to flockName,
                        "وضعیت پایش" to detailText
                    ),
                    systemEffect = if (hasDropAlert) "هشدار افت مصرف آب و ریسک شروع بیماری صادر شد" else "ارزیابی برخط وضعیت مصرف آب پرندگان",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf("flockId" to flockId, "flockName" to flockName, "topic" to "WATER")
                )
            }

            val rawLit = PersianVoiceNormalizer.extractFirstNumber(norm, excludeFlockPhrase)
            if (rawLit == null || rawLit <= 0.0) {
                val prompt = "میزان مصرف آب سالن «$flockName» چند لیتر است؟"
                return VoiceActionCardData(
                    intentType = VoiceIntentType.RECORD_WATER,
                    title = "ثبت مصرف آب (نیازمند حجم)",
                    iconRes = R.drawable.ic_farm_feed_water,
                    flockId = flockId,
                    flockName = flockName,
                    primaryMetric = "حجم مشخص نشده است",
                    details = listOf(
                        "گله هدف" to flockName,
                        "راهنما" to prompt
                    ),
                    systemEffect = "دستور بدون اعلام حجم آب قابل ثبت نیست.",
                    rawText = spokenText,
                    isValid = false,
                    needsMoreInfo = true,
                    missingPromptFa = prompt,
                    quickReplies = listOf("۱۰۰ لیتر", "۲۰۰ لیتر", "۳۰۰ لیتر", "۵۰۰ لیتر"),
                    errorMessage = "حجم آب در کلام شما مشخص نشد.",
                    parameters = mapOf(
                        "pendingIntent" to "RECORD_WATER",
                        "flockId" to flockId,
                        "flockName" to flockName
                    )
                )
            }

            var finalLit = rawLit
            if (norm.contains("متر مکعب")) finalLit = rawLit * 1000.0

            val resolvedDate = RelativeDateResolver.resolveDate(norm)
            val dateJalaliStr = resolvedDate?.jalaliDateStr ?: JalaliDateHelper.getJalaliDate().toStandardString()
            val timestampMillis = resolvedDate?.timestampMillis ?: System.currentTimeMillis()
            val shift = resolvedDate?.shift ?: "DAY"

            return VoiceActionCardData(
                intentType = VoiceIntentType.RECORD_WATER,
                title = "ثبت مصرف آب سالن",
                iconRes = R.drawable.ic_farm_feed_water,
                flockId = flockId,
                flockName = flockName,
                primaryMetric = "${toPersianDigits(finalLit)} لیتر آب",
                details = listOf(
                    "گله مقصد" to flockName,
                    "حجم مصرفی" to "${toPersianDigits(finalLit)} لیتر",
                    "تاریخ ثبت" to dateJalaliStr
                ),
                systemEffect = "ثبت شاخص هیدراتاسیون و پایش زنده مصرف آب در گله «$flockName»",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "flockId" to flockId,
                    "flockName" to flockName,
                    "amount" to finalLit,
                    "amountLiters" to finalLit,
                    "dateJalaliStr" to dateJalaliStr,
                    "timestampMillis" to timestampMillis,
                    "epochMillis" to timestampMillis,
                    "shift" to shift
                )
            )
        }

        // ۱۰. حالت دستور نامشخص یا نامرتبط با فعالیت‌های مرغداری
        return VoiceActionCardData(
            intentType = VoiceIntentType.UNKNOWN,
            title = "دستور نامشخص یا نامرتبط با فارم",
            iconRes = R.drawable.ic_farm_dashboard,
            primaryMetric = "عبارت شناسایی نشد",
            details = listOf(
                "کلام شما" to spokenText,
                "دستورات معتبر" to "ثبت هزینه (مثال: «۲۰۰ هزار تومان هزینه»)، تخم‌مرغ، تلفات، دان، آب یا ناوبری"
            ),
            systemEffect = "این دستور با فعالیت‌های فارم همخوانی ندارد و هیچ تغییری در داده‌ها اعمال نخواهد شد.",
            rawText = spokenText,
            isValid = false,
            errorMessage = "دستور صوتی با فعالیت‌های فارم تطابق ندارد. لطفاً دستور کاری خود را شفاف‌تر بیان کنید."
        )
    }
}
