package com.example.poultryapp.assistant.core.entities

import com.example.poultryapp.assistant.core.normalize.TextNormalizer

data class ResolvedFlock(
    val flockId: Long,
    val flockName: String,
    val originalSpan: String,
    val startIndex: Int,
    val endIndex: Int
)

data class ResolvedCategory(
    val categoryKey: String,
    val categoryTitleFa: String,
    val isExpense: Boolean,
    val originalSpan: String,
    val startIndex: Int,
    val endIndex: Int
)

object EntityResolver {

    private val CATEGORY_SYNONYMS = listOf(
        // FEED: دان، خوراک، کنسانتره، ذرت، سویا
        Triple("FEED", "خوراک و دان", listOf("دان", "دانه", "خوراک", "کنسانتره", "ذرت", "سویا", "feed", "food")),
        // MEDICINE: دارو، واکسن، مکمل، آنتی بیوتیک
        Triple("MEDICATION", "دارو و واکسن", listOf("دارو", "واکسن", "مکمل", "ویتامین", "آنتی بیوتیک", "medicine", "vaccine", "drug")),
        // UTILITIES: برق، آب، گاز، سوخت، گازوئیل
        Triple("UTILITIES", "قبوض و انشعابات", listOf("قبض آب", "آب و برق", "پول آب", "هزینه آب", "برق", "گاز", "سوخت", "گازوئیل", "utilities", "electricity", "gas")),
        // LABOR: حقوق، دستمزد، کارگر
        Triple("LABOR", "حقوق و دستمزد", listOf("حقوق", "دستمزد", "کارگر", "مساعده", "salary", "wages", "labor")),
        // EGG_SALES: فروش تخم مرغ
        Triple("EGG_SALES", "فروش تخم‌مرغ", listOf("فروش تخم مرغ", "فروش تخممرغ", "فروش تخم", "فروش شانه", "egg sales", "sold eggs")),
        // CHICKEN_SALES: فروش مرغ
        Triple("CHICKEN_SALES", "فروش پرنده", listOf("فروش مرغ", "فروش پرنده", "فروش جوجه", "chicken sales", "sold birds"))
    )

    private val GENERIC_FLOCK_TOKENS = setOf(
        "سالن", "گله", "فارم", "واحد", "بند", "مرغداری",
        "flock", "hall", "house", "shed", "barn", "coop"
    )

    private val EXPLICIT_ALL_FLOCKS_PHRASES = listOf(
        "همه گله ها", "همه ی گله ها", "کل گله ها", "تمام گله ها",
        "همه سالن ها", "همه ی سالن ها", "کل سالن ها", "تمام سالن ها",
        "همه گلهها", "کل گلهها", "همه سالنها", "کل سالنها",
        "all flocks", "all halls", "all houses", "all sheds"
    )

    private val BARE_ALL_TOKENS = setOf(
        "همه", "همشون", "همهشون", "همش", "کلشون", "تمام", "all"
    )

    fun isAllFlocksPhrase(text: String, allowBareAllKeyword: Boolean = false): Boolean {
        val norm = TextNormalizer.normalize(text)
        val matchKey = norm.matchKey
        val spaceNorm = norm.normalized.lowercase().replace("\u200C", " ").replace(Regex("""\s+"""), " ").trim()
        if (EXPLICIT_ALL_FLOCKS_PHRASES.any { phrase ->
                matchKey.contains(phrase) || spaceNorm.contains(phrase)
            }
        ) {
            return true
        }
        if (allowBareAllKeyword && norm.tokens.any { it in BARE_ALL_TOKENS }) {
            return true
        }
        return false
    }

    fun resolveFlock(
        text: String,
        availableFlocks: List<Pair<Long, String>> // List of id to name
    ): ResolvedFlock? {
        return resolveAllFlocks(text, availableFlocks, allowBareAllKeyword = false).firstOrNull()
    }

    fun resolveAllFlocks(
        text: String,
        availableFlocks: List<Pair<Long, String>>,
        allowBareAllKeyword: Boolean = false
    ): List<ResolvedFlock> {
        if (text.isBlank() || availableFlocks.isEmpty()) return emptyList()

        if (isAllFlocksPhrase(text, allowBareAllKeyword = allowBareAllKeyword)) {
            val allPattern = if (allowBareAllKeyword) {
                "(?i)(?:همه(?:[\\s\\u200C]+ی)?|کل|تمام)[\\s\\u200C]*(?:گله[\\s\\u200C]*ها|سالن[\\s\\u200C]*ها)|all[\\s\\u200C]+(?:flocks|halls|houses|sheds)|همه|همشون|همهشون|همش|کلشون|all"
            } else {
                "(?i)(?:همه(?:[\\s\\u200C]+ی)?|کل|تمام)[\\s\\u200C]*(?:گله[\\s\\u200C]*ها|سالن[\\s\\u200C]*ها)|all[\\s\\u200C]+(?:flocks|halls|houses|sheds)"
            }
            val allPhraseMatch = Regex(allPattern).find(text)
            val span = allPhraseMatch?.value ?: ""
            val start = allPhraseMatch?.range?.first ?: 0
            val end = allPhraseMatch?.range?.last?.plus(1) ?: 0
            return availableFlocks.map { (id, name) ->
                ResolvedFlock(
                    flockId = id,
                    flockName = name,
                    originalSpan = span,
                    startIndex = start,
                    endIndex = end
                )
            }
        }

        val matches = mutableListOf<ResolvedFlock>()
        val usedRanges = mutableListOf<IntRange>()

        fun overlapsExisting(range: IntRange): Boolean {
            return usedRanges.any { existing -> range.first <= existing.last && range.last >= existing.first }
        }

        // Normalize lookup map for shorthand suffix resolution like "سالن 1 و 2"
        val normalizedFlockLookup = availableFlocks.associate { (id, name) ->
            normalizeFlockLookupKey(name) to (id to name)
        }

        // Sort longest flock names first so "سالن 10" is checked before "سالن 1"
        val sortedFlocks = availableFlocks.sortedByDescending {
            TextNormalizer.normalize(it.second).normalized.length
        }

        for ((id, name) in sortedFlocks) {
            val regex = buildFlexibleFlockRegex(name) ?: continue
            for (m in regex.findAll(text)) {
                val range = m.range
                if (overlapsExisting(range)) continue
                usedRanges.add(range)
                matches.add(
                    ResolvedFlock(
                        flockId = id,
                        flockName = name,
                        originalSpan = m.value,
                        startIndex = range.first,
                        endIndex = range.last + 1
                    )
                )

                // Handle shorthand conjunctions such as "سالن ۱ و ۲" or "سالن 1، 2 و 3"
                val normFlockTokens = TextNormalizer.normalize(name).tokens
                if (normFlockTokens.size == 2 &&
                    normFlockTokens[0] in GENERIC_FLOCK_TOKENS &&
                    normFlockTokens[1].all { it.isDigit() }
                ) {
                    val prefix = normFlockTokens[0]
                    var cursor = range.last + 1
                    val conjRegex = Regex("""^\s*(?:و|،|,|and)\s*([0-9۰-۹٠-٩]+)(?![\p{L}\p{Nd}])""", RegexOption.IGNORE_CASE)
                    while (cursor < text.length) {
                        val subMatch = conjRegex.find(text.substring(cursor)) ?: break
                        val numGroup = subMatch.groups[1] ?: break
                        val asciiNum = toAsciiDigits(numGroup.value)
                        val candidate = normalizedFlockLookup["$prefix $asciiNum"]
                        val absStart = cursor + numGroup.range.first
                        val absEnd = cursor + numGroup.range.last + 1
                        val absRange = absStart until absEnd
                        if (candidate != null && !overlapsExisting(absRange) && matches.none { it.flockId == candidate.first }) {
                            usedRanges.add(absRange)
                            matches.add(
                                ResolvedFlock(
                                    flockId = candidate.first,
                                    flockName = candidate.second,
                                    originalSpan = numGroup.value,
                                    startIndex = absStart,
                                    endIndex = absEnd
                                )
                            )
                            cursor = absEnd
                        } else {
                            break
                        }
                    }
                }
            }
        }

        if (matches.isNotEmpty()) {
            return matches.distinctBy { it.flockId }.sortedBy { it.startIndex }
        }

        // Fuzzy fallback for non-numeric descriptive flock names (ignoring generic words and pure numbers)
        val normInput = TextNormalizer.normalize(text)
        val ignoredPlaceholders = setOf("date", "num", "money", "category", "flock")
        for (token in normInput.tokens) {
            if (token in GENERIC_FLOCK_TOKENS || token in ignoredPlaceholders) continue
            if (token.length < 2 || token.all { it.isDigit() }) continue
            for ((id, name) in availableFlocks) {
                val normNameTokens = TextNormalizer.normalize(name).tokens.filter {
                    it !in GENERIC_FLOCK_TOKENS && it.length >= 2 && !it.all { ch -> ch.isDigit() }
                }
                for (nameToken in normNameTokens) {
                    if (TextNormalizer.isFuzzyMatch(token, nameToken)) {
                        val start = text.indexOf(token, ignoreCase = true).takeIf { it >= 0 } ?: 0
                        return listOf(
                            ResolvedFlock(
                                flockId = id,
                                flockName = name,
                                originalSpan = token,
                                startIndex = start,
                                endIndex = start + token.length
                            )
                        )
                    }
                }
            }
        }
        return emptyList()
    }

    private fun normalizeFlockLookupKey(name: String): String {
        return TextNormalizer.normalize(name).tokens.joinToString(" ")
    }

    private fun toAsciiDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(
                when (ch) {
                    in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
                    in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    private fun buildFlexibleFlockRegex(flockName: String): Regex? {
        val tokens = TextNormalizer.normalize(flockName).tokens
        if (tokens.isEmpty()) return null
        val tokenPatterns = tokens.map { tok ->
            val sb = StringBuilder()
            for (ch in tok) {
                when {
                    ch in '0'..'9' -> {
                        val offset = ch.code - '0'.code
                        val persian = ('۰'.code + offset).toChar()
                        val arabic = ('٠'.code + offset).toChar()
                        sb.append('[').append(ch).append(persian).append(arabic).append(']')
                    }
                    ch == 'ی' -> sb.append("[یي]")
                    ch == 'ک' -> sb.append("[کك]")
                    ch == 'ه' -> sb.append("[هة]")
                    else -> sb.append(Regex.escape(ch.toString()))
                }
            }
            sb.toString()
        }
        val joined = tokenPatterns.joinToString("""[\s\u200C]+""")
        return Regex("""(?<![\p{L}\p{Nd}])$joined(?![\p{L}\p{Nd}])""", RegexOption.IGNORE_CASE)
    }

    fun resolveFlocksForTransfer(
        text: String,
        availableFlocks: List<Pair<Long, String>>
    ): Pair<ResolvedFlock?, ResolvedFlock?> {
        val norm = TextNormalizer.normalize(text).normalized
        // Check Persian pattern: از <flock1> به <flock2>
        val azIndex = norm.indexOf("از ")
        val beIndex = norm.indexOf(" به ")
        if (azIndex in 0 until beIndex) {
            val fromText = norm.substring(azIndex + 3, beIndex).trim()
            val toText = norm.substring(beIndex + 4).trim()
            val fromFlock = resolveFlock(fromText, availableFlocks)
            val toFlock = resolveFlock(toText, availableFlocks)
            return fromFlock to toFlock
        }

        // Check English pattern: from <flock1> to <flock2>
        val fromIndex = norm.indexOf("from ")
        val toIndex = norm.indexOf(" to ")
        if (fromIndex in 0 until toIndex) {
            val fromText = norm.substring(fromIndex + 5, toIndex).trim()
            val toText = norm.substring(toIndex + 4).trim()
            val fromFlock = resolveFlock(fromText, availableFlocks)
            val toFlock = resolveFlock(toText, availableFlocks)
            return fromFlock to toFlock
        }

        // Fallback: check all flock matches in order of appearance
        val matches = mutableListOf<ResolvedFlock>()
        for ((id, name) in availableFlocks) {
            val normName = TextNormalizer.normalize(name).matchKey
            val idx = norm.indexOf(normName)
            if (idx >= 0) {
                matches.add(ResolvedFlock(id, name, name, idx, idx + name.length))
            }
        }
        matches.sortBy { it.startIndex }
        val first = matches.getOrNull(0)
        val second = matches.getOrNull(1)
        return first to second
    }

    fun resolveCategory(text: String): ResolvedCategory? {
        val normInput = TextNormalizer.normalize(text)
        val matchKey = normInput.matchKey

        if (matchKey.contains("شارژ") || matchKey.contains("refill") ||
            matchKey.contains("کشتار") || matchKey.contains("ذبح") ||
            matchKey.contains("سربرید") || matchKey.contains("slaughter")
        ) {
            return null
        }

        for ((key, titleFa, synonyms) in CATEGORY_SYNONYMS) {
            for (syn in synonyms) {
                val normSyn = TextNormalizer.normalize(syn).matchKey
                if (normSyn.isNotBlank() && matchKey.contains(normSyn)) {
                    val start = text.indexOf(syn).takeIf { it >= 0 } ?: 0
                    val isExp = key != "EGG_SALES" && key != "CHICKEN_SALES"
                    return ResolvedCategory(
                        categoryKey = key,
                        categoryTitleFa = titleFa,
                        isExpense = isExp,
                        originalSpan = syn,
                        startIndex = start,
                        endIndex = start + syn.length
                    )
                }
            }
        }
        return null
    }

    fun resolveFeedWaterType(text: String): String? {
        val norm = TextNormalizer.normalize(text).matchKey
        return when {
            norm.contains("دان") || norm.contains("خوراک") || norm.contains("پلت") ||
                    norm.contains("کنسانتره") || norm.contains("سویا") || norm.contains("ذرت") ||
                    norm.contains("feed") || norm.contains("food") -> "FEED"
            norm.contains("آب") || norm.contains("اب") || norm.contains("لیتر") || norm.contains("water") -> "WATER"
            else -> null
        }
    }

    fun resolveMortalityReason(text: String): String {
        val norm = TextNormalizer.normalize(text).matchKey
        return when {
            norm.contains("گرما") || norm.contains("حرارت") || norm.contains("خفگی") ||
                    norm.contains("قطعی فن") || norm.contains("سرما") || norm.contains("heat") || norm.contains("cold") -> "HEAT_COLD_STRESS"
            norm.contains("کانیبالیسم") || norm.contains("خودخوری") || norm.contains("نوک زدن") || norm.contains("cannibalism") -> "CANNIBALISM"
            norm.contains("شکارچی") || norm.contains("حادثه") || norm.contains("افتادن") || norm.contains("accident") || norm.contains("predator") -> "PREDATOR_ACCIDENT"
            norm.contains("حذف") || norm.contains("ضعیف") || norm.contains("cull") -> "CULLING"
            else -> "DISEASE"
        }
    }

    fun resolveWarehouseItem(text: String, items: List<Pair<Long, String>>): Pair<Long, String>? {
        val normInput = TextNormalizer.normalize(text).matchKey
        for ((id, name) in items) {
            val normName = TextNormalizer.normalize(name).matchKey
            if (normName.isNotBlank() && normInput.contains(normName)) {
                return id to name
            }
        }
        return null
    }

    fun resolveScreenTarget(text: String): Pair<String, String?>? {
        val norm = TextNormalizer.normalize(text).matchKey
        return when {
            norm.contains("انبار") || norm.contains("warehouse") -> "MANAGEMENT" to "WAREHOUSE"
            norm.contains("مالی") || norm.contains("هزینه") || norm.contains("درامد") || norm.contains("finance") -> "MANAGEMENT" to "FINANCIAL"
            norm.contains("جیره") || norm.contains("ration") -> "MANAGEMENT" to "RATION_BUILDER"
            norm.contains("مخاطب") || norm.contains("تلفن") || norm.contains("contacts") -> "MANAGEMENT" to "CONTACTS"
            norm.contains("زیستی") || norm.contains("بیوسکوریتی") || norm.contains("biosecurity") -> "HEALTH" to "BIOSECURITY"
            norm.contains("شبیه ساز") || norm.contains("امکان سنجی") || norm.contains("simulator") -> "MANAGEMENT" to "SIMULATORS"
            norm.contains("بیماری") || norm.contains("تشخیص") || norm.contains("درمان") || norm.contains("diagnosis") -> "HEALTH" to "DIAGNOSIS"
            norm.contains("دارو") || norm.contains("واکسن") || norm.contains("medication") -> "HEALTH" to "DRUGS"
            norm.contains("مکمل") || norm.contains("supplement") -> "HEALTH" to "SUPPLEMENTS"
            norm.contains("دامپزشک") || norm.contains("مشاوره") || norm.contains("vet") -> "HEALTH" to "VET_CONSULT"
            norm.contains("سلامت") || norm.contains("بهداشت") || norm.contains("health") -> "HEALTH" to "DIAGNOSIS"
            norm.contains("شارژ") || norm.contains("دان و آب") || norm.contains("خوراک و اب") || norm.contains("feed") -> "FLOCKS" to "FEED_WATER"
            norm.contains("تقویم") || norm.contains("یادآور") || norm.contains("برنامه") || norm.contains("calendar") -> "FLOCKS" to "CALENDAR"
            norm.contains("فروش و کشتار") || norm.contains("کشتار") || norm.contains("فروش پرنده") || norm.contains("فروش مرغ") || norm.contains("slaughter") -> "FLOCKS" to "SALES_SLAUGHTER"
            norm.contains("تلفات") || norm.contains("mortality") -> "FLOCKS" to "MORTALITY"
            norm.contains("تخم") || norm.contains("تولید") || norm.contains("egg") -> "FLOCKS" to "EGG_PRODUCTION"
            norm.contains("سالن") || norm.contains("گله") || norm.contains("flock") -> "FLOCKS" to "OVERVIEW"
            norm.contains("خانه") || norm.contains("صفحه اصلی") || norm.contains("داشبورد") || norm.contains("home") -> "HOME" to null
            norm.contains("تنظیمات") || norm.contains("settings") -> "SETTINGS" to null
            norm.contains("راهنما") || norm.contains("آموزش") || norm.contains("guide") -> "BREEDING_GUIDE" to null
            norm.contains("بازی") || norm.contains("مسابقه") || norm.contains("game") -> "GOLDEN_BREEDING" to null
            else -> null
        }
    }

    private val COMMON_MEDICATIONS = listOf(
        "اکسی تتراسایکلین", "انروفلوکساسین", "آمپی سیلین", "آمپرولیوم", "تیلوزین", "تایلوزین",
        "سولفادیمیدین", "کولیستین", "دیکلازوریل", "مولتی ویتامین", "ویتامین ad3e", "ویتامین c",
        "الکترولیت", "لوامیزول", "ایورمکتین", "فلورفنیکل", "داکسی سایکلین",
        "oxytetracycline", "enrofloxacin", "tylosin", "amoxicillin", "doxycycline", "colistin"
    )

    fun resolveMedication(text: String): String? {
        val norm = TextNormalizer.normalize(text).matchKey
        for (med in COMMON_MEDICATIONS) {
            val normMed = TextNormalizer.normalize(med).matchKey
            if (norm.contains(normMed)) return med
        }
        return null
    }

    private val COMMON_SYMPTOMS = listOf(
        "اسهال سفید", "اسهال سبز", "اسهال خونی", "اسهال آبکی", "اسهال",
        "آبریزش بینی", "عطسه", "سرفه", "خس خس", "تنگی نفس", "تنفس با دهان باز",
        "تورم سر", "پف کردن چشم", "کف در چشم", "تورم صورت", "کاهش مصرف دان",
        "افت شدید تخم", "افت تخم", "پوسته نرم", "فلجی پای مرغ", "پیچش گردن", "لرزش سر",
        "بی حالی", "کسالت", "ژولیدگی پر", "خواب آلودگی", "لاغری", "کبودی تاج", "رنگ پریدگی تاج"
    )

    fun resolveSymptoms(text: String): List<String> {
        val norm = TextNormalizer.normalize(text).matchKey
        val found = mutableListOf<String>()
        for (sym in COMMON_SYMPTOMS) {
            val normSym = TextNormalizer.normalize(sym).matchKey
            if (norm.contains(normSym)) {
                found.add(sym)
            }
        }
        return found
    }
}

