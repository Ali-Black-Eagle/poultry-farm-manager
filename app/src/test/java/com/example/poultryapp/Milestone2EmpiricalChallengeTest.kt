package com.example.poultryapp

import com.example.poultryapp.data.local.*
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.engine.*
import com.example.poultryapp.voice.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Adversarial empirical challenge test suite for Milestone 2.
 * Tests anti-hijacking, packaging unit conversions, zero-count handling,
 * phonetic correction, decimal/date normalization, and hall number extraction.
 */
class Milestone2EmpiricalChallengeTest {

    private lateinit var mockFlocks: List<Flock>
    private lateinit var mockContacts: List<FarmContact>
    private lateinit var mockContext: FarmLiveContext

    @Before
    fun setUp() {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 3600 * 1000

        mockFlocks = listOf(
            Flock(
                id = 1L,
                name = "سالن ۱ تخمگذار",
                species = BirdSpecies.CHICKEN,
                breed = "بلک استار",
                birthEpochMillis = now - 150L * dayMillis,
                birdCount = 1000,
                customTempCelsius = 21.0
            ),
            Flock(
                id = 2L,
                name = "سالن ۲ گوشتی",
                species = BirdSpecies.CHICKEN,
                breed = "راس ۳۰۸",
                birthEpochMillis = now - 35L * dayMillis,
                birdCount = 2000,
                customTempCelsius = 22.0
            ),
            Flock(
                id = 3L,
                name = "سالن ۳ بومی",
                species = BirdSpecies.CHICKEN,
                breed = "گلپایگانی",
                birthEpochMillis = now - 90L * dayMillis,
                birdCount = 500,
                customTempCelsius = 20.0
            ),
            Flock(
                id = 25L,
                name = "سالن ۲۵ گوشتی",
                species = BirdSpecies.CHICKEN,
                breed = "آرین",
                birthEpochMillis = now - 20L * dayMillis,
                birdCount = 10000,
                customTempCelsius = 24.0
            )
        )

        mockContacts = listOf(
            FarmContact(
                id = 1L,
                fullName = "دکتر دارابی",
                phonePrimary = "09121112233",
                category = ContactCategory.VET_PHARMACY
            )
        )

        mockContext = FarmLiveContext(
            flocks = mockFlocks,
            contacts = mockContacts
        )
    }

    // =========================================================================
    // 1. Anti-Hijacking in Stage 7
    // =========================================================================

    @Test
    fun test01_AntiHijacking_PastTenseWithBreedName_RoutesToOperationalRecord() {
        val prompt = "سالن ۲ آرین ۵۰۰ کیلو دان مصرف کرد"
        val norm = PersianVoiceNormalizer.normalize(prompt)

        // 1. Check Semantic Router
        val routedIntent = FarmIntentSemanticRouter.routeIntent(norm, prompt, mockFlocks[1])
        assertEquals("Should route to OPERATIONAL_RECORD", FarmSemanticIntent.OPERATIONAL_RECORD, routedIntent)
        assertNotEquals("Must NOT be hijacked by KNOWLEDGE_API_QUERY", FarmSemanticIntent.KNOWLEDGE_API_QUERY, routedIntent)

        // 2. Check VoiceIntentParser
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = prompt,
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertTrue("Card must be valid", card.isValid)
        assertFalse("Should not need more info", card.needsMoreInfo)
        assertEquals("Intent must be RECORD_FEED", VoiceIntentType.RECORD_FEED, card.intentType)
        assertEquals("Flock ID must be 2", 2L, card.flockId)
        val amountKg = (card.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0
        assertEquals("Amount must be 500 kg", 500.0, amountKg, 0.01)

        // 3. Check Conversational AI Engine
        val turnResult = FarmConversationalAiEngine.processTurn(
            spokenText = prompt,
            currentState = AiSessionState(),
            farmContext = mockContext
        )
        assertNotNull("Turn result actionCard must not be null", turnResult.actionCard)
        assertEquals("Turn result intent must be RECORD_FEED", VoiceIntentType.RECORD_FEED, turnResult.actionCard?.intentType)
    }

    @Test
    fun test02_AntiHijacking_PastTenseWithSymptomName_RoutesToOperationalRecord() {
        val prompt = "امروز سالن ۱ پنج تا تلفات آسیت دادیم"
        val norm = PersianVoiceNormalizer.normalize(prompt)

        // 1. Check Semantic Router
        val routedIntent = FarmIntentSemanticRouter.routeIntent(norm, prompt, mockFlocks[0])
        assertEquals("Should route to OPERATIONAL_RECORD", FarmSemanticIntent.OPERATIONAL_RECORD, routedIntent)
        assertNotEquals("Must NOT be hijacked by KNOWLEDGE_API_QUERY", FarmSemanticIntent.KNOWLEDGE_API_QUERY, routedIntent)

        // 2. Check VoiceIntentParser
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = prompt,
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertTrue("Card must be valid", card.isValid)
        assertFalse("Should not need more info", card.needsMoreInfo)
        assertEquals("Intent must be RECORD_MORTALITY", VoiceIntentType.RECORD_MORTALITY, card.intentType)
        assertEquals("Flock ID must be 1", 1L, card.flockId)
        val count = (card.parameters["count"] as? Number)?.toInt() ?: 0
        assertEquals("Mortality count must be 5", 5, count)

        // 3. Check Conversational AI Engine
        val turnResult = FarmConversationalAiEngine.processTurn(
            spokenText = prompt,
            currentState = AiSessionState(),
            farmContext = mockContext
        )
        assertNotNull("Turn result actionCard must not be null", turnResult.actionCard)
        assertEquals("Turn result intent must be RECORD_MORTALITY", VoiceIntentType.RECORD_MORTALITY, turnResult.actionCard?.intentType)
    }

    @Test
    fun test03_AntiHijacking_PassiveDeclarativeCommands() {
        val prompt1 = "برای سالن ۳ پنجاه کیسه دان تخلیه شد"
        val norm1 = PersianVoiceNormalizer.normalize(prompt1)
        val routed1 = FarmIntentSemanticRouter.routeIntent(norm1, prompt1, mockFlocks[2])
        assertEquals(FarmSemanticIntent.OPERATIONAL_RECORD, routed1)

        val card1 = VoiceIntentParser.parseVoiceCommand(
            spokenText = prompt1,
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertEquals(VoiceIntentType.RECORD_FEED, card1.intentType)
        val amountKg1 = (card1.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0
        assertEquals(2500.0, amountKg1, 0.01) // 50 sacks * 50 kg

        val prompt2 = "سالن ۱ ده تا تلفات به خاطر آسیت دادیم"
        val norm2 = PersianVoiceNormalizer.normalize(prompt2)
        val routed2 = FarmIntentSemanticRouter.routeIntent(norm2, prompt2, mockFlocks[0])
        assertEquals(FarmSemanticIntent.OPERATIONAL_RECORD, routed2)

        val card2 = VoiceIntentParser.parseVoiceCommand(
            spokenText = prompt2,
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertEquals(VoiceIntentType.RECORD_MORTALITY, card2.intentType)
        val count2 = (card2.parameters["count"] as? Number)?.toInt() ?: 0
        assertEquals(10, count2)
    }

    // =========================================================================
    // 2. Packaging Unit Conversions
    // =========================================================================

    @Test
    fun test04_PackagingUnits_StandardCartons_180Eggs() {
        val prompt = "۱۰ کارتن تخم مرغ برای سالن ۱ ثبت کن"
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = prompt,
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertTrue(card.isValid)
        assertEquals(VoiceIntentType.RECORD_EGG, card.intentType)
        val totalEggs = (card.parameters["totalEggs"] as? Number)?.toInt() ?: 0
        val cartonCount = (card.parameters["cartonCount"] as? Number)?.toDouble() ?: 0.0
        val trayCount = (card.parameters["trayCount"] as? Number)?.toDouble() ?: 0.0

        assertEquals("10 standard cartons must equal 1800 eggs", 1800, totalEggs)
        assertEquals("cartonCount parameter must be 10.0", 10.0, cartonCount, 0.01)
        assertEquals("trayCount parameter must be 60.0 (1800/30)", 60.0, trayCount, 0.01)
    }

    @Test
    fun test05_PackagingUnits_ExportCartons_360Eggs() {
        val prompt = "۱۰ کارتن صادراتی تخم مرغ برای سالن ۱ ثبت کن"
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = prompt,
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertTrue(card.isValid)
        assertEquals(VoiceIntentType.RECORD_EGG, card.intentType)
        val totalEggs = (card.parameters["totalEggs"] as? Number)?.toInt() ?: 0
        val cartonCount = (card.parameters["cartonCount"] as? Number)?.toDouble() ?: 0.0
        val trayCount = (card.parameters["trayCount"] as? Number)?.toDouble() ?: 0.0

        assertEquals("10 export cartons must equal 3600 eggs", 3600, totalEggs)
        assertEquals("cartonCount parameter must be 10.0", 10.0, cartonCount, 0.01)
        assertEquals("trayCount parameter must be 120.0 (3600/30)", 120.0, trayCount, 0.01)
    }

    @Test
    fun test06_PackagingUnits_Trays_30Eggs() {
        val prompt = "۵ شانه تخم مرغ برای سالن ۱ ثبت کن"
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = prompt,
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertTrue(card.isValid)
        assertEquals(VoiceIntentType.RECORD_EGG, card.intentType)
        val totalEggs = (card.parameters["totalEggs"] as? Number)?.toInt() ?: 0
        val trayCount = (card.parameters["trayCount"] as? Number)?.toDouble() ?: 0.0
        val cartonCount = (card.parameters["cartonCount"] as? Number)?.toDouble() ?: 0.0

        assertEquals("5 trays must equal 150 eggs", 150, totalEggs)
        assertEquals("trayCount parameter must be 5.0", 5.0, trayCount, 0.01)
        assertEquals("cartonCount parameter must be 150/180", 150.0 / 180.0, cartonCount, 0.01)
    }

    @Test
    fun test07_PackagingUnits_Sacks_50Kg() {
        val prompt1 = "۴ کیسه دان برای سالن ۲ ثبت کن"
        val card1 = VoiceIntentParser.parseVoiceCommand(
            spokenText = prompt1,
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertTrue(card1.isValid)
        assertEquals(VoiceIntentType.RECORD_FEED, card1.intentType)
        val amountKg1 = (card1.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0
        val sackCount1 = (card1.parameters["sackCount"] as? Number)?.toDouble() ?: 0.0
        assertEquals("4 sacks must equal 200 kg", 200.0, amountKg1, 0.01)
        assertEquals("sackCount parameter must be 4.0", 4.0, sackCount1, 0.01)

        val prompt2 = "۱۰ گونی دان برای سالن ۲ ثبت کن"
        val card2 = VoiceIntentParser.parseVoiceCommand(
            spokenText = prompt2,
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertTrue(card2.isValid)
        val amountKg2 = (card2.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0
        val sackCount2 = (card2.parameters["sackCount"] as? Number)?.toDouble() ?: 0.0
        assertEquals("10 guni must equal 500 kg", 500.0, amountKg2, 0.01)
        assertEquals("sackCount parameter must be 10.0", 10.0, sackCount2, 0.01)
    }

    @Test
    fun test08_PackagingUnits_HalfUnits() {
        // Half carton
        val prompt1 = "ثبت نیم کارتن تخم مرغ سالن ۱"
        val card1 = VoiceIntentParser.parseVoiceCommand(prompt1, mockFlocks, mockContacts, farmContext = mockContext)
        val eggs1 = (card1.parameters["totalEggs"] as? Number)?.toInt() ?: 0
        assertEquals("Half carton must equal 90 eggs", 90, eggs1)

        // Half tray
        val prompt2 = "ثبت نیم شانه تخم مرغ سالن ۱"
        val card2 = VoiceIntentParser.parseVoiceCommand(prompt2, mockFlocks, mockContacts, farmContext = mockContext)
        val eggs2 = (card2.parameters["totalEggs"] as? Number)?.toInt() ?: 0
        assertEquals("Half tray must equal 15 eggs", 15, eggs2)

        // Half sack
        val prompt3 = "ثبت نیم کیسه دان سالن ۲"
        val card3 = VoiceIntentParser.parseVoiceCommand(prompt3, mockFlocks, mockContacts, farmContext = mockContext)
        val kg3 = (card3.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0
        assertEquals("Half sack must equal 25 kg", 25.0, kg3, 0.01)

        // Half ton
        val prompt4 = "ثبت نیم تن دان سالن ۲"
        val card4 = VoiceIntentParser.parseVoiceCommand(prompt4, mockFlocks, mockContacts, farmContext = mockContext)
        val kg4 = (card4.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0
        assertEquals("Half ton must equal 500 kg", 500.0, kg4, 0.01)
    }

    // =========================================================================
    // 3. Zero-Count Handling
    // =========================================================================

    @Test
    fun test09_ZeroCount_Mortality_ProducesValidCardWithoutMissingPrompt() {
        val testPhrases = listOf(
            "صفر تلفات",
            "ثبت صفر تلفات برای سالن ۱",
            "امروز سالن ۱ صفر تلفات داشتیم",
            "سالن ۱ تلفات نداشتیم",
            "امروز هیچ تلفاتی نداشتیم سالن ۱"
        )

        for (phrase in testPhrases) {
            val card = VoiceIntentParser.parseVoiceCommand(
                spokenText = phrase,
                flocks = mockFlocks,
                contacts = mockContacts,
                farmContext = mockContext
            )
            assertTrue("Phrase '$phrase' must be valid", card.isValid)
            assertFalse("Phrase '$phrase' must NOT need more info", card.needsMoreInfo)
            assertNull("Phrase '$phrase' must not have missing prompt", card.missingPromptFa)
            assertEquals("Phrase '$phrase' must be RECORD_MORTALITY", VoiceIntentType.RECORD_MORTALITY, card.intentType)
            val count = (card.parameters["count"] as? Number)?.toInt() ?: -1
            assertEquals("Phrase '$phrase' count must be 0", 0, count)
        }
    }

    @Test
    fun test10_ZeroCount_Eggs_ProducesValidCardWithoutMissingPrompt() {
        val testPhrases = listOf(
            "امروز هیچ تخم مرغ نداشتیم",
            "سالن ۱ امروز هیچ تخم مرغ نداشتیم",
            "برای سالن ۱ امروز هیچ تخم مرغی نداشتیم",
            "سالن ۱ تخم مرغ نداشتیم",
            "ثبت صفر تخم مرغ برای سالن ۱"
        )

        for (phrase in testPhrases) {
            val card = VoiceIntentParser.parseVoiceCommand(
                spokenText = phrase,
                flocks = mockFlocks,
                contacts = mockContacts,
                farmContext = mockContext
            )
            assertTrue("Phrase '$phrase' must be valid", card.isValid)
            assertFalse("Phrase '$phrase' must NOT need more info", card.needsMoreInfo)
            assertNull("Phrase '$phrase' must not have missing prompt", card.missingPromptFa)
            assertEquals("Phrase '$phrase' must be RECORD_EGG", VoiceIntentType.RECORD_EGG, card.intentType)
            val totalEggs = (card.parameters["totalEggs"] as? Number)?.toInt() ?: -1
            assertEquals("Phrase '$phrase' totalEggs must be 0", 0, totalEggs)
        }
    }

    // =========================================================================
    // 4. Acoustic & Phonetic Corrections (Ascites vs Acidifier)
    // =========================================================================

    @Test
    fun test11_PhoneticCorrector_AscitesVsAcidifier() {
        // Legitimate dietary acids MUST NOT be mutated to آسیت
        val citric = PoultryPhoneticCorrector.correctSpeechErrors("اسید سیتریک به دان جوجه اضافه کن")
        assertTrue("Must preserve اسید سیتریک", citric.contains("اسید سیتریک"))
        assertFalse("Must NOT convert to آسیت", citric.contains("آسیت"))

        val organic = PoultryPhoneticCorrector.correctSpeechErrors("دوز اسید ارگانیک در آبخوری مرغ‌ها چقدر است")
        assertTrue("Must preserve اسید ارگانیک", organic.contains("اسید ارگانیک"))
        assertFalse("Must NOT convert to آسیت", organic.contains("آسیت"))

        val acidifier = PoultryPhoneticCorrector.correctSpeechErrors("قیمت اسیدیفایر برای مرغداری")
        assertTrue("Must preserve اسیدیفایر", acidifier.contains("اسیدیفایر"))
        assertFalse("Must NOT convert to آسیت", acidifier.contains("آسیت"))

        // Acoustic misspelling of ascites (اسیت) MUST be converted to آسیت
        val ascitesAcoustic = PoultryPhoneticCorrector.correctSpeechErrors("جوجه‌ها اسیت گرفتن")
        assertTrue("Must convert اسیت to آسیت", ascitesAcoustic.contains("آسیت"))

        // Symptom marker present with اسید MUST convert to آسیت
        val ascitesSymptom = PoultryPhoneticCorrector.correctSpeechErrors("جوجه شکم آب آورده و اسید داره")
        assertTrue("Must convert to آسیت when abdominal dropsy symptom present", ascitesSymptom.contains("آسیت"))
    }

    // =========================================================================
    // 5. Persian Decimal Normalization & Date Protection
    // =========================================================================

    @Test
    fun test12_PersianVoiceNormalizer_MomayyezAndSlashDecimals() {
        // Momayyez U+066B normalized to dot
        val momayyez = PersianVoiceNormalizer.normalize("۲٫۵ کیلو دان")
        assertTrue("Momayyez must normalize to dot", momayyez.contains("2.5"))

        // Slash in decimal normalized to dot
        val slashDecimal = PersianVoiceNormalizer.normalize("۲/۵ کیلو دان")
        assertTrue("Slash decimal must normalize to dot", slashDecimal.contains("2.5"))

        // Jalali date with slash must NOT be converted to dot
        val jalaliDate = PersianVoiceNormalizer.normalize("تلفات تاریخ 1403/08/10 سالن ۱")
        assertTrue("Jalali date must preserve slashes", jalaliDate.contains("1403/08/10"))
        assertFalse("Jalali date must not convert slashes to dots", jalaliDate.contains("1403.08.10"))
    }

    // =========================================================================
    // 6. Hall Number Support 1..100
    // =========================================================================

    @Test
    fun test13_HallNumbers_1To100_And_NoFeedConfusion() {
        // Hall 25 numeric
        val card25 = VoiceIntentParser.parseVoiceCommand(
            spokenText = "سالن ۲۵ پانصد کیلو دان ثبت کن",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertEquals(25L, card25.flockId)
        val kg25 = (card25.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0
        assertEquals(500.0, kg25, 0.01)

        // Hall 25 words
        val card25Word = VoiceIntentParser.parseVoiceCommand(
            spokenText = "سالن بیست و پنج پانصد کیلو دان ثبت کن",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertEquals(25L, card25Word.flockId)

        // Feed quantity word 'دویست' must NOT be mistaken for hall number
        val cardFeed = VoiceIntentParser.parseVoiceCommand(
            spokenText = "سالن ۱ دویست کیلو دان ثبت کن",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = mockContext
        )
        assertEquals("Must match hall 1, not 200", 1L, cardFeed.flockId)
        val kgFeed = (cardFeed.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0
        assertEquals(200.0, kgFeed, 0.01)
    }
}
