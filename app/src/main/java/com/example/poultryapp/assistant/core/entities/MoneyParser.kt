package com.example.poultryapp.assistant.core.entities

enum class Currency(val symbolFa: String, val code: String) {
    TOMAN("تومان", "TOMAN"),
    RIAL("ریال", "IRR"),
    USD("دلار", "USD"),
    EUR("یورو", "EUR")
}

data class ExtractedMoney(
    val amount: Double,
    val currency: Currency,
    val amountInTomans: Double,
    val originalSpan: String,
    val startIndex: Int,
    val endIndex: Int
)

object MoneyParser {

    private val CURRENCY_REGEX_MAP = listOf(
        Pair(Regex("""(?:تومان|تومن|\bت\b)"""), Currency.TOMAN),
        Pair(Regex("""(?:ر[یي]ال|\birr\b)"""), Currency.RIAL),
        Pair(Regex("""(?:دلار|\$|dollars?|\busd\b)"""), Currency.USD),
        Pair(Regex("""(?:یورو|euros?|\beur\b)"""), Currency.EUR)
    )

    private val NON_MONEY_UNIT_AFTER_REGEX = Regex(
        """^(?:قطعه|تا|عدد|پرنده|مرغ|جوجه|خروس|بوقلمون|بلدرچین|اردک|غاز|کبک|شانه|شونه|کارتن|بسته|کیلوگرم|کیلو|گرم|کیسه|گونی|لیتر|تن|روز|هفته|ماه|birds?|chickens?|chicks?|turkeys?|quails?|ducks?|hens?|roosters?|eggs?|trays?|kg|kilograms?|grams?|liters?|days?|weeks?)\b"""
    )

    /**
     * Extracts monetary values by checking extracted numbers followed or preceded by currency indicators.
     */
    fun extractMoney(
        text: String,
        extractedNumbers: List<ExtractedNumber>,
        defaultCurrency: Currency = Currency.TOMAN
    ): List<ExtractedMoney> {
        val results = mutableListOf<ExtractedMoney>()

        for (num in extractedNumbers) {
            // Check adjacent text around the number span
            val searchAfterStart = num.endIndex
            val searchAfterEnd = minOf(text.length, searchAfterStart + 18)
            val textAfter = if (searchAfterStart < text.length) text.substring(searchAfterStart, searchAfterEnd) else ""

            val searchBeforeEnd = num.startIndex
            val searchBeforeStart = maxOf(0, searchBeforeEnd - 15)
            val textBefore = if (searchBeforeEnd > 0) text.substring(searchBeforeStart, searchBeforeEnd) else ""
            val widerBeforeStart = maxOf(0, searchBeforeEnd - 50)
            val widerBefore = if (searchBeforeEnd > 0) text.substring(widerBeforeStart, searchBeforeEnd) else ""

            var detectedCurrency: Currency? = null
            var matchedSpan = num.originalSpan
            var spanStart = num.startIndex
            var spanEnd = num.endIndex

            // Check after number first (e.g., "500 هزار تومان")
            for ((regex, curr) in CURRENCY_REGEX_MAP) {
                val matchAfter = regex.find(textAfter)
                if (matchAfter != null && matchAfter.range.first <= 3) {
                    detectedCurrency = curr
                    spanEnd = searchAfterStart + matchAfter.range.last + 1
                    matchedSpan = text.substring(spanStart, spanEnd).trim()
                    break
                }
            }

            // Check before number (e.g., "$500")
            if (detectedCurrency == null) {
                for ((regex, curr) in CURRENCY_REGEX_MAP) {
                    val matchBefore = regex.find(textBefore)
                    if (matchBefore != null && (textBefore.length - matchBefore.range.last) <= 3) {
                        detectedCurrency = curr
                        spanStart = searchBeforeStart + matchBefore.range.first
                        matchedSpan = text.substring(spanStart, spanEnd).trim()
                        break
                    }
                }
            }

            // Guard: when no explicit currency symbol/word follows, do NOT parse a bird/weight/packaging quantity as money
            // (e.g. "فروش ۵۰ قطعه مرغ" or "فروش ۳۰ تا جوجه ۱۵ میلیون").
            val normalizedAfterTrimmed = com.example.poultryapp.assistant.core.normalize.TextNormalizer
                .normalize(textAfter).normalized.trimStart().lowercase()
            if (detectedCurrency == null && NON_MONEY_UNIT_AFTER_REGEX.containsMatchIn(normalizedAfterTrimmed)) {
                continue
            }

            val lowerBefore = textBefore.lowercase()
            val lowerWiderBefore = widerBefore.lowercase()
            val hasMagnitudeWord = num.originalSpan.contains("میلیون") ||
                    num.originalSpan.contains("میلیارد") ||
                    num.originalSpan.contains("هزار") ||
                    num.originalSpan.lowercase().contains("million") ||
                    num.originalSpan.lowercase().contains("thousand")
            val hasSaleOrFinanceInWider = lowerWiderBefore.contains("فروش") ||
                    lowerWiderBefore.contains("فروختم") ||
                    lowerWiderBefore.contains("خرید") ||
                    lowerWiderBefore.contains("هزینه") ||
                    lowerWiderBefore.contains("sold")
            val hasExplicitMoneyPhrase = lowerBefore.contains("مبلغ") || lowerBefore.contains("قیمت") ||
                    lowerWiderBefore.contains("به مبلغ") || lowerWiderBefore.contains("بمبلغ") ||
                    lowerWiderBefore.contains("به قیمت") || lowerWiderBefore.contains("تومان") ||
                    (hasMagnitudeWord && hasSaleOrFinanceInWider) ||
                    (hasSaleOrFinanceInWider && (lowerBefore.endsWith("for ") || lowerBefore.endsWith("به ") || lowerBefore.endsWith("بابت ")))
            val hasFinancialContext = hasExplicitMoneyPhrase ||
                    lowerBefore.contains("هزینه") || lowerBefore.contains("خرج") ||
                    lowerBefore.contains("خرید") || lowerBefore.contains("فروش") ||
                    lowerBefore.contains("فروختم") ||
                    lowerBefore.contains("expense") || lowerBefore.contains("spent") ||
                    lowerBefore.contains("income") || lowerBefore.contains("sold") ||
                    lowerBefore.contains("cost") || lowerBefore.contains("paid")

            if (detectedCurrency == null && !hasFinancialContext) {
                // Not money - plain number
                continue
            }

            val finalCurrency = detectedCurrency ?: defaultCurrency
            val amountTomans = when (finalCurrency) {
                Currency.TOMAN -> num.value
                Currency.RIAL -> num.value / 10.0 // 10 Rials = 1 Toman
                Currency.USD -> num.value * 60_000.0 // Configurable or reference exchange rate
                Currency.EUR -> num.value * 65_000.0
            }

            results.add(
                ExtractedMoney(
                    amount = num.value,
                    currency = finalCurrency,
                    amountInTomans = amountTomans,
                    originalSpan = matchedSpan,
                    startIndex = spanStart,
                    endIndex = spanEnd
                )
            )
        }
        return results.sortedBy { it.startIndex }
    }
}
