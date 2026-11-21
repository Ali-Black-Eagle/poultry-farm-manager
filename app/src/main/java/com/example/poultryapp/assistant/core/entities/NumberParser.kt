package com.example.poultryapp.assistant.core.entities

data class ExtractedNumber(
    val value: Double,
    val isInteger: Boolean,
    val originalSpan: String,
    val startIndex: Int,
    val endIndex: Int
) {
    val longValue: Long get() = value.toLong()
    val intValue: Int get() = value.toInt()
}

object NumberParser {

    private val PERSIAN_UNITS = mapOf(
        "صفر" to 0.0, "یک" to 1.0, "یه" to 1.0, "یکی" to 1.0,
        "دو" to 2.0, "سه" to 3.0, "چهار" to 4.0, "پنج" to 5.0,
        "شش" to 6.0, "شیش" to 6.0, "هفت" to 7.0, "هشت" to 8.0, "نه" to 9.0
    )

    private val PERSIAN_TEENS = mapOf(
        "ده" to 10.0, "یازده" to 11.0, "دوازده" to 12.0, "سیزده" to 13.0,
        "چهارده" to 14.0, "پانزده" to 15.0, "پونزده" to 15.0, "شانزده" to 16.0,
        "شونزده" to 16.0, "هفده" to 17.0, "هجده" to 18.0, "نوزده" to 19.0
    )

    private val PERSIAN_TENS = mapOf(
        "بیست" to 20.0, "سی" to 30.0, "چهل" to 40.0, "پنجاه" to 50.0,
        "شصت" to 60.0, "هفتاد" to 70.0, "هشتاد" to 80.0, "نود" to 90.0
    )

    private val PERSIAN_HUNDREDS = mapOf(
        "صد" to 100.0, "یکصد" to 100.0, "دویست" to 200.0, "سیصد" to 300.0,
        "چهارصد" to 400.0, "پانصد" to 500.0, "پونصد" to 500.0, "ششصد" to 600.0,
        "هفتصد" to 700.0, "هشتصد" to 800.0, "نهصد" to 900.0
    )

    private val PERSIAN_MULTIPLIERS = mapOf(
        "هزار" to 1_000.0,
        "میلیون" to 1_000_000.0,
        "میلیارد" to 1_000_000_000.0
    )

    private val ENGLISH_WORDS = mapOf(
        "zero" to 0.0, "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0,
        "five" to 5.0, "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0,
        "ten" to 10.0, "eleven" to 11.0, "twelve" to 12.0, "thirteen" to 13.0,
        "fourteen" to 14.0, "fifteen" to 15.0, "sixteen" to 16.0, "seventeen" to 17.0,
        "eighteen" to 18.0, "nineteen" to 19.0, "twenty" to 20.0, "thirty" to 30.0,
        "forty" to 40.0, "fifty" to 50.0, "sixty" to 60.0, "seventy" to 70.0,
        "eighty" to 80.0, "ninety" to 90.0, "hundred" to 100.0
    )

    private val ENGLISH_MULTIPLIERS = mapOf(
        "thousand" to 1_000.0, "k" to 1_000.0,
        "million" to 1_000_000.0, "m" to 1_000_000.0,
        "billion" to 1_000_000_000.0, "b" to 1_000_000_000.0
    )

    /**
     * Parse all number instances from normalized text tokens, identifying contiguous runs of number words
     * and standalone digits (e.g. 120, 2.5k, یک میلیون و پانصد هزار).
     */
    fun extractNumbers(text: String): List<ExtractedNumber> {
        val results = mutableListOf<ExtractedNumber>()
        val normalized = com.example.poultryapp.assistant.core.normalize.TextNormalizer.normalize(text).normalized
        if (normalized.isBlank()) return emptyList()

        val words = normalized.split(" ")
        val wordStarts = IntArray(words.size)
        var curPos = 0
        for (idx in words.indices) {
            wordStarts[idx] = curPos
            curPos += words[idx].length + 1
        }

        // 1. Parse Persian number phrases (spelled-out or mixed with digits, multipliers, counters)
        var i = 0
        while (i < words.size) {
            val word = words[i]
            if (isPersianNumberToken(word)) {
                val runTokens = mutableListOf<String>()
                var j = i
                var hasNonDigitWord = false

                while (j < words.size) {
                    val currentWord = words[j]
                    if (isPersianNumberToken(currentWord) || currentWord == "و" || currentWord == "تا") {
                        if (currentWord != "و" && currentWord.toDoubleOrNull() == null) {
                            hasNonDigitWord = true
                        }
                        runTokens.add(currentWord)
                        j++
                    } else {
                        break
                    }
                }

                // Strip trailing joiner
                while (runTokens.isNotEmpty() && runTokens.last() == "و") {
                    runTokens.removeAt(runTokens.size - 1)
                    j--
                }

                if (hasNonDigitWord && runTokens.isNotEmpty()) {
                    val parsedVal = parsePersianNumberRun(runTokens)
                    if (parsedVal != null) {
                        val startIndex = wordStarts[i]
                        val endIndex = wordStarts[j - 1] + words[j - 1].length
                        val span = normalized.substring(startIndex, endIndex)
                        results.add(
                            ExtractedNumber(
                                value = parsedVal,
                                isInteger = parsedVal % 1.0 == 0.0,
                                originalSpan = span,
                                startIndex = startIndex,
                                endIndex = endIndex
                            )
                        )
                        i = j
                        continue
                    }
                }
            }
            i++
        }

        // 2. Parse English number phrases (e.g. "one hundred twenty", "1.5 million")
        i = 0
        while (i < words.size) {
            val word = words[i].lowercase()
            if (isEnglishNumberToken(word)) {
                val runTokens = mutableListOf<String>()
                var j = i
                var hasNonDigitWord = false

                while (j < words.size) {
                    val current = words[j].lowercase()
                    if (isEnglishNumberToken(current) || current == "and") {
                        if (current != "and" && current.toDoubleOrNull() == null) {
                            hasNonDigitWord = true
                        }
                        runTokens.add(current)
                        j++
                    } else {
                        break
                    }
                }

                while (runTokens.isNotEmpty() && runTokens.last() == "and") {
                    runTokens.removeAt(runTokens.size - 1)
                    j--
                }

                if (hasNonDigitWord && runTokens.isNotEmpty()) {
                    val parsedVal = parseEnglishNumberRun(runTokens)
                    if (parsedVal != null) {
                        val startIndex = wordStarts[i]
                        val endIndex = wordStarts[j - 1] + words[j - 1].length
                        val span = normalized.substring(startIndex, endIndex)
                        val overlaps = results.any { it.startIndex < endIndex && it.endIndex > startIndex }
                        if (!overlaps) {
                            results.add(
                                ExtractedNumber(
                                    value = parsedVal,
                                    isInteger = parsedVal % 1.0 == 0.0,
                                    originalSpan = span,
                                    startIndex = startIndex,
                                    endIndex = endIndex
                                )
                            )
                            i = j
                            continue
                        }
                    }
                }
            }
            i++
        }

        // 3. Match standalone digits and suffix numbers (e.g., 2k, 120, 150.5, 5000000)
        val digitSuffixRegex = Regex("""\b(\d+(?:\.\d+)?)\s*([kKmMbB]|million|thousand|billion)?\b""")
        for (match in digitSuffixRegex.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            val overlaps = results.any { it.startIndex < end && it.endIndex > start }
            if (overlaps) continue

            val baseVal = match.groupValues[1].toDoubleOrNull() ?: continue
            val suffix = match.groupValues[2].lowercase()
            val mult = when (suffix) {
                "k", "thousand" -> 1_000.0
                "m", "million" -> 1_000_000.0
                "b", "billion" -> 1_000_000_000.0
                else -> 1.0
            }
            val total = baseVal * mult
            results.add(
                ExtractedNumber(
                    value = total,
                    isInteger = total % 1.0 == 0.0,
                    originalSpan = match.value,
                    startIndex = start,
                    endIndex = end
                )
            )
        }

        return results.sortedBy { it.startIndex }
    }

    private fun isPersianNumberToken(token: String): Boolean {
        return token.toDoubleOrNull() != null ||
                PERSIAN_UNITS.containsKey(token) ||
                PERSIAN_TEENS.containsKey(token) ||
                PERSIAN_TENS.containsKey(token) ||
                PERSIAN_HUNDREDS.containsKey(token) ||
                PERSIAN_MULTIPLIERS.containsKey(token) ||
                token == "نیم"
    }

    private fun isEnglishNumberToken(token: String): Boolean {
        return token.toDoubleOrNull() != null || ENGLISH_WORDS.containsKey(token) || ENGLISH_MULTIPLIERS.containsKey(token)
    }

    /**
     * Implements Section 5.1 Algorithm for a run of Persian number words.
     */
    fun parsePersianNumberRun(tokens: List<String>): Double? {
        var total = 0.0
        var current = 0.0
        var lastMultiplier: Double? = null
        var foundAny = false

        for (token in tokens) {
            if (token == "و" || token == "تا") continue

            val digitVal = token.toDoubleOrNull()
            if (digitVal != null) {
                current += digitVal
                foundAny = true
                continue
            }

            val unitVal = PERSIAN_UNITS[token] ?: PERSIAN_TEENS[token] ?: PERSIAN_TENS[token] ?: PERSIAN_HUNDREDS[token]
            if (unitVal != null) {
                current += unitVal
                foundAny = true
            } else {
                val mult = PERSIAN_MULTIPLIERS[token]
                if (mult != null) {
                    total += maxOf(current, 1.0) * mult
                    current = 0.0
                    lastMultiplier = mult
                    foundAny = true
                } else if (token == "نیم") {
                    if (lastMultiplier != null) {
                        total += 0.5 * lastMultiplier
                    } else {
                        current += 0.5
                    }
                    foundAny = true
                }
            }
        }

        if (!foundAny) return null
        return total + current
    }

    /**
     * Parses standard English number word runs (e.g., "one hundred twenty", "two thousand five hundred").
     */
    fun parseEnglishNumberRun(tokens: List<String>): Double? {
        var total = 0.0
        var current = 0.0
        var foundAny = false

        for (token in tokens) {
            if (token == "and") continue

            val digitVal = token.toDoubleOrNull()
            if (digitVal != null) {
                current += digitVal
                foundAny = true
                continue
            }

            val valNum = ENGLISH_WORDS[token]
            if (valNum != null) {
                if (token == "hundred") {
                    current = (if (current == 0.0) 1.0 else current) * 100.0
                } else {
                    current += valNum
                }
                foundAny = true
            } else {
                val mult = ENGLISH_MULTIPLIERS[token]
                if (mult != null) {
                    total += (if (current == 0.0) 1.0 else current) * mult
                    current = 0.0
                    foundAny = true
                }
            }
        }

        if (!foundAny) return null
        return total + current
    }
}
