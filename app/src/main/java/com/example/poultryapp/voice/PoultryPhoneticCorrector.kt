package com.example.poultryapp.voice

import kotlin.math.max
import kotlin.math.min

/**
 * موتور تصحیح فونتیک و پالایش خطاهای گفتار به متن (ASR) ویژه اصطلاحات تخصصی و بومی مرغداری
 * (Poultry Phonetic & Speech Recognition Error Corrector)
 *
 * قابلیت‌ها:
 * ۱. تبدیل آواهای همسان زبان فارسی (حروف هم‌صدا مانند س/ص/ث، ت/ط، ز/ض/ظ/ذ)
 * ۲. جدول نگاشت عبارات تخصصی مرغداری که توسط Google Speech اشتباه شنیده می‌شوند
 * ۳. الگوریتم فاصله دمرائو-لون‌اشتاین (Damerau-Levenshtein) با تطابق ترانهش (Transposition)
 * ۴. تصحیح فازی نام داروها، نژادها، سویه‌ها و اصطلاحات بالینی
 */
object PoultryPhoneticCorrector {

    /**
     * نگاشت اصطلاحات مرکب و تک‌کلمه‌ای مرغداری به شکل استاندارد
     */
    private val ASR_REPLACEMENTS = listOf(
        // سویه‌ها و نژادها
        Regex("""(?i)\bسویا\s+راس\b""") to "سویه راس",
        Regex("""(?i)\bسویا\s+کاب\b""") to "سویه کاب",
        Regex("""(?i)\bسویا\s+لوهمن\b""") to "سویه لوهمن",
        Regex("""(?i)\bسویا\s+های\s*لاین\b""") to "سویه هایلاین",
        Regex("""(?i)\bسویا\s+بلک\s*استار\b""") to "سویه بلک استار",
        Regex("""(?i)\bسویا\s+آرین\b""") to "سویه آرین",
        Regex("""(?i)\bسویا\s+ارین\b""") to "سویه آرین",
        Regex("""(?i)\bسویه\s+ارین\b""") to "سویه آرین",
        Regex("""(?i)\bسویا\s+هابارد\b""") to "سویه هابارد",
        Regex("""(?i)\bهوبارد\b""") to "هابارد",
        Regex("""(?i)\bسویا\s+آربور\s*اکرز\b""") to "سویه آربور اکرز",
        Regex("""(?i)\bآربوراکرز\b""") to "آربور اکرز",
        Regex("""(?i)\bسویا\s+های\s*سکس\b""") to "سویه های‌سکس",
        Regex("""(?i)\bهایسکس\b""") to "های‌سکس",
        Regex("""(?i)\bهای\s+سکس\b""") to "های‌سکس",
        Regex("""(?i)\bسویا\s+دکالب\b""") to "سویه دکالب",
        Regex("""(?i)\bدیکالب\b""") to "دکالب",
        Regex("""(?i)\bسویا\s+نووژن\b""") to "سویه نووژن",
        Regex("""(?i)\bسویروس\b""") to "سویه راس",
        Regex("""(?i)\bهای\s+لاین\b""") to "هایلاین",
        Regex("""(?i)\bبلک\s+استار\b""") to "بلک استار",
        Regex("""(?i)\bلگهورن\b""") to "لگهورن",
        Regex("""(?i)\bلک\s*هورن\b""") to "لگهورن",
        Regex("""(?i)\bمرندی\b""") to "مرندی",

        // بیماری‌ها و آسیب‌شناسی
        Regex("""(?i)\bانرو\b""") to "انروفلوکساسین",
        Regex("""(?i)\bانروسین\b""") to "انروفلوکساسین",
        Regex("""(?i)\bاسیت\b""") to "آسیت",
        Regex("""(?i)\bکانی\s+بالیسم\b""") to "کانیبالیسم",
        Regex("""(?i)\bکوک\s*سید\s*یوز\b""") to "کوکسیدیوز",
        Regex("""(?i)\bکوکسید\s*یوز\b""") to "کوکسیدیوز",
        Regex("""(?i)\bکوکسیدیوزیس\b""") to "کوکسیدیوز",
        Regex("""(?i)\bنیو\s*کاسل\b""") to "نیوکاسل",
        Regex("""(?i)\bگام\s*برو\b""") to "گامبورو",
        Regex("""(?i)\bگامبرو\b""") to "گامبورو",
        Regex("""(?i)\bبرون\s*شیت\b""") to "برونشیت",
        Regex("""(?i)\bمایکو\s*پلاسما\b""") to "مایکوپلاسما",
        Regex("""(?i)\bمایکرو\s*پلاسما\b""") to "مایکوپلاسما",
        Regex("""(?i)\bکوری\s*زا\b""") to "کوریزا",
        Regex("""(?i)\bانترو\s*توکسمی\b""") to "انتروتوکسمی",
        Regex("""(?i)\bپودو\s*درماتیت\b""") to "پودودرماتیت",
        Regex("""(?i)\bلهیب\s*می\s*زن\b""") to "له‌له می‌زنند",
        Regex("""(?i)\bلهیب\s*میزنن\b""") to "له‌له می‌زنند",
        Regex("""(?i)\bلهیب\b""") to "له‌له",
        Regex("""(?i)\bدهنشون\s*بازه\b""") to "دهان باز",

        // واژه‌های نامفهوم و اعوجاج صوتی ناشی از سر و صدای فن‌ها و تجهیزات
        Regex("""(?i)\bسبت\b""") to "ثبت",
        Regex("""(?i)\bسارن\b""") to "سالن",
        Regex("""(?i)\bارائم\b""") to "علائم",
        Regex("""(?i)\bبیمری\b""") to "بیماری",
        Regex("""(?i)\bچیح\b""") to "چیه"
    )

    /**
     * فهرست اصطلاحات مرجع مرغداری جهت تطابق فازی
     */
    val CANONICAL_TERMS = listOf(
        // داروها و واکسن‌ها
        "انروفلوکساسین", "فلورفنیکل", "داکسی‌سایکلین", "تایلوزین", "آمپرولیوم",
        "تولترازوریل", "لوامیزول", "کولیستین", "فسفومایسین", "سولفادیمیدین",
        "ویتامین", "الکترولیت", "پروبیوتیک", "توکسین‌بایندر", "دیاتومه", "سرکه سیب",
        "واکسن نیوکاسل", "واکسن گامبورو", "واکسن برونشیت", "واکسن آنفلوآنزا", "واکسن آبله",

        // بیماری‌ها
        "کوکسیدیوز", "نیوکاسل", "گامبورو", "برونشیت", "آسیت", "کوریزا",
        "مایکوپلاسما", "کانیبالیسم", "سالمونلا", "کلی‌باسیلوز", "پودودرماتیت",
        "مارک", "آبله", "کوکسیدیوز روده", "کوکسیدیوز سکومی",

        // اصطلاحات فنی و تجهیزات
        "FCR", "EPEF", "پولت", "نیمچه", "کرامبل", "پلت", "مش", "پیش‌آغازین", "آغازین", "رشد", "پایانی",
        "تخم لمبه", "پوسته نرم", "نوک‌چینی", "تولک‌بری", "فوتوپریود", "تهویه تونلی", "پد سلولزی",
        "نیپل", "آبخوری", "دانخوری", "رطوبت بستر", "گاز آمونیاک", "کندلینگ", "ستر", "هچر",
        "جت هیتر", "اینلت", "هواکش", "دیمر", "مه‌پاش", "ستون آب",

        // گونه‌ها و سویه‌ها
        "بلک استار", "راس ۳۰۸", "کاب ۵۰۰", "لوهمن", "هایلاین", "لگهورن", "مرندی",
        "آرین", "هابارد", "آربور اکرز", "های‌سکس", "دکالب", "نووژن", "لاری",
        "بلدرچین", "بوقلمون", "شترمرغ", "کبک", "قرقاول", "اردک", "غاز", "کبوتر", "قناری"
    )

    /**
     * نگاشت فونتیک آواهای همسان فارسی
     */
    fun phoneticNormalize(text: String): String {
        return text.trim()
            .replace('ص', 'س')
            .replace('ث', 'س')
            .replace('ط', 'ت')
            .replace('ح', 'ه')
            .replace('ذ', 'ز')
            .replace('ض', 'ز')
            .replace('ظ', 'ز')
            .replace('غ', 'ق')
            .replace('ئ', 'ی')
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
    }

    /**
     * محاسبه فاصله دمرائو-لون‌اشتاین بین دو رشته
     * شامل جابجایی دو حرف مجاور (Transposition)، حذف، درج و جایگزینی
     */
    fun damerauLevenshteinDistance(source: String, target: String): Int {
        val s = source.lowercase()
        val t = target.lowercase()
        val sLen = s.length
        val tLen = t.length

        if (sLen == 0) return tLen
        if (tLen == 0) return sLen

        val d = Array(sLen + 1) { IntArray(tLen + 1) }

        for (i in 0..sLen) d[i][0] = i
        for (j in 0..tLen) d[0][j] = j

        for (i in 1..sLen) {
            for (j in 1..tLen) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1

                var minCost = min(d[i - 1][j] + 1, min(d[i][j - 1] + 1, d[i - 1][j - 1] + cost))

                // Transposition
                if (i > 1 && j > 1 && s[i - 1] == t[j - 2] && s[i - 2] == t[j - 1]) {
                    minCost = min(minCost, d[i - 2][j - 2] + cost)
                }

                d[i][j] = minCost
            }
        }

        return d[sLen][tLen]
    }

    /**
     * درصد تشابه دو رشته (بین 0.0 تا 1.0)
     */
    fun similarity(s1: String, s2: String): Double {
        val p1 = phoneticNormalize(s1)
        val p2 = phoneticNormalize(s2)
        val maxLen = max(p1.length, p2.length)
        if (maxLen == 0) return 1.0
        val dist = damerauLevenshteinDistance(p1, p2)
        return 1.0 - (dist.toDouble() / maxLen)
    }

    /**
     * یافتن نزدیک‌ترین عبارت استاندارد در صورت خطای شنیداری
     */
    fun findBestFuzzyMatch(token: String, candidates: List<String> = CANONICAL_TERMS, threshold: Double = 0.80): String? {
        val cleanToken = phoneticNormalize(token)
        if (cleanToken.length < 3) return null

        var bestMatch: String? = null
        var highestSim = 0.0

        for (candidate in candidates) {
            val sim = similarity(cleanToken, candidate)
            if (sim >= threshold && sim > highestSim) {
                highestSim = sim
                bestMatch = candidate
            }
        }

        return bestMatch
    }

    /**
     * اعمال اصلاحات فونتیک و تصحیح خطاهای شنیداری بر روی متن ورودی
     */
    fun correctSpeechErrors(text: String): String {
        var result = text

        // ۱. اعمال الگوهای رگولار اشتباهات متداول ASR
        for ((pattern, replacement) in ASR_REPLACEMENTS) {
            result = pattern.replace(result, replacement)
        }

        // ۲. تصحیح کلمه «سویا» زمانی که در معنای سویه جوجه/مرغ به کار رفته است
        result = result.replace(Regex("""(?<=\s|^)سویا(?=\s+(?:راس|کاب|لوهمن|هایلاین|بلک|آرین|ارین|هابارد|آربور|های‌سکس|هایسکس|دکالب|نووژن|مرندی|لاری|گوشتی|تخمگذار|پرورشی|مرغ|جوجه|۳۰۸|500))"""), "سویه")

        // ۳. تصحیح دو سطحی آسیت (Dual-Tier Ascites vs Acidifier Fix):
        // الف) تصحیح قطعی و بدون قید خطای شنیداری «اسیت» (با ت) -> «آسیت»
        result = result.replace(Regex("""(?i)\bاسیت\b"""), "آسیت")

        // ب) تصحیح مشروط و محافظت‌شده «اسید» (با د):
        // استثنا کردن اسیدهای خوراکی و ترکیبات اسیدی جیره
        val isDietaryAcid = result.contains("اسید ارگانیک") || result.contains("اسید سیتریک") ||
            result.contains("اسید چرب") || result.contains("اسیدهای چرب") ||
            result.contains("اسید آمینه") || result.contains("اسیدهای آمینه") ||
            result.contains("اسیدیفایر") || result.contains("اسید پروپیونیک") ||
            result.contains("اسید بوتیریک") || result.contains("اسید فرمیک") ||
            result.contains("اسید فسفریک") || result.contains("اسیدوز") ||
            result.contains("اسیدوفیلوس")

        // احراز صریح علائم و نشانه‌های پاتولوژیک آسیت و پرفشاری ریوی
        val hasAscitesPathologyContext = result.contains("شکم") ||
            result.contains("باد کرده") ||
            result.contains("آب آورده") ||
            result.contains("اب اورده") ||
            result.contains("کبودی") ||
            result.contains("تنگی نفس") ||
            result.contains("سیاه شدن تاج") ||
            result.contains("آب آوردگی شکم")

        // جایگزینی «اسید» با «آسیت» صرفاً در صورت وجود علائم بالینی و عدم ذکر اسیدهای خوراکی
        if (hasAscitesPathologyContext && !isDietaryAcid) {
            result = result.replace(Regex("""(?i)\bاسید\b"""), "آسیت")
        }

        return result
    }
}

