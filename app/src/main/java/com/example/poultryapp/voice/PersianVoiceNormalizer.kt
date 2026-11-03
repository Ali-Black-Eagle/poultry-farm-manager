package com.example.poultryapp.voice

object PersianVoiceNormalizer {

    private val wordToNumberMap = mapOf(
        "صفر" to 0L, "یک" to 1L, "یه" to 1L, "یکی" to 1L, "دو" to 2L, "دوتا" to 2L, "سه" to 3L, "سه تا" to 3L, "چهار" to 4L, "چهارتا" to 4L, "پنج" to 5L, "پنج تا" to 5L,
        "شش" to 6L, "شیش" to 6L, "هفت" to 7L, "هشت" to 8L, "نه" to 9L, "ده" to 10L,
        "یازده" to 11L, "دوازده" to 12L, "سیزده" to 13L, "چهارده" to 14L, "پانزده" to 15L, "پونزده" to 15L,
        "شانزده" to 16L, "شونزده" to 16L, "هفده" to 17L, "هجده" to 18L, "هیجده" to 18L, "نوزده" to 19L,
        "بیست" to 20L, "سی" to 30L, "چهل" to 40L, "پنجاه" to 50L,
        "شصت" to 60L, "هفتاد" to 70L, "هشتاد" to 80L, "نود" to 90L,
        "صد" to 100L, "دویست" to 200L, "سیصد" to 300L, "چهارصد" to 400L, "پانصد" to 500L, "پونصد" to 500L,
        "ششصد" to 600L, "شیشصد" to 600L, "هفتصد" to 700L, "هفصد" to 700L, "هشتصد" to 800L, "هشصد" to 800L, "نهصد" to 900L,
        "هزار" to 1000L, "میلیون" to 1000000L, "میلیارد" to 1000000000L
    )

    fun wordToNumber(word: String): Long? = wordToNumberMap[word.trim()]

    /**
     * استانداردسازی متن فارسی، تبدیل اعراب، ارقام فارسی/عربی، نشانه‌های نگارشی و تسامح گویش‌های مرغداری.
     */
    fun normalize(text: String): String {
        val correctedInput = PoultryPhoneticCorrector.correctSpeechErrors(text)
        var result = correctedInput.trim()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
            .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
            .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
            .replace(Regex("[\u064B-\u065F\u0670]"), "") // حذف اعراب و تنوین
            .replace('\u200c', ' ') // تبدیل نیم‌فاصله به فاصله

        // ۱. نرمال‌سازی ممیز فارسی (٫) و اسلش اعشاری تاریخ‌محافظت‌شده (Date-Safe Slash)
        result = result
            .replace(Regex("""(?<=\d)\s*[٫]\s*(?=\d)"""), ".")
            .replace(Regex("""(?<!\b\d{1,4}/\d{1,2})(?<=\b\d{1,3})/(?=\d{1,3}\b(?!/\d{1,2}))"""), ".")

        // ۲. حذف نشانه‌های نگارشی و اموجی‌ها
        result = result
            .replace(Regex("[،,؛;?!«»\"'():\\-_]"), " ")
            .replace(Regex("[\\p{So}\\p{Sk}]"), " ") // حذف اموجی‌ها و آیکون‌های متفرقه

        // ۳. نرمال‌سازی پسوندهای ضمایر متصل (ـش، ـشون، ـمون) بدون تخریب ریشه واژگان
        result = result
            .replace(Regex("""(?<=\s|^)ثبتش(?=\s+کن|\s*$)"""), "ثبت")
            .replace(Regex("""(?<=\s|^)ثبتشون(?=\s+کن|\s*$)"""), "ثبت")
            .replace(Regex("""(?<=\s|^)(?:دونشون|دانشون|دونمون|دانمون)(?=\s|$)"""), "دان")
            .replace(Regex("""(?<=\s|^)(?:تلفاتمون|تلفاتشون|تلفاتش)(?=\s|$)"""), "تلفات")
            .replace(Regex("""(?<=\s|^)(?:آبشون|ابشون|آبمون|ابمون|آبش|ابش)(?=\s|$)"""), "آب")
            .replace(Regex("""(?<=\s|^)(?:دماش|دماشون|دماشو|دماهاش)(?=\s|$)"""), "دما")
            .replace(Regex("""(?<=\s|^)(?:سالنش|سالنشون|سالنمون)(?=\s|$)"""), "سالن")
            .replace(Regex("""(?<=\s|^)(?:گله\s*اش|گله\s*شون|گله\s*مون)(?=\s|$)"""), "گله")

        // ۴. جایگزینی ایمن کلمات اعشاری رایج (ابتدا عبارات مرکب، سپس نیم مستقل با مرز کلمه)
        result = result
            .replace("یک و نیم", "1.5")
            .replace("دو و نیم", "2.5")
            .replace("سه و نیم", "3.5")
            .replace("چهار و نیم", "4.5")
            .replace("پنج و نیم", "5.5")
            .replace("شش و نیم", "6.5")
            .replace("هفت و نیم", "7.5")
            .replace("هشت و نیم", "8.5")
            .replace("نه و نیم", "9.5")
            .replace("ده و نیم", "10.5")
            .replace(Regex("""(?<=\s|^)نیم(?=\s|$)"""), "0.5")

        // ۵. همسان‌سازی واژگان عامیانه مالی، بسته‌بندی و اصطلاحات بومی مرغداری
        result = result.replace("تومن", "تومان")
            .replace("درامد", "درآمد")
            .replace("خرجی", "هزینه")
            .replace("سولن", "سالن")
            .replace("سالون", "سالن")
            .replace("سالونه", "سالن")
            .replace("تخممرغ", "تخم مرغ")
            .replace("کارتون", "کارتن")
            .replace("شونه", "شانه")
            .replace("کیسهدان", "کیسه دان")
            .replace("گونیدان", "کیسه دان")
            .replace("گونی دان", "کیسه دان")
            .replace("کیسه دون", "کیسه دان")
            .replace("دون دادیم", "دان دادیم")
            .replace("دون مصرف", "دان مصرف")
            .replace("دون ریختیم", "دان دادیم")
            .replace("تخم جمع کردیم", "تخم مرغ")
            .replace(Regex("""(?<=\s|^)دون(?=\s|$)"""), "دان")
            .replace("پری روز", "پریروز")
            .replace("پس فردا", "پسفردا")
            .replace("دو شنبه", "دوشنبه")
            .replace("سه شنبه", "سه‌شنبه")
            .replace("چهار شنبه", "چهارشنبه")
            .replace("پنج شنبه", "پنجشنبه")
            .replace("یک شنبه", "یکشنبه")
            .replace("جوجه کشی", "جوجه‌کشی")
            .replace("شتر مرغ", "شترمرغ")
            .replace("تلف شده", "تلفات")
            .replace("تلف شدن", "تلفات")
            .replace("تلف شدند", "تلفات")
            .replace("مردن", "تلفات")
            .replace("مردند", "تلفات")
            .replace("مرگ و میر", "تلفات")
            .replace("مرگ و میر داشتیم", "تلفات")
            .replace("تلف دادیم", "تلفات")
            .replace("تلفات دادیم", "تلفات")
            .replace("تلف شد", "تلفات")
            .replace("چیندون", "چینه‌دان")
            .replace("کرک", "مرغ کرچ")
            .replace("کورک", "مرغ کرچ")
            .replace("تویوق‌لار", "مرغ‌ها")
            .replace("تویوقلار", "مرغ‌ها")
            .replace("تویوق", "مرغ")
            .replace("یومورتا", "تخم مرغ")
            .replace("قویوبلار", "گذاشتند")
            .replace("دنه", "دانه")
            .replace("بیره", "یک")
            .replace("یاز", "بنویس")
            .replace("چتی", "چطور")
            .replace("بویه", "شده")
            .replace("شیشک", "نیمچه")
            .replace("چوری", "جوجه")
            .replace("آب خوری", "آبخوری")
            .replace("دان خوری", "دانخوری")
            .replace("زنگ بزن", "تماس بگیر")
            .replace("تماس با", "تماس بگیر با")
            .replace("پیام بده", "ارسال پیامک")
            .replace("اس ام اس", "ارسال پیامک")
            .replace("پیامک بزن", "ارسال پیامک")
            .replace("چیکار کنیم", "چیکار کنم")
            .replace("چه باید کرد", "چیکار کنم")
            .replace("چه کار باید کرد", "چیکار کنم")

        return result.replace(Regex("\\s+"), " ").trim()
    }

    val WEEKDAY_MAP = mapOf(
        "شنبه" to 0,
        "یکشنبه" to 1,
        "دوشنبه" to 2,
        "سه‌شنبه" to 3,
        "چهارشنبه" to 4,
        "پنجشنبه" to 5,
        "جمعه" to 6
    )

    fun findWeekday(norm: String): Pair<String, Int>? {
        for ((name, index) in WEEKDAY_MAP) {
            if (norm.contains(name)) {
                return Pair(name, index)
            }
        }
        return null
    }

    /**
     * استخراج مبلغ مالی بر حسب تومان با پشتیبانی از ریال، تومان، ضرایب هزار، میلیون و میلیارد.
     */
    fun extractFinancialAmountTomans(text: String): Double? {
        val norm = normalize(text)
        val isRials = norm.contains("ریال")

        // ۱. بررسی ارقام عددی دارای ضریب مانند "200 هزار", "5 میلیون", "1.5 میلیون"
        val scaleRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(هزار|میلیون|میلیارد)")
        val scaleMatch = scaleRegex.find(norm)
        if (scaleMatch != null) {
            val base = scaleMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val scaleWord = scaleMatch.groupValues[2]
            val mult = when (scaleWord) {
                "هزار" -> 1_000.0
                "میلیون" -> 1_000_000.0
                "میلیارد" -> 1_000_000_000.0
                else -> 1.0
            }
            val amount = base * mult
            return if (isRials) amount / 10.0 else amount
        }

        // ۲. بررسی اعداد کلامی مانند "دویست هزار", "پنج میلیون و دویست هزار"
        val wordAmount = extractFromWords(norm)
        if (wordAmount != null && wordAmount > 0.0) {
            return if (isRials) wordAmount / 10.0 else wordAmount
        }

        // ۳. بررسی ارقام عددی مستقیم مانند "200000" یا "50000"
        val directDigits = Regex("(\\d+(?:\\.\\d+)?)").find(norm)
        if (directDigits != null) {
            val raw = directDigits.groupValues[1].toDoubleOrNull() ?: 0.0
            if (raw > 0.0) {
                return if (isRials) raw / 10.0 else raw
            }
        }

        return null
    }

    /**
     * استخراج عدد کلامی یا عددی از رشته متن فارسی با امکان استثنا کردن یک بخش (مانند شماره سالن).
     */
    fun extractFirstNumber(text: String, excludePhrase: String? = null): Double? {
        var normalized = normalize(text)
        if (!excludePhrase.isNullOrBlank()) {
            val normExclude = normalize(excludePhrase)
            normalized = normalized.replace(normExclude, " ")
        }

        // اگر کاربر کلام قبلی خود را تصحیح کرده است (مانند: "۲۰۰ کیلو نه بکنش ۲۵۰" یا "اشتباه شد ۳۰۰")
        val correctionTokens = listOf("نه بکنش", "نه ببخشید", "نه", "اشتباه شد", "صبر کن", "بکنش", "تغییر بده به", "عوض کن به", "اصلاح")
        for (ct in correctionTokens) {
            val idx = normalized.lastIndexOf(ct)
            if (idx != -1 && idx + ct.length < normalized.length) {
                val afterPart = normalized.substring(idx + ct.length).trim()
                val numAfter = extractFirstNumberInternal(afterPart)
                if (numAfter != null && numAfter > 0.0) {
                    return numAfter
                }
            }
        }

        return extractFirstNumberInternal(normalized)
    }

    private fun extractFirstNumberInternal(normalizedInput: String): Double? {
        var normalized = normalizedInput
            .replace(Regex("(?:(\\d+|یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده)\\s+روز\\s+(?:پیش|قبل|بعد|دیگه|آینده))"), " ")
            .replace(Regex("ساعت\\s*\\d+(?::\\d+)?"), " ")
            .replace(Regex("ساعت\\s*(?:یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده|یازده|دوازده)"), " ")
            .replace(Regex("\\s+"), " ").trim()

        // اول بررسی ارقام رقمی همراه با ضریب (مانند "200 هزار" یا "1.5 میلیون")
        val digitScaleMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(هزار|میلیون|میلیارد)").find(normalized)
        if (digitScaleMatch != null) {
            val base = digitScaleMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val scaleWord = digitScaleMatch.groupValues[2]
            val mult = when (scaleWord) {
                "هزار" -> 1_000.0
                "میلیون" -> 1_000_000.0
                "میلیارد" -> 1_000_000_000.0
                else -> 1.0
            }
            return base * mult
        }

        // پردازش کلمات عددی ترکیبی (مانند "بیست و پنج", "صد و بیست", "دویست هزار", "هفتصد و پنجاه")
        val wordResult = extractFromWords(normalized)
        if (wordResult != null) {
            return wordResult
        }

        // بررسی اعداد رقمی مستقیم مستقل (مانند "20" یا "3.5")
        val digitMatch = Regex("(\\d+(\\.\\d+)?)").find(normalized)
        if (digitMatch != null) {
            val num = digitMatch.groupValues[1].toDoubleOrNull()
            if (num != null) return num
        }

        return null
    }

    private fun extractFromWords(text: String): Double? {
        val tokens = text.split(" ")
        var total = 0.0
        var currentSum = 0.0
        var foundAny = false

        for (token in tokens) {
            val cleanToken = token.trim()
            if (cleanToken == "و") continue

            val value = wordToNumberMap[cleanToken]
            if (value != null) {
                foundAny = true
                if (value == 1000L || value == 1000000L || value == 1000000000L) {
                    val multiplier = if (currentSum == 0.0) 1.0 else currentSum
                    total += multiplier * value
                    currentSum = 0.0
                } else {
                    currentSum += value
                }
            } else if (currentSum > 0.0 || total > 0.0) {
                // اتمام بخش عددی
                break
            }
        }

        val result = total + currentSum
        return if (foundAny) result else null
    }

    /**
     * استخراج سن پرنده به هفته یا روز از کلام صوتی کاربر (مثلاً «جوجه‌های ۳ هفته»، «۲۵ روزه»، «مرغ تخمگذار»).
     * خروجی: Pair(مقدار عددی, "WEEKS" یا "DAYS")
     */
    fun extractFlockAge(text: String): Pair<Int, String>? {
        val norm = normalize(text)

        // ۱. بررسی کلمات کلیدی مراحل رشد
        if (norm.contains("یک روزه") || norm.contains("یکروزه") || norm.contains("تازه هچ") || norm.contains("نوزاد")) {
            return Pair(1, "DAYS")
        }
        if (norm.contains("تخمگذار") || norm.contains("تخم گذار") || norm.contains("تخمگذاری") || norm.contains("تولید تخم")) {
            return Pair(25, "WEEKS") // سن بلوغ تخم‌گذاری
        }
        if (norm.contains("پولت") || norm.contains("نیمچه")) {
            return Pair(12, "WEEKS")
        }

        // ۲. بررسی ارقام عددی به همراه هفته (مانند "3 هفته", "2 هفتگی", "هفته 4")
        val weekRegex1 = Regex("(\\d+)\\s*(هفته|هفتگی|هفته ای)")
        val matchWeek1 = weekRegex1.find(norm)
        if (matchWeek1 != null) {
            val w = matchWeek1.groupValues[1].toIntOrNull()
            if (w != null && w > 0) return Pair(w, "WEEKS")
        }

        val weekRegex2 = Regex("هفته\\s*(\\d+)")
        val matchWeek2 = weekRegex2.find(norm)
        if (matchWeek2 != null) {
            val w = matchWeek2.groupValues[1].toIntOrNull()
            if (w != null && w > 0) return Pair(w, "WEEKS")
        }

        // کلمات عددی هفته (مانند "سه هفتگی")
        val weekWords = mapOf("یک" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5, "شش" to 6, "شیش" to 6, "هفت" to 7, "هشت" to 8, "نه" to 9, "ده" to 10)
        for ((word, num) in weekWords) {
            if (norm.contains("$word هفته") || norm.contains("$word هفتگی")) {
                return Pair(num, "WEEKS")
            }
        }

        // ۳. بررسی ارقام عددی به همراه روز (مانند "25 روزه", "10 روزگی", "سن 40 روز")
        val dayRegex1 = Regex("(\\d+)\\s*(روزه|روزگی|روز)")
        val matchDay1 = dayRegex1.find(norm)
        if (matchDay1 != null) {
            val d = matchDay1.groupValues[1].toIntOrNull()
            if (d != null && d > 0) return Pair(d, "DAYS")
        }

        val dayRegex2 = Regex("سن\\s*(\\d+)\\s*روز")
        val matchDay2 = dayRegex2.find(norm)
        if (matchDay2 != null) {
            val d = matchDay2.groupValues[1].toIntOrNull()
            if (d != null && d > 0) return Pair(d, "DAYS")
        }

        return null
    }
}
