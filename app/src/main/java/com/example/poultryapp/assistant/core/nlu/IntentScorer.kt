package com.example.poultryapp.assistant.core.nlu

import com.example.poultryapp.assistant.core.normalize.Lang
import com.example.poultryapp.assistant.core.normalize.TextNormalizer

object IntentScorer {

    private val TRIGGER_LEXICON: Map<IntentId, List<Pair<String, Float>>> = mapOf(
        IntentId.ADD_EGGS to listOf(
            "تخم" to 0.5f, "تخممرغ" to 0.5f, "تخم مرغ" to 0.5f, "شانه" to 0.6f, "شونه" to 0.6f, "کارتن" to 0.6f,
            "egg" to 0.5f, "eggs" to 0.5f, "tray" to 0.6f, "trays" to 0.6f,
            "جمع کردم" to 0.5f, "جمع شد" to 0.5f, "ثبت کن" to 0.4f, "ثبت" to 0.4f, "اضافه کن" to 0.4f, "اضافه" to 0.4f,
            "بنویس" to 0.3f, "وارد کن" to 0.3f,
            "add" to 0.4f, "log" to 0.4f, "record" to 0.4f, "collected" to 0.5f, "gathered" to 0.5f
        ),
        IntentId.ADD_TRANSACTION to listOf(
            "هزینه" to 0.5f, "خرج" to 0.5f, "خریدم" to 0.5f, "خرید" to 0.4f, "پرداخت" to 0.4f, "دادم" to 0.3f,
            "درآمد" to 0.5f, "فروختم" to 0.5f, "فروش" to 0.4f, "واریز" to 0.4f, "ثبت" to 0.3f,
            "تومان" to 0.3f, "ریال" to 0.3f, "دان خریدم" to 0.5f, "دارو خریدم" to 0.5f,
            "expense" to 0.5f, "spent" to 0.5f, "bought" to 0.5f, "cost" to 0.4f, "paid" to 0.4f,
            "income" to 0.5f, "sold" to 0.5f, "revenue" to 0.4f, "earned" to 0.4f
        ),
        IntentId.QUERY_EGGS to listOf(
            "چند تا تخم" to 0.6f, "چقدر تخم" to 0.6f, "آمار تخم" to 0.5f, "تخم امروز" to 0.5f,
            "how many eggs" to 0.6f, "eggs today" to 0.5f, "eggs this week" to 0.5f
        ),
        IntentId.QUERY_FINANCE to listOf(
            "چقدر خرج" to 0.6f, "چقدر هزینه" to 0.6f, "سود" to 0.5f, "تراز مالی" to 0.6f, "مخارج" to 0.5f,
            "how much did i spend" to 0.6f, "profit this week" to 0.6f, "expenses this month" to 0.6f
        ),
        IntentId.ADD_FEED_WATER to listOf(
            "شارژ دان" to 0.7f, "شارژ آب" to 0.7f, "شارژ" to 0.5f, "دان دادم" to 0.6f, "آب دادم" to 0.6f,
            "ریختم" to 0.4f, "پر کردم" to 0.4f, "refill" to 0.7f, "feed" to 0.4f, "water" to 0.4f
        ),
        IntentId.QUERY_FEED_WATER to listOf(
            "مصرف دان" to 0.8f, "مصرف آب" to 0.8f, "مصرف" to 0.7f, "چقدر دان" to 0.8f, "چقدر آب" to 0.8f,
            "کی تموم میشه" to 0.8f, "ماندگاری دان" to 0.8f, "ماندگاری آب" to 0.8f,
            "feed remaining" to 0.8f, "water remaining" to 0.8f, "feed consumed" to 0.8f
        ),
        IntentId.LOG_MORTALITY to listOf(
            "تلفات" to 0.7f, "مرد" to 0.5f, "مرده" to 0.5f, "تلف شد" to 0.6f, "کشته شد" to 0.5f,
            "mortality" to 0.7f, "dead birds" to 0.7f, "died" to 0.5f
        ),
        IntentId.SELL_BIRDS to listOf(
            "فروش مرغ" to 0.85f, "فروش پرنده" to 0.85f, "فروش جوجه" to 0.85f, "فروختم" to 0.55f,
            "فروش" to 0.45f, "مرغ" to 0.35f, "پرنده" to 0.35f, "جوجه" to 0.35f, "بوقلمون" to 0.35f, "بلدرچین" to 0.35f, "قطعه" to 0.25f,
            "sell birds" to 0.85f, "sold birds" to 0.85f, "sold chickens" to 0.85f, "sell chickens" to 0.85f,
            "sold" to 0.4f, "sell" to 0.4f, "birds" to 0.3f, "chickens" to 0.3f
        ),
        IntentId.SLAUGHTER_BIRDS to listOf(
            "کشتار" to 0.9f, "کشتارگاه" to 0.85f, "کشتار پرنده" to 0.9f, "کشتار مرغ" to 0.9f,
            "ذبح" to 0.9f, "سربریدم" to 0.9f, "سربریدن" to 0.9f, "سر بریدم" to 0.9f, "سر بریدن" to 0.9f,
            "وزن لاشه" to 0.6f, "لاشه" to 0.5f,
            "slaughter" to 0.9f, "slaughtered" to 0.9f, "butcher" to 0.85f, "carcass" to 0.6f
        ),
        IntentId.QUERY_MORTALITY to listOf(
            "تلفات این هفته" to 0.8f, "تلفات امروز" to 0.8f, "آمار تلفات" to 0.8f, "میزان تلفات" to 0.8f,
            "چند تا تلفات" to 0.8f, "تلفات سالن" to 0.7f, "تلفات" to 0.6f, "mortality rate" to 0.7f
        ),
        IntentId.QUERY_FLOCKS to listOf(
            "چند تا مرغ داریم" to 0.8f, "چند تا مرغ" to 0.8f, "تعداد مرغ" to 0.8f, "آمار سالن" to 0.6f, "سن گله" to 0.7f,
            "چند تا سالن داریم" to 0.7f, "وضعیت سالن" to 0.6f, "وضعیت گله" to 0.6f, "flock age" to 0.7f, "bird count" to 0.7f
        ),
        IntentId.TRANSFER_BIRDS to listOf(
            "انتقال" to 0.8f, "انتقال مرغ" to 0.8f, "انتقال پرنده" to 0.8f, "انتقال از سالن" to 0.8f, "انتقال به سالن" to 0.8f,
            "جابجا" to 0.7f, "جابجا کن" to 0.7f, "انتقال بده" to 0.8f, "transfer" to 0.8f, "transfer birds" to 0.8f
        ),
        IntentId.QUERY_WAREHOUSE to listOf(
            "موجودی انبار" to 0.8f, "انبار" to 0.7f, "موجودی" to 0.7f, "چقدر دان داریم" to 0.7f, "چقدر دان تو انباره" to 0.7f, "کسری انبار" to 0.7f,
            "موجودی دان" to 0.7f, "اقلام انبار" to 0.6f, "warehouse stock" to 0.8f, "stock" to 0.7f
        ),
        IntentId.CONSUME_WAREHOUSE to listOf(
            "مصرف" to 0.7f, "برداشت" to 0.7f, "مصرف از انبار" to 0.8f, "از انبار برداشتم" to 0.8f, "برداشت از انبار" to 0.8f,
            "consume" to 0.7f, "consume stock" to 0.8f
        ),
        IntentId.DIAGNOSE_DISEASE to listOf(
            "چه بیماریه" to 0.8f, "تشخیص بیماری" to 0.8f, "علائم بیماری" to 0.7f, "بیماری چیه" to 0.7f,
            "مرغ ها عطسه میکنن" to 0.7f, "اسهال دارن" to 0.7f, "diagnose" to 0.8f, "what disease" to 0.8f
        ),
        IntentId.QUERY_MEDICATION to listOf(
            "دوز دارو" to 0.8f, "دوز" to 0.6f, "طریقه مصرف" to 0.7f, "دوره پرهیز" to 0.8f, "پرهیز دارویی" to 0.8f,
            "dosage" to 0.8f, "withdrawal period" to 0.8f
        ),
        IntentId.ADD_SCHEDULE to listOf(
            "یادآور تنظیم کن" to 0.8f, "تنظیم یادآور" to 0.8f, "یادآور" to 0.7f, "برنامه ریزی کن" to 0.7f, "واکسن بزنم" to 0.6f,
            "remind me" to 0.8f, "schedule task" to 0.8f
        ),
        IntentId.QUERY_SCHEDULE to listOf(
            "برنامه امروز" to 0.8f, "برنامه های امروز" to 0.8f, "برنامه‌های امروز" to 0.8f, "برنامه ها" to 0.7f, "برنامه" to 0.7f,
            "واکسن های امروز" to 0.8f, "یادآورهای امروز" to 0.8f, "today tasks" to 0.8f, "today schedule" to 0.8f
        ),
        IntentId.QUERY_CONTACTS to listOf(
            "شماره تلفن" to 0.8f, "شماره" to 0.6f, "تلفن" to 0.6f, "شماره دامپزشک" to 0.8f, "تلفن کارخانه" to 0.8f,
            "مخاطبین" to 0.7f, "phone number" to 0.8f, "contact" to 0.7f
        ),
        IntentId.QUERY_WEATHER to listOf(
            "دمای فارم" to 0.8f, "دمای سالن" to 0.8f, "هوای فارم" to 0.7f, "وضعیت هوا" to 0.7f, "تهویه سالن" to 0.7f,
            "دما" to 0.7f, "تهویه" to 0.7f, "هوا" to 0.6f, "weather" to 0.8f, "temperature" to 0.7f
        ),
        IntentId.UNDO_LAST to listOf(
            "برگردون" to 0.8f, "برگردان" to 0.8f, "اشتباه شد" to 0.8f, "پاکش کن" to 0.7f, "لغوش کن" to 0.7f,
            "undo" to 0.8f, "undo that" to 0.8f, "revert" to 0.7f
        ),
        IntentId.OPEN_SCREEN to listOf(
            "برو به" to 0.8f, "باز کن" to 0.7f, "صفحه" to 0.5f,
            "open" to 0.8f, "go to" to 0.7f, "navigate to" to 0.8f
        ),
        IntentId.HELP to listOf(
            "راهنما" to 0.8f, "کمک" to 0.7f, "چه کارهایی" to 0.7f, "دستورات" to 0.7f,
            "help" to 0.8f, "what can you do" to 0.8f, "commands" to 0.7f
        ),
        IntentId.GREETING to listOf(
            "سلام" to 0.8f, "درود" to 0.8f, "صبح بخیر" to 0.7f, "عصر بخیر" to 0.7f,
            "hi" to 0.8f, "hello" to 0.8f, "hey" to 0.7f
        )
    )

    private val NEGATIVE_QUESTION_TRIGGERS = listOf(
        "چرا", "علت", "دلیل", "درمان", "چطور", "چگونه", "چه دارویی", "چه کار کنم", "راهکار", "چیست",
        "why", "how do i", "what should", "how to treat", "cure"
    )

    private val PHRASE_BANKS: Map<IntentId, List<String>> = mapOf(
        IntentId.ADD_EGGS to listOf(
            "add <NUM> eggs", "collected <NUM> eggs today", "log <NUM> eggs yesterday",
            "<NUM> trays today", "<NUM> trays <DATE>", "<NUM> trays",
            "امروز <NUM> تا تخم مرغ اضافه کن", "<NUM> تا تخم جمع کردم",
            "دیروز <NUM> تخم مرغ ثبت کن", "<NUM> شانه امروز", "<NUM> شانه <DATE>", "<NUM> شانه",
            "<NUM> <DATE>", "ثبت تخم مرغ <NUM> عدد", "ثبت تخم مرغ", "اضافه کردن تخم مرغ", "add eggs"
        ),
        IntentId.ADD_TRANSACTION to listOf(
            "add expense <MONEY> for feed", "spent <MONEY> on medicine", "sold eggs for <MONEY>",
            "income <MONEY> egg sales", "هزینه دان <MONEY> ثبت کن", "<MONEY> دارو خریدم",
            "<MONEY> تخم مرغ فروختم", "خرج کارگر <MONEY>", "ثبت هزینه <MONEY>",
            "<CATEGORY> <MONEY>", "<CATEGORY> <MONEY> ثبت کن",
            "ثبت هزینه", "ثبت خرج", "add expense", "ثبت درآمد"
        ),
        IntentId.ADD_FEED_WATER to listOf(
            "شارژ دان <NUM> کیلو برای سالن", "شارژ دان <NUM> برای سالن", "شارژ دان <NUM> کیلو", "شارژ دان <NUM>",
            "شارژ آب <NUM> لیتر", "شارژ آب <NUM>", "add <NUM> kg feed to flock", "refill <NUM> liters water", "<NUM> kg feed", "<NUM> لیتر آب"
        ),
        IntentId.QUERY_FEED_WATER to listOf(
            "امروز چقدر دان مصرف شده", "<DATE> چقدر دان مصرف شده", "چقدر دان مصرف شده",
            "<DATE> چقدر <CATEGORY> مصرف شده", "چقدر <CATEGORY> مصرف شده",
            "امروز چقدر آب مصرف شده", "<DATE> چقدر آب مصرف شده", "چقدر آب مصرف شده",
            "آب سالن کی تموم میشه", "ماندگاری دان چقدره",
            "how much feed consumed today", "when does water run out"
        ),
        IntentId.LOG_MORTALITY to listOf(
            "تلفات <NUM> قطعه در سالن", "تلفات <NUM> تا به علت گرمازدگی", "تلفات <NUM> قطعه در سالن به علت گرمازدگی",
            "امروز <NUM> تلفات داشتیم", "<NUM> dead birds in flock", "log <NUM> mortality"
        ),
        IntentId.SELL_BIRDS to listOf(
            "فروش <NUM> مرغ", "فروش <NUM> قطعه مرغ", "فروش <NUM> مرغ <MONEY>", "<NUM> مرغ فروختم",
            "<NUM> مرغ فروختم <MONEY>", "فروش <NUM> مرغ وزن زنده <WEIGHT> <MONEY>",
            "<NUM> قطعه مرغ فروختم <MONEY>", "فروش پرنده", "فروش مرغ",
            "sell <NUM> birds", "sold <NUM> birds for <MONEY>", "sold <NUM> chickens"
        ),
        IntentId.SLAUGHTER_BIRDS to listOf(
            "کشتار <NUM> مرغ", "کشتار <NUM> قطعه مرغ", "ذبح <NUM> پرنده", "<NUM> مرغ سربریدم",
            "کشتار <NUM> مرغ وزن زنده <WEIGHT> وزن لاشه <WEIGHT>",
            "<NUM> مرغ کشتارگاه فرستادم وزن زنده <WEIGHT> وزن لاشه <WEIGHT>",
            "کشتار پرنده", "کشتار مرغ",
            "slaughter <NUM> birds", "slaughter <NUM> birds weight <WEIGHT> carcass <WEIGHT>"
        ),
        IntentId.QUERY_MORTALITY to listOf(
            "تلفات این هفته چقدر بوده", "تلفات <DATE> چقدر بوده", "آمار تلفات سالن", "امروز چند تا تلفات داشتیم",
            "how many dead birds this week", "mortality rate"
        ),
        IntentId.QUERY_FLOCKS to listOf(
            "چند تا مرغ داریم", "تعداد کل پرنده‌ها", "سن گله سالن یک چقدره", "دوره پرهیز سالن",
            "how many birds in total", "flock age", "active flocks"
        ),
        IntentId.TRANSFER_BIRDS to listOf(
            "انتقال <NUM> مرغ از سالن به سالن", "انتقال <NUM> از سالن به سالن", "انتقال <NUM> تا مرغ", "انتقال <NUM> تا پرنده",
            "transfer <NUM> birds to flock"
        ),
        IntentId.QUERY_WAREHOUSE to listOf(
            "موجودی انبار چقدره", "چقدر دان تو انباره", "اقلام رو به اتمام انبار", "کسری انبار",
            "warehouse stock", "low stock items"
        ),
        IntentId.CONSUME_WAREHOUSE to listOf(
            "مصرف <NUM> کیلو از انبار برای سالن", "مصرف <NUM> از انبار برای سالن", "برداشت از انبار <NUM> کیلو", "برداشت <NUM> از انبار",
            "consume <NUM> kg from warehouse"
        ),
        IntentId.DIAGNOSE_DISEASE to listOf(
            "مرغ‌ها اسهال سفید و بی حالی دارند چه بیماریه", "علائم نیوکاسل چیه", "درمان اسهال خونی مرغ",
            "chickens have white diarrhea", "symptoms of newcastle disease"
        ),
        IntentId.QUERY_MEDICATION to listOf(
            "دوز داروی اکسی تتراسایکلین چقدره", "طریقه مصرف انروفلوکساسین", "دوره پرهیز گوشت",
            "dosage for oxytetracycline", "withdrawal period for enrofloxacin"
        ),
        IntentId.ADD_SCHEDULE to listOf(
            "یادآور واکسن فردا ساعت ۹ صبح", "تنظیم یادآور برای سالن",
            "remind me vaccine tomorrow at 9 am"
        ),
        IntentId.QUERY_SCHEDULE to listOf(
            "برنامه‌های امروز چیه", "برنامه های امروز چیه", "برنامه‌های <DATE> چیه", "برنامه های <DATE> چیه",
            "برنامه‌های امروز", "برنامه های امروز", "واکسن‌ها و ویتامین‌های این هفته",
            "today schedule", "upcoming vaccines"
        ),
        IntentId.QUERY_CONTACTS to listOf(
            "شماره تلفن دامپزشک چنده", "تلفن کارخانه دان", "شماره تماس خریدار تخم مرغ",
            "phone number of vet", "feed supplier contact"
        ),
        IntentId.QUERY_WEATHER to listOf(
            "دمای هوای فارم چنده", "وضعیت هوای سالن چطوره", "توصیه تهویه امروز",
            "farm temperature", "weather and ventilation"
        ),
        IntentId.QUERY_EGGS to listOf(
            "how many eggs today", "eggs this week", "امروز چند تا تخم داشتیم",
            "تخم مرغ این هفته چقدر بود", "وضعیت تخم مرغ سالن یک"
        ),
        IntentId.QUERY_FINANCE to listOf(
            "how much did i spend this month", "profit this week", "این ماه چقدر خرج کردم",
            "سود این هفته چقدره", "سود <CATEGORY> چقدره", "سود چقدره", "گزارش مالی هفته"
        ),
        IntentId.UNDO_LAST to listOf(
            "undo", "undo that", "revert last", "برگردون", "اشتباه شد", "لغو کن"
        ),
        IntentId.OPEN_SCREEN to listOf(
            "open finance", "open settings", "open eggs", "برو به مالی", "برو به تنظیمات",
            "برو به انبار", "برو به بخش انبار", "برو به سلامت", "برو به گله ها", "برو به سالن ها"
        ),
        IntentId.HELP to listOf(
            "help", "what can you do", "راهنما", "چه کارهایی میتونی بکنی"
        ),
        IntentId.GREETING to listOf(
            "hi", "hello", "hey", "سلام", "درود"
        )
    )

    fun score(
        templateText: String,
        extractedSlots: Map<SlotId, SlotValue>,
        tokens: List<String>,
        lang: Lang,
        config: NluConfig = NluConfig()
    ): NluResult {
        val trace = mutableListOf<String>()
        val normTemplate = templateText.lowercase().replace("\u200C", "")

        // 1. General Question Guard: Check for negative question triggers
        val hasQuestionWord = NEGATIVE_QUESTION_TRIGGERS.any { q ->
            tokens.any { t ->
                val matches = (t == q || TextNormalizer.isFuzzyMatch(t, q))
                if (matches && t == "علت") {
                    !normTemplate.contains("به علت")
                } else {
                    matches
                }
            }
        }
        val hasCommandVerbWithNum = (extractedSlots.containsKey(SlotId.COUNT) || extractedSlots.containsKey(SlotId.AMOUNT)) &&
                (normTemplate.contains("ثبت") || normTemplate.contains("اضافه") || normTemplate.contains("add") ||
                 normTemplate.contains("log") || normTemplate.contains("شارژ") || normTemplate.contains("تلفات") ||
                 normTemplate.contains("انتقال") || normTemplate.contains("مصرف") || normTemplate.contains("برداشت") ||
                 normTemplate.contains("فروختم") || normTemplate.contains("سربرید") || normTemplate.contains("sold"))

        val hasClinicalOrMedTopic = tokens.any {
            it in listOf("اسهال", "سرفه", "عطسه", "آبریزش", "تورم", "پف", "فلجی", "کسالت", "اکسی", "انرو", "واکسن", "تیلوزین", "دارو", "دوز", "پرهیز", "تلفات", "گرمازدگی", "مرگ")
        } || normTemplate.contains("بیماری") || normTemplate.contains("دوز") || normTemplate.contains("پرهیز") || normTemplate.contains("تلفات") || normTemplate.contains("گرمازدگی")

        if (hasQuestionWord && !hasCommandVerbWithNum && !hasClinicalOrMedTopic) {
            trace.add("Guard matched question keyword without strong command verb -> GENERAL_QUESTION")
            return NluResult(
                intent = IntentId.GENERAL_QUESTION,
                confidence = 0.90f,
                margin = 0.50f,
                slots = extractedSlots,
                alternatives = emptyList(),
                lang = lang,
                templateText = templateText,
                trace = trace
            )
        }

        val scores = mutableMapOf<IntentId, Float>()

        val hasEggWord = normTemplate.contains("تخم") || normTemplate.contains("egg") ||
                tokens.any { it in listOf("تخم", "تخممرغ", "شانه", "شونه", "کارتن", "egg", "eggs", "tray", "trays") }
        val hasEggCommandVerb = normTemplate.contains("ثبت") || normTemplate.contains("اضافه") ||
                normTemplate.contains("add") || normTemplate.contains("log") || normTemplate.contains("جمع") ||
                normTemplate.contains("collected") || normTemplate.contains("بنویس") || normTemplate.contains("وارد")

        val hasFinWord = normTemplate.contains("هزینه") || normTemplate.contains("خرج") || normTemplate.contains("درآمد") ||
                normTemplate.contains("دان") || normTemplate.contains("دارو") || normTemplate.contains("expense") ||
                normTemplate.contains("spent") || normTemplate.contains("bought") || normTemplate.contains("income") || normTemplate.contains("sold")
        val hasFinCommandVerb = normTemplate.contains("ثبت") || normTemplate.contains("اضافه") ||
                normTemplate.contains("add") || normTemplate.contains("log") || normTemplate.contains("بنویس") ||
                normTemplate.contains("خرید") || normTemplate.contains("فروش") || normTemplate.contains("paid")

        val hasExplicitCommandVerb = normTemplate.contains("ثبت") || normTemplate.contains("اضافه") ||
                normTemplate.contains("بنویس") || normTemplate.contains("وارد") ||
                normTemplate.contains("add") || normTemplate.contains("log") || normTemplate.contains("record")

        val hasSlaughterCue = normTemplate.contains("کشتار") || normTemplate.contains("ذبح") ||
                normTemplate.contains("سربرید") || normTemplate.contains("سر برید") ||
                normTemplate.contains("slaughter") || normTemplate.contains("butcher") ||
                extractedSlots.containsKey(SlotId.CARCASS_WEIGHT_KG)
        val hasSaleVerb = normTemplate.contains("فروش") || normTemplate.contains("فروختم") ||
                tokens.any { it in listOf("فروش", "فروختم", "sell", "sold", "sale") }
        val hasBirdWord = !hasEggWord && (
                normTemplate.contains("مرغ") || normTemplate.contains("پرنده") ||
                normTemplate.contains("جوجه") || normTemplate.contains("خروس") ||
                normTemplate.contains("بوقلمون") || normTemplate.contains("بلدرچین") ||
                normTemplate.contains("اردک") || normTemplate.contains("غاز") || normTemplate.contains("کبک") ||
                normTemplate.contains("قطعه") || normTemplate.contains("گله") || normTemplate.contains("سالن") ||
                tokens.any { it in listOf("bird", "birds", "chicken", "chickens", "chick", "chicks", "turkey", "turkeys", "quail", "duck", "flock") }
                )
        val isFinanceOnlyQuery = !extractedSlots.containsKey(SlotId.COUNT) &&
                (normTemplate.contains("سود") || normTemplate.contains("تراز") || normTemplate.contains("profit") ||
                 normTemplate.contains("چقدر") || normTemplate.contains("how much") ||
                 (normTemplate.contains("درآمد") && !hasExplicitCommandVerb))
        val isBirdSaleIntentCue = hasSaleVerb &&
                (hasBirdWord || extractedSlots.containsKey(SlotId.COUNT)) &&
                !hasEggWord && !hasSlaughterCue && !isFinanceOnlyQuery &&
                !(normTemplate.contains("درآمد") && !extractedSlots.containsKey(SlotId.COUNT))

        for (intent in IntentId.values()) {
            if (intent == IntentId.UNKNOWN || intent == IntentId.GENERAL_QUESTION) continue

            // S_lex: Lexicon trigger hits capped at 1.0
            val lexicon = TRIGGER_LEXICON[intent] ?: emptyList()
            var sLex = 0.0f
            for ((keyword, weight) in lexicon) {
                val normKey = keyword.lowercase().replace("\u200C", "")
                if (normTemplate.contains(normKey)) {
                    sLex += weight
                } else {
                    for (token in tokens) {
                        if (token == normKey) {
                            sLex += weight
                            break
                        } else if (TextNormalizer.isFuzzyMatch(token, normKey)) {
                            sLex += weight * 0.8f
                            break
                        }
                    }
                }
            }
            sLex = minOf(sLex, 1.0f)

            // S_pat: Soft pattern template matching
            val sPat = when (intent) {
                IntentId.ADD_EGGS -> when {
                    normTemplate.contains("<num>") && hasEggWord -> 1.0f
                    hasEggWord && hasEggCommandVerb -> 0.85f
                    hasEggWord -> 0.5f
                    else -> 0.0f
                }
                IntentId.ADD_TRANSACTION -> when {
                    normTemplate.contains("<money>") || (normTemplate.contains("<num>") && hasFinWord) -> 1.0f
                    hasFinWord && hasFinCommandVerb -> 0.85f
                    hasFinWord -> 0.5f
                    else -> 0.0f
                }
                IntentId.QUERY_EGGS -> if ((normTemplate.contains("چند") || normTemplate.contains("how many") || normTemplate.contains("چقدر")) && (normTemplate.contains("تخم") || normTemplate.contains("egg"))) 1.0f else 0.0f
                IntentId.QUERY_FINANCE -> if (
                    (normTemplate.contains("چقدر") || normTemplate.contains("how much") || normTemplate.contains("تراز") || normTemplate.contains("گزارش مالی") || normTemplate.contains("profit") || normTemplate.contains("سود")) &&
                    (normTemplate.contains("خرج") || normTemplate.contains("spend") || normTemplate.contains("هزینه") || normTemplate.contains("مالی") || normTemplate.contains("سود") || normTemplate.contains("درآمد") || normTemplate.contains("profit") || normTemplate.contains("income"))
                ) 1.0f else 0.0f
                IntentId.ADD_FEED_WATER -> if ((normTemplate.contains("شارژ") || normTemplate.contains("refill") || normTemplate.contains("دادم") || normTemplate.contains("ریختم")) && (normTemplate.contains("<num>") || normTemplate.contains("دان") || normTemplate.contains("آب") || extractedSlots.containsKey(SlotId.FEED_WATER_TYPE))) 1.0f else 0.0f
                IntentId.QUERY_FEED_WATER -> if ((normTemplate.contains("مصرف") || normTemplate.contains("کی تموم") || normTemplate.contains("ماندگاری") || normTemplate.contains("remaining")) && (normTemplate.contains("دان") || normTemplate.contains("آب") || normTemplate.contains("<category>") || extractedSlots.containsKey(SlotId.FEED_WATER_TYPE) || normTemplate.contains("feed") || normTemplate.contains("water") || tokens.any { it in listOf("دان", "خوراک", "آب", "feed", "water") })) 1.0f else 0.0f
                IntentId.LOG_MORTALITY -> if ((normTemplate.contains("تلفات") || normTemplate.contains("مرد") || normTemplate.contains("مرده") || normTemplate.contains("mortality") || normTemplate.contains("dead")) && normTemplate.contains("<num>") && !normTemplate.contains("چند") && !normTemplate.contains("چقدر")) 1.0f else if (normTemplate.contains("تلفات") || normTemplate.contains("mortality")) 0.85f else 0.0f
                IntentId.QUERY_MORTALITY -> if ((normTemplate.contains("چند") || normTemplate.contains("چقدر") || normTemplate.contains("آمار") || normTemplate.contains("how many") || normTemplate.contains("rate")) && (normTemplate.contains("تلفات") || normTemplate.contains("مرده") || normTemplate.contains("mortality"))) 1.0f else 0.0f
                IntentId.SELL_BIRDS -> when {
                    isBirdSaleIntentCue && normTemplate.contains("<num>") -> 1.0f
                    isBirdSaleIntentCue -> 0.85f
                    else -> 0.0f
                }
                IntentId.SLAUGHTER_BIRDS -> when {
                    hasSlaughterCue && normTemplate.contains("<num>") -> 1.0f
                    hasSlaughterCue -> 0.85f
                    else -> 0.0f
                }
                IntentId.QUERY_FLOCKS -> if ((normTemplate.contains("چند") || normTemplate.contains("سن") || normTemplate.contains("وضعیت") || normTemplate.contains("how many") || normTemplate.contains("age") || normTemplate.contains("تعداد")) && (normTemplate.contains("مرغ") || normTemplate.contains("سالن") || normTemplate.contains("گله") || normTemplate.contains("<flock>") || normTemplate.contains("flock") || normTemplate.contains("bird"))) 1.0f else 0.0f
                IntentId.TRANSFER_BIRDS -> if ((normTemplate.contains("انتقال") || normTemplate.contains("جابجا") || normTemplate.contains("transfer")) && (normTemplate.contains("<num>") || normTemplate.contains("مرغ") || normTemplate.contains("پرنده") || normTemplate.contains("سالن") || normTemplate.contains("<flock>") || normTemplate.contains("bird") || normTemplate.contains("flock"))) 1.0f else 0.0f
                IntentId.QUERY_WAREHOUSE -> if ((normTemplate.contains("انبار") || normTemplate.contains("موجودی") || normTemplate.contains("کسری") || normTemplate.contains("warehouse") || normTemplate.contains("stock")) && !normTemplate.contains("مصرف") && !normTemplate.contains("برداشت") && !normTemplate.contains("consume")) 1.0f else 0.0f
                IntentId.CONSUME_WAREHOUSE -> if ((normTemplate.contains("مصرف") || normTemplate.contains("برداشت") || normTemplate.contains("consume")) && (normTemplate.contains("انبار") || normTemplate.contains("warehouse"))) 1.0f else 0.0f
                IntentId.DIAGNOSE_DISEASE -> if (normTemplate.contains("بیماری") || normTemplate.contains("تشخیص") || normTemplate.contains("علائم") || normTemplate.contains("diagnose") || normTemplate.contains("disease") || tokens.any { it in listOf("اسهال", "سرفه", "عطسه", "آبریزش", "تورم", "پف", "فلجی") }) 1.0f else 0.0f
                IntentId.QUERY_MEDICATION -> if (normTemplate.contains("دوز") || normTemplate.contains("پرهیز") || normTemplate.contains("dosage") || normTemplate.contains("withdrawal") || tokens.any { it in listOf("اکسی", "انرو", "تیلوزین", "آمپرولیوم", "سولفادیمیدین", "کولیستین") }) 1.0f else 0.0f
                IntentId.ADD_SCHEDULE -> if ((normTemplate.contains("یادآور") || normTemplate.contains("تنظیم") || normTemplate.contains("remind")) && (normTemplate.contains("ساعت") || normTemplate.contains("فردا") || normTemplate.contains("<date>") || normTemplate.contains("امروز") || normTemplate.contains("واکسن"))) 1.0f else 0.0f
                IntentId.QUERY_SCHEDULE -> if ((normTemplate.contains("برنامه") || normTemplate.contains("یادآور") || normTemplate.contains("schedule") || normTemplate.contains("task")) && (normTemplate.contains("امروز") || normTemplate.contains("<date>") || normTemplate.contains("هفته") || normTemplate.contains("today") || normTemplate.contains("چیه") || normTemplate.contains("چیست"))) 1.0f else 0.0f
                IntentId.QUERY_CONTACTS -> if (normTemplate.contains("شماره") || normTemplate.contains("تلفن") || normTemplate.contains("مخاطب") || normTemplate.contains("phone") || normTemplate.contains("contact")) 1.0f else 0.0f
                IntentId.QUERY_WEATHER -> if (normTemplate.contains("دما") || normTemplate.contains("هوا") || normTemplate.contains("تهویه") || normTemplate.contains("weather") || normTemplate.contains("temperature") || normTemplate.contains("ventilation")) 1.0f else 0.0f
                IntentId.UNDO_LAST -> if (normTemplate.contains("برگردون") || normTemplate.contains("برگردان") || normTemplate.contains("undo") || normTemplate.contains("اشتباه") || normTemplate.contains("پاک") || normTemplate.contains("لغو")) 1.0f else 0.0f
                IntentId.HELP -> if (normTemplate.contains("راهنما") || normTemplate.contains("help") || normTemplate.contains("دستورات") || normTemplate.contains("کمک")) 1.0f else 0.0f
                IntentId.GREETING -> if (normTemplate.contains("سلام") || normTemplate.contains("hi") || normTemplate.contains("hello") || normTemplate.contains("درود")) 1.0f else 0.0f
                IntentId.OPEN_SCREEN -> if (normTemplate.contains("برو به") || normTemplate.contains("open") || normTemplate.contains("باز کن")) 1.0f else 0.0f
                IntentId.UNKNOWN, IntentId.GENERAL_QUESTION -> 0.0f
            }

            // S_sim: Phrase bank character 3-gram & Jaccard overlap
            val phraseBank = PHRASE_BANKS[intent] ?: emptyList()
            var maxSim = 0.0f
            for (phrase in phraseBank) {
                val sim = computeSimilarity(normTemplate, phrase.lowercase().replace("\u200C", ""))
                if (sim > maxSim) maxSim = sim
            }
            val sSim = maxSim

            // S_ent: Fraction of required slots extracted
            val required = when (intent) {
                IntentId.ADD_EGGS -> listOf(SlotId.COUNT)
                IntentId.ADD_TRANSACTION -> listOf(SlotId.AMOUNT)
                IntentId.ADD_FEED_WATER -> listOf(SlotId.COUNT)
                IntentId.LOG_MORTALITY -> listOf(SlotId.COUNT)
                IntentId.SELL_BIRDS -> listOf(SlotId.COUNT)
                IntentId.SLAUGHTER_BIRDS -> listOf(SlotId.COUNT)
                IntentId.TRANSFER_BIRDS -> listOf(SlotId.COUNT)
                IntentId.CONSUME_WAREHOUSE -> listOf(SlotId.COUNT)
                else -> emptyList()
            }
            val sEnt = if (required.isEmpty()) 1.0f else {
                val filled = required.count { extractedSlots.containsKey(it) }
                if (filled == required.size) 1.0f else {
                    if (sLex >= 0.6f && sPat >= 0.7f) 0.6f else 0.0f
                }
            }

            // Formula: score = 0.30*S_lex + 0.25*S_pat + 0.25*S_sim + 0.20*S_ent
            var finalScore = (0.30f * sLex) + (0.25f * sPat) + (0.25f * sSim) + (0.20f * sEnt)

            // Navigation command suppression on non-navigation intents
            val isNavCommand = normTemplate.startsWith("برو به") || normTemplate.startsWith("برو") ||
                    normTemplate.startsWith("باز کن") || normTemplate.startsWith("open") ||
                    normTemplate.startsWith("go to") || normTemplate.startsWith("navigate")
            if (isNavCommand && intent != IntentId.OPEN_SCREEN) {
                finalScore = 0.0f
            }

            // Suppress query intents if an explicit command verb or money is present
            if ((hasExplicitCommandVerb || extractedSlots.containsKey(SlotId.AMOUNT)) && (intent == IntentId.QUERY_EGGS || intent == IntentId.QUERY_FINANCE)) {
                finalScore = 0.0f
            }

            // Strict 3-way separation between Mortality vs Bird Sale vs Slaughter vs Generic Finance
            if (hasSlaughterCue && intent != IntentId.SLAUGHTER_BIRDS && intent != IntentId.QUERY_MEDICATION) {
                if (intent in listOf(IntentId.LOG_MORTALITY, IntentId.QUERY_MORTALITY, IntentId.SELL_BIRDS, IntentId.ADD_TRANSACTION, IntentId.QUERY_FLOCKS, IntentId.ADD_FEED_WATER, IntentId.CONSUME_WAREHOUSE)) {
                    finalScore *= 0.1f
                }
            }
            if (isBirdSaleIntentCue && intent in listOf(IntentId.ADD_TRANSACTION, IntentId.LOG_MORTALITY, IntentId.QUERY_MORTALITY, IntentId.SLAUGHTER_BIRDS, IntentId.QUERY_FLOCKS)) {
                finalScore *= if (intent == IntentId.ADD_TRANSACTION) 0.25f else 0.1f
            }
            if (hasEggWord && intent == IntentId.SELL_BIRDS) {
                finalScore = 0.0f
            }
            if (!hasExplicitCommandVerb && !extractedSlots.containsKey(SlotId.COUNT) &&
                (normTemplate.contains("چند") || normTemplate.contains("چقدر")) &&
                (intent == IntentId.SELL_BIRDS || intent == IntentId.SLAUGHTER_BIRDS)
            ) {
                finalScore *= 0.35f
            }

            // Question penalty on command intents
            if (hasQuestionWord && intent in listOf(IntentId.ADD_EGGS, IntentId.ADD_TRANSACTION, IntentId.SELL_BIRDS, IntentId.SLAUGHTER_BIRDS)) {
                finalScore *= 0.3f
            }

            scores[intent] = finalScore
            trace.add("Intent $intent: score=$finalScore (sLex=$sLex, sPat=$sPat, sSim=$sSim, sEnt=$sEnt)")
        }

        val sorted = scores.toList().sortedByDescending { it.second }
        val top = sorted.firstOrNull() ?: Pair(IntentId.UNKNOWN, 0.0f)
        val runnerUp = if (sorted.size > 1) sorted[1] else Pair(IntentId.UNKNOWN, 0.0f)
        val margin = top.second - runnerUp.second

        val alternatives = sorted.drop(1).take(2)

        val resolvedIntent = if (top.second < config.clarifyThreshold) {
            IntentId.GENERAL_QUESTION
        } else {
            top.first
        }

        return NluResult(
            intent = resolvedIntent,
            confidence = top.second,
            margin = margin,
            slots = extractedSlots,
            alternatives = alternatives,
            lang = lang,
            templateText = templateText,
            trace = trace
        )
    }

    private fun cleanForSimilarity(s: String): Set<String> {
        return s.lowercase()
            .replace("\u200C", " ")
            .replace(Regex("""[^\p{L}\p{Nd}\s<>]"""), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun computeSimilarity(s1: String, s2: String): Float {
        val tokens1 = cleanForSimilarity(s1)
        val tokens2 = cleanForSimilarity(s2)
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0f
        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size
        return intersection.toFloat() / union
    }
}
