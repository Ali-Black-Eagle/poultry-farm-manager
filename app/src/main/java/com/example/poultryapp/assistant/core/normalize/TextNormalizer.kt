package com.example.poultryapp.assistant.core.normalize

import java.text.Normalizer

enum class Lang {
    EN,
    FA,
    UNKNOWN
}

data class NormalizedText(
    val original: String,
    val normalized: String,
    val matchKey: String,
    val tokens: List<String>,
    val lang: Lang
)

object TextNormalizer {

    private val DIGIT_MAPPINGS = mapOf(
        '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
        '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9',
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    private val DIACRITICS_REGEX = Regex("[\u064B-\u065F\u0670\u0640]") // Tanwin, Harakat, Tatweel
    private val DECIMAL_MOMAYYEZ_REGEX = Regex("""(?<=\d)\s*٫\s*(?=\d)""")
    private val THOUSANDS_SEPARATOR_REGEX = Regex("""(?<=\d)[٬،,](?=\d)""")
    private val PERSIAN_COUNTER_SPLIT_REGEX = Regex("""(?<=\d)تا\b""")

    /**
     * Common conversational prefixes, clitics, and polite fillers in Persian speech.
     */
    private val PERSIAN_POLITE_FILLERS = listOf(
        "لطفا", "لطفاً", "بی زحمت", "بی‌زحمت", "زحمت بکش", "ممنون", "داداش", "مهندس", "حاجی"
    )

    fun normalize(raw: String): NormalizedText {
        if (raw.isBlank()) {
            return NormalizedText(
                original = raw,
                normalized = "",
                matchKey = "",
                tokens = emptyList(),
                lang = Lang.UNKNOWN
            )
        }

        // 1. Unicode NFKC
        var s = Normalizer.normalize(raw, Normalizer.Form.NFKC)

        // 2. Character mappings
        s = s.replace('ي', 'ی')
            .replace('ى', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('ٱ', 'ا')
            .replace('؟', '?')
            .replace('٪', '%')

        // 3. Remove diacritics and tatweel
        s = DIACRITICS_REGEX.replace(s, "")

        // 4. Digits to ASCII 0-9
        val sbDigits = StringBuilder()
        for (ch in s) {
            sbDigits.append(DIGIT_MAPPINGS[ch] ?: ch)
        }
        s = sbDigits.toString()

        // 5. Persian momayyez (٫) & slash decimal separator to standard dot (.)
        s = DECIMAL_MOMAYYEZ_REGEX.replace(s, ".")

        // 6. Thousands separator (، and ,) removed inside numbers
        s = THOUSANDS_SEPARATOR_REGEX.replace(s, "")

        // 7. Split Persian counter 'تا' (e.g. 120تا -> 120 تا)
        s = PERSIAN_COUNTER_SPLIT_REGEX.replace(s, " تا")

        // 8. Collapse whitespace
        s = s.replace(Regex("""\s+"""), " ").trim()

        // 9. Language detection: Arabic-script letters >= 30% of all letters -> FA
        val arabicLetterCount = s.count { it in '\u0600'..'\u06FF' && it.isLetter() }
        val totalLetters = s.count { it.isLetter() }
        val lang = when {
            totalLetters > 0 && (arabicLetterCount.toFloat() / totalLetters) >= 0.30f -> Lang.FA
            totalLetters > 0 -> Lang.EN
            else -> Lang.UNKNOWN
        }

        // 10. MatchKey: Lowercase, remove ZWNJ, replace punctuation with space, collapse whitespace
        val matchKey = s.lowercase()
            .replace("\u200C", "")
            .replace(Regex("""[^\p{L}\p{Nd}\s.]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // 11. Tokens: ZWNJ treated as space, punctuation separated
        val tokenizedString = s.lowercase()
            .replace("\u200C", " ")
            .replace(Regex("""[^\p{L}\p{Nd}\s.]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        val tokens = if (tokenizedString.isEmpty()) {
            emptyList()
        } else {
            tokenizedString.split(" ").filter { it.isNotBlank() }
        }

        return NormalizedText(
            original = raw,
            normalized = s,
            matchKey = matchKey,
            tokens = tokens,
            lang = lang
        )
    }

    /**
     * Damerau-Levenshtein distance for fuzzy matching against keywords and entity names.
     * Thresholds:
     * - Length <= 3: 0 edits
     * - Length 4 to 6: 1 edit
     * - Length >= 7: 2 edits
     */
    fun isFuzzyMatch(token: String, target: String): Boolean {
        if (token == target) return true
        val maxLen = maxOf(token.length, target.length)
        val maxAllowedEdits = when {
            maxLen <= 3 -> 0
            maxLen <= 6 -> 1
            else -> 2
        }
        if (maxAllowedEdits == 0) return false
        val dist = damerauLevenshteinDistance(token, target)
        return dist <= maxAllowedEdits
    }

    fun damerauLevenshteinDistance(source: String, target: String): Int {
        val srcLen = source.length
        val tgtLen = target.length
        if (srcLen == 0) return tgtLen
        if (tgtLen == 0) return srcLen

        val d = Array(srcLen + 1) { IntArray(tgtLen + 1) }

        for (i in 0..srcLen) d[i][0] = i
        for (j in 0..tgtLen) d[0][j] = j

        for (i in 1..srcLen) {
            val sChar = source[i - 1]
            for (j in 1..tgtLen) {
                val tChar = target[j - 1]
                val cost = if (sChar == tChar) 0 else 1

                d[i][j] = minOf(
                    d[i - 1][j] + 1,       // deletion
                    d[i][j - 1] + 1,       // insertion
                    d[i - 1][j - 1] + cost // substitution
                )

                // Transposition
                if (i > 1 && j > 1 && sChar == target[j - 2] && source[i - 2] == tChar) {
                    d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + cost)
                }
            }
        }
        return d[srcLen][tgtLen]
    }
}
