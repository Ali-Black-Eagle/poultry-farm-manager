package com.example.poultryapp

import com.example.poultryapp.data.local.ContactCategory
import com.example.poultryapp.data.local.FarmContact
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.data.local.TransactionType
import com.example.poultryapp.data.local.TransactionCategory
import com.example.poultryapp.data.local.EggProduction
import com.example.poultryapp.data.local.FlockSubGroup
import com.example.poultryapp.data.local.WarehouseItem
import com.example.poultryapp.data.local.WarehouseCategory
import com.example.poultryapp.data.local.FinancialTransaction
import com.example.poultryapp.data.local.UserRole
import com.example.poultryapp.engine.FarmLiveContext
import com.example.poultryapp.voice.PersianVoiceNormalizer
import com.example.poultryapp.voice.VoiceIntentParser
import com.example.poultryapp.voice.VoiceIntentType
import com.example.poultryapp.voice.VoiceSpeaker
import com.example.poultryapp.voice.VoiceActionCardData
import com.example.poultryapp.voice.FarmConversationalAiEngine
import com.example.poultryapp.voice.AiSessionState
import com.example.poultryapp.voice.ConversationTurn
import com.example.poultryapp.voice.TurnSender
import com.example.poultryapp.voice.LocalPoultryAiAgent
import com.example.poultryapp.voice.DefaultAiQuestionAnswerBridge
import com.example.poultryapp.voice.AiQuestionAnswerBridge
import com.example.poultryapp.voice.AgentExecutionMode
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.data.local.FeedWaterCharge
import com.example.poultryapp.data.local.FlockMortalityLog
import com.example.poultryapp.data.local.MortalityReason
import com.example.poultryapp.voice.AgenticProactiveAction
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VoiceIntelligenceUnitTest {

    private lateinit var mockFlocks: List<Flock>
    private lateinit var mockContacts: List<FarmContact>

    @Before
    fun setUp() {
        mockFlocks = listOf(
            Flock(
                id = 1L,
                name = "گله تخمگذار ۱",
                species = BirdSpecies.CHICKEN,
                breed = "لگهورن",
                birthEpochMillis = System.currentTimeMillis() - 100L * 24 * 3600 * 1000,
                birdCount = 1000
            ),
            Flock(
                id = 2L,
                name = "سالن ۲ گوشتی",
                species = BirdSpecies.CHICKEN,
                breed = "راس ۳۰۸",
                birthEpochMillis = System.currentTimeMillis() - 30L * 24 * 3600 * 1000,
                birdCount = 2000
            ),
            Flock(
                id = 3L,
                name = "سالن ۳ بومی",
                species = BirdSpecies.CHICKEN,
                breed = "گلپایگانی",
                birthEpochMillis = System.currentTimeMillis() - 60L * 24 * 3600 * 1000,
                birdCount = 500
            )
        )

        mockContacts = listOf(
            FarmContact(
                id = 1L,
                fullName = "دکتر دارابی",
                category = ContactCategory.VET_PHARMACY,
                phonePrimary = "09121234567"
            )
        )
    }

    @Test
    fun testPersianVoiceNormalizer_NumbersAndColloquials() {
        val text1 = "امروز پونصد کیلو دان به سالن ۲ دادیم"
        val normalized1 = PersianVoiceNormalizer.normalize(text1)
        assertTrue("Normalized text should contain converted Persian digit 2", normalized1.contains("2"))

        val numFromText = PersianVoiceNormalizer.extractFirstNumber(normalized1, excludePhrase = "سالن 2")
        assertEquals("Primary quantity must be 500 when 'سالن 2' is excluded", 500.0, numFromText ?: 0.0, 0.01)

        val text2 = "هفتصد و پنجاه"
        val num2 = PersianVoiceNormalizer.extractFirstNumber(text2)
        assertEquals(750.0, num2 ?: 0.0, 0.01)
    }

    @Test
    fun testVoiceIntentParser_FlockEggCollectionVariousPhrasings() {
        // Phrasing 1: Formal
        val card1 = VoiceIntentParser.parseVoiceCommand("ثبت ۲۰۰ تخم مرغ برای گله ۱", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_EGG, card1.intentType)
        assertTrue(card1.isValid)
        assertEquals(1L, card1.flockId)
        assertEquals(200, (card1.parameters["count"] as? Number)?.toInt())

        // Phrasing 2: Colloquial with salon token and word numbers
        val card2 = VoiceIntentParser.parseVoiceCommand("امروز پنجاه تا تخم جمع کردیم توی سالن ۲", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_EGG, card2.intentType)
        assertTrue(card2.isValid)
        assertEquals(2L, card2.flockId)
        assertEquals(50, (card2.parameters["count"] as? Number)?.toInt())

        // Phrasing 3: Salon first then quantity
        val card3 = VoiceIntentParser.parseVoiceCommand("سالن ۳ صد و بیست عدد تخم مرغ", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_EGG, card3.intentType)
        assertTrue(card3.isValid)
        assertEquals(3L, card3.flockId)
        assertEquals(120, (card3.parameters["count"] as? Number)?.toInt())
    }

    @Test
    fun testVoiceIntentParser_FlockMortalityNoNumberCollisions() {
        // Here flock is 1 and mortality is 3
        val card1 = VoiceIntentParser.parseVoiceCommand("ثبت ۳ تلفات در گله ۱", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_MORTALITY, card1.intentType)
        assertTrue(card1.isValid)
        assertEquals(1L, card1.flockId)
        assertEquals(3, (card1.parameters["count"] as? Number)?.toInt())

        // Here salon is 2 and casualties is 10
        val card2 = VoiceIntentParser.parseVoiceCommand("سالن ۲ ده تا مرغ تلف شدند", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_MORTALITY, card2.intentType)
        assertTrue(card2.isValid)
        assertEquals(2L, card2.flockId)
        assertEquals(10, (card2.parameters["count"] as? Number)?.toInt())

        // Casual phrasing
        val card3 = VoiceIntentParser.parseVoiceCommand("توی سالن ۳ پنج تا تلفات داشتیم", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_MORTALITY, card3.intentType)
        assertTrue(card3.isValid)
        assertEquals(3L, card3.flockId)
        assertEquals(5, (card3.parameters["count"] as? Number)?.toInt())
    }

    @Test
    fun testVoiceIntentParser_FeedAndWaterOperations() {
        val feedCard = VoiceIntentParser.parseVoiceCommand("برای سالن ۱ پنجاه کیلو دون بریز", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FEED, feedCard.intentType)
        assertTrue(feedCard.isValid)
        assertEquals(1L, feedCard.flockId)
        assertEquals(50.0, (feedCard.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0, 0.01)

        val waterCard = VoiceIntentParser.parseVoiceCommand("مصرف اب گله ۲ سیصد لیتر", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_WATER, waterCard.intentType)
        assertTrue(waterCard.isValid)
        assertEquals(2L, waterCard.flockId)
        assertEquals(300.0, (waterCard.parameters["amountLiters"] as? Number)?.toDouble() ?: 0.0, 0.01)
    }

    @Test
    fun testVoiceIntentParser_NavigationAndContactQueries() {
        val navWarehouse = VoiceIntentParser.parseVoiceCommand("برو به انبار و موجودی دان", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.NAVIGATE_SCREEN, navWarehouse.intentType)
        assertTrue(navWarehouse.isValid)
        assertEquals("WAREHOUSE", navWarehouse.parameters["targetScreen"])

        val navRation = VoiceIntentParser.parseVoiceCommand("صفحه جیره غذایی را بیار", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.NAVIGATE_SCREEN, navRation.intentType)
        assertTrue(navRation.isValid)
        assertEquals("RATION", navRation.parameters["targetScreen"])

        val contactCall = VoiceIntentParser.parseVoiceCommand("با دکتر دارابی تماس بگیر", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.CALL_CONTACT, contactCall.intentType)
        assertTrue(contactCall.isValid)
        assertEquals("09121234567", contactCall.parameters["phoneNumber"])
    }

    @Test
    fun testVoiceIntentParser_FinancialTransactionsOmniIntelligence() {
        // User's exact command from the screenshot: "۲۰۰ هزار تومان هزینه"
        val card1 = VoiceIntentParser.parseVoiceCommand("۲۰۰ هزار تومان هزینه", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, card1.intentType)
        assertTrue(card1.isValid)
        assertEquals(200000.0, (card1.parameters["amountTomans"] as? Number)?.toDouble() ?: 0.0, 0.01)
        assertEquals(TransactionType.EXPENSE, card1.parameters["type"])
        assertEquals(TransactionCategory.OTHER_EXPENSE, card1.parameters["category"])

        // Feed purchase expense: "ثبت هزینه ۵۰۰ هزار تومان دان"
        val card2 = VoiceIntentParser.parseVoiceCommand("ثبت هزینه ۵۰۰ هزار تومان دان", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, card2.intentType)
        assertTrue(card2.isValid)
        assertEquals(500000.0, (card2.parameters["amountTomans"] as? Number)?.toDouble() ?: 0.0, 0.01)
        assertEquals(TransactionType.EXPENSE, card2.parameters["type"])
        assertEquals(TransactionCategory.FEED_PURCHASE, card2.parameters["category"])

        // Egg sale income: "یک میلیون تومان درآمد فروش تخم مرغ"
        val card3 = VoiceIntentParser.parseVoiceCommand("یک میلیون تومان درآمد فروش تخم مرغ", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, card3.intentType)
        assertTrue(card3.isValid)
        assertEquals(1000000.0, (card3.parameters["amountTomans"] as? Number)?.toDouble() ?: 0.0, 0.01)
        assertEquals(TransactionType.INCOME, card3.parameters["type"])
        assertEquals(TransactionCategory.EGG_SALE, card3.parameters["category"])

        // Bird sale income: "۳ میلیون تومان فروش پرنده"
        val card4 = VoiceIntentParser.parseVoiceCommand("۳ میلیون تومان فروش پرنده", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, card4.intentType)
        assertTrue(card4.isValid)
        assertEquals(3000000.0, (card4.parameters["amountTomans"] as? Number)?.toDouble() ?: 0.0, 0.01)
        assertEquals(TransactionType.INCOME, card4.parameters["type"])
        assertEquals(TransactionCategory.BIRD_SALE, card4.parameters["category"])

        // Equipment expense: "هزینه برق ۱۰۰ هزار تومان"
        val card5 = VoiceIntentParser.parseVoiceCommand("هزینه برق ۱۰۰ هزار تومان", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, card5.intentType)
        assertTrue(card5.isValid)
        assertEquals(100000.0, (card5.parameters["amountTomans"] as? Number)?.toDouble() ?: 0.0, 0.01)
        assertEquals(TransactionCategory.EQUIPMENT, card5.parameters["category"])

        // Rial conversion to Tomans: "۲۰ میلیون ریال هزینه" -> 2,000,000 Tomans
        val card6 = VoiceIntentParser.parseVoiceCommand("۲۰ میلیون ریال هزینه", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, card6.intentType)
        assertTrue(card6.isValid)
        assertEquals(2000000.0, (card6.parameters["amountTomans"] as? Number)?.toDouble() ?: 0.0, 0.01)
    }

    @Test
    fun testVoiceIntentParser_MedicationAndQueries() {
        val medCard = VoiceIntentParser.parseVoiceCommand("ثبت مصرف ویتامین در سالن ۱", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_MEDICATION, medCard.intentType)
        assertTrue(medCard.isValid)
        assertEquals(1L, medCard.flockId)
        assertTrue((medCard.parameters["medName"] as? String)?.contains("ویتامین") == true)

        val queryCard = VoiceIntentParser.parseVoiceCommand("امروز چقدر دان مصرف کردیم؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.QUERY_STATS, queryCard.intentType)
        assertTrue(queryCard.isValid)
    }

    @Test
    fun testVoiceIntentParser_IrrelevantAndUnrecognizedCommandsMustBeInvalid() {
        // Irrelevant casual chit-chat
        val irrelevant1 = VoiceIntentParser.parseVoiceCommand("سلام چطوری هوا امروز خیلی خوبه", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.UNKNOWN, irrelevant1.intentType)
        assertFalse("Casual chit chat must be marked invalid", irrelevant1.isValid)
        assertTrue(irrelevant1.systemEffect.contains("تغییری"))

        // Unrelated food request
        val irrelevant2 = VoiceIntentParser.parseVoiceCommand("ناهار چی داریم امروز", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.UNKNOWN, irrelevant2.intentType)
        assertFalse("Irrelevant food query must be invalid", irrelevant2.isValid)

        // Empty command
        val emptyCard = VoiceIntentParser.parseVoiceCommand("", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.UNKNOWN, emptyCard.intentType)
        assertFalse("Empty speech must be invalid", emptyCard.isValid)
    }

    @Test
    fun testVoiceIntentParser_DiseaseDiagnosisAndSymptomExtraction() {
        // Test 1: Symptoms inquiry with respiratory rales & white diarrhea
        val card1 = VoiceIntentParser.parseVoiceCommand("طیورم خس خس سینه دارن و اسهال سفید، چه بیماری دارن؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, card1.intentType)
        assertTrue(card1.isValid)
        assertNotNull(card1.parameters["diseaseId"])
        assertTrue(card1.primaryMetric.contains("احتمال"))

        // Test 2: Cecal Coccidiosis bloody diarrhea symptom
        val card2 = VoiceIntentParser.parseVoiceCommand("اسهال خونی در سالن ۲ نشانه چه بیماری است؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, card2.intentType)
        assertTrue(card2.isValid)
        val diseaseName2 = card2.parameters["diseaseName"] as? String
        assertTrue("Disease should be coccidiosis", diseaseName2?.contains("کوکسیدیوز") == true)

        // Test 3: Direct disease inquiry for Newcastle
        val card3 = VoiceIntentParser.parseVoiceCommand("علائم نیوکاسل چیست؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, card3.intentType)
        assertTrue(card3.isValid)
        assertEquals("newcastle", card3.parameters["diseaseId"])

        // Test 4: Direct disease inquiry for Coccidiosis treatment
        val card4 = VoiceIntentParser.parseVoiceCommand("درمان کوکسیدیوز چیه؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, card4.intentType)
        assertTrue(card4.isValid)
        assertTrue((card4.parameters["diseaseName"] as? String)?.contains("کوکسیدیوز") == true)

        // Test 5: Direct question for Necrotic Enteritis
        val card5 = VoiceIntentParser.parseVoiceCommand("درمان انتریت نکروتیک چیه؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, card5.intentType)
        assertTrue(card5.isValid)
        assertEquals("necrotic_enteritis", card5.parameters["diseaseId"])

        // Test 6: Bowed legs deficiency symptom inquiry
        val card6 = VoiceIntentParser.parseVoiceCommand("پای پرانتزی جوجه ها نشانه چه بیماری یا کمبودی است؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, card6.intentType)
        assertTrue(card6.isValid)
        assertTrue((card6.parameters["diseaseName"] as? String)?.contains("نیاسین") == true || (card6.parameters["diseaseName"] as? String)?.contains("کمبود") == true)

        // Test 7: Soft rubbery beak inquiry
        val card7 = VoiceIntentParser.parseVoiceCommand("نوک لاستیکی و منقار نرم مرغها علائم چیست؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, card7.intentType)
        assertTrue(card7.isValid)
        assertNotNull(card7.parameters["diseaseId"])

        // Test 8: Direct spoken symptom "پرنده من لنگ می‌زند"
        val card8 = VoiceIntentParser.parseVoiceCommand("پرنده من لنگ می‌زند", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, card8.intentType)
        assertTrue(card8.isValid)
        assertNotNull(card8.parameters["diseaseId"])

        // Test 9: Colloquial "مرغم میلنگه"
        val card9 = VoiceIntentParser.parseVoiceCommand("مرغم میلنگه", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, card9.intentType)
        assertTrue(card9.isValid)
        assertNotNull(card9.parameters["diseaseId"])

        // Test 10: Respiratory symptom "جوجه ها خس خس میکنن"
        val card10 = VoiceIntentParser.parseVoiceCommand("جوجه ها خس خس میکنن", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, card10.intentType)
        assertTrue(card10.isValid)
        assertNotNull(card10.parameters["diseaseId"])
    }

    @Test
    fun testVoiceIntentParser_AgeAwareVeterinaryConsultationAndDifferentialDiagnosis() {
        // Test Spoken Age Extraction
        val age1 = PersianVoiceNormalizer.extractFlockAge("جوجه‌های ۳ هفته لنگ می‌زنند")
        assertNotNull(age1)
        assertEquals(3, age1?.first)
        assertEquals("WEEKS", age1?.second)

        val age2 = PersianVoiceNormalizer.extractFlockAge("مرغ‌های ۲۵ روزه اسهال خونی دارند")
        assertNotNull(age2)
        assertEquals(25, age2?.first)
        assertEquals("DAYS", age2?.second)

        val age3 = PersianVoiceNormalizer.extractFlockAge("مرغ‌های تخمگذار افت تخم دارن")
        assertNotNull(age3)
        assertEquals("WEEKS", age3?.second)

        // Test Nutritional Deficiency Diagnosis with Age Context
        val cardNutr = VoiceIntentParser.parseVoiceCommand("جوجه‌های ۳ هفته پاهای پرانتزی دارن و لنگ می‌زنند", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, cardNutr.intentType)
        assertTrue(cardNutr.isValid)
        assertEquals(true, cardNutr.parameters["isNutritional"])
        assertNotNull(cardNutr.parameters["clinicalReasoningFa"])
        assertNotNull(cardNutr.parameters["immediateActionFa"])

        // Verify Differential Diagnosis list is populated
        val differentials = cardNutr.parameters["differentials"] as? List<Map<String, Any>>
        assertNotNull(differentials)
        assertTrue("Should have differential diagnosis options", differentials?.isNotEmpty() == true)

        // Test Adult Laying Syndrome
        val cardLaying = VoiceIntentParser.parseVoiceCommand("مرغ‌های تخمگذار تخم لمبه و پوسته نرم می‌گذارند و عطسه می‌کنند", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, cardLaying.intentType)
        assertTrue(cardLaying.isValid)
        val dId = cardLaying.parameters["diseaseId"] as? String
        assertTrue("Should be laying-related pathology", dId == "infectious_bronchitis" || dId == "egg_drop_syndrome" || dId == "rickets_d3_calcium")
    }

    @Test
    fun testVoiceIntentParser_MultiTurnFlockCreationInteractive() {
        // نوبت اول: کاربر دستور ناقص اعلام می‌کند: "یک گله آمروک اضافه کن"
        val turn1Card = VoiceIntentParser.parseVoiceCommand("یک گله آمروک اضافه کن", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.CREATE_FLOCK, turn1Card.intentType)
        assertFalse("دستور به دلیل کسری تعداد نباید به صورت قطعی قابل اجرا باشد", turn1Card.isValid)
        assertTrue("باید وضعیت نیاز به اطلاعات تکمیلی فعال باشد", turn1Card.needsMoreInfo)
        assertNotNull("باید پیام پرسش کسری ثبت شده باشد", turn1Card.missingPromptFa)
        assertTrue("پیام باید حاوی نام نژاد باشد", turn1Card.missingPromptFa!!.contains("آمروک"))
        assertTrue("باید گزینه‌های پاسخ سریع ارائه شده باشد", turn1Card.quickReplies.isNotEmpty())
        assertEquals("CREATE_FLOCK", turn1Card.parameters["pendingIntent"])

        // نوبت دوم: کاربر پاسخ تکمیلی می‌دهد: "۵۰ تا تخمگذار"
        val turn2Card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "۵۰ تا تخمگذار",
            flocks = mockFlocks,
            contacts = mockContacts,
            sessionContext = turn1Card.parameters
        )
        assertEquals(VoiceIntentType.CREATE_FLOCK, turn2Card.intentType)
        assertTrue("پس از تکمیل اطلاعات، کارت باید معتبر باشد", turn2Card.isValid)
        assertFalse("دیگر نیازی به اطلاعات تکمیلی نیست", turn2Card.needsMoreInfo)
        assertEquals(50, turn2Card.parameters["birdCount"])
        assertEquals("آمروک", turn2Card.parameters["breed"])
        assertEquals(BirdSpecies.CHICKEN, turn2Card.parameters["species"])
        assertTrue("نام گله باید به درستی تشکیل شده باشد", (turn2Card.parameters["flockName"] as String).contains("آمروک"))
    }

    @Test
    fun testVoiceIntentParser_OneShotFlockCreation() {
        val card = VoiceIntentParser.parseVoiceCommand("یک گله ۱۰۰ تایی آمروک اضافه کن", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.CREATE_FLOCK, card.intentType)
        assertTrue(card.isValid)
        assertFalse(card.needsMoreInfo)
        assertEquals(100, card.parameters["birdCount"])
        assertEquals("آمروک", card.parameters["breed"])
    }

    @Test
    fun testVoiceIntentParser_MultiTurnMissingParameters_EggMortalityFeedFinancial() {
        // ۱. تخم‌مرغ بدون تعداد -> پرسش تکمیلی -> پاسخ با شانه
        val eggTurn1 = VoiceIntentParser.parseVoiceCommand("ثبت تخم مرغ سالن ۱", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_EGG, eggTurn1.intentType)
        assertTrue(eggTurn1.needsMoreInfo)
        assertNotNull(eggTurn1.missingPromptFa)
        val eggTurn2 = VoiceIntentParser.parseVoiceCommand("۲ شانه", mockFlocks, mockContacts, eggTurn1.parameters)
        assertTrue(eggTurn2.isValid)
        assertEquals(60, eggTurn2.parameters["count"])

        // ۲. تلفات بدون تعداد -> پرسش تکمیلی -> پاسخ با ۳ قطعه
        val mortTurn1 = VoiceIntentParser.parseVoiceCommand("ثبت تلفات سالن ۲", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_MORTALITY, mortTurn1.intentType)
        assertTrue(mortTurn1.needsMoreInfo)
        val mortTurn2 = VoiceIntentParser.parseVoiceCommand("۳ تا تلفات به خاطر گرما", mockFlocks, mockContacts, mortTurn1.parameters)
        assertTrue(mortTurn2.isValid)
        assertEquals(3, mortTurn2.parameters["count"])

        // ۳. دان بدون مقدار -> پرسش تکمیلی -> پاسخ با ۱ کیسه (۵۰ کیلوگرم)
        val feedTurn1 = VoiceIntentParser.parseVoiceCommand("شارژ دان سالن ۱", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FEED, feedTurn1.intentType)
        assertTrue(feedTurn1.needsMoreInfo)
        val feedTurn2 = VoiceIntentParser.parseVoiceCommand("۱ کیسه", mockFlocks, mockContacts, feedTurn1.parameters)
        assertTrue(feedTurn2.isValid)
        assertEquals(50.0, (feedTurn2.parameters["amount"] as Number).toDouble(), 0.01)

        // ۴. مالی بدون مبلغ -> پرسش تکمیلی -> پاسخ با ۲۰۰ هزار تومان
        val finTurn1 = VoiceIntentParser.parseVoiceCommand("ثبت هزینه خرید دان", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, finTurn1.intentType)
        assertTrue(finTurn1.needsMoreInfo)
        val finTurn2 = VoiceIntentParser.parseVoiceCommand("۲۰۰ هزار تومان", mockFlocks, mockContacts, finTurn1.parameters)
        assertTrue(finTurn2.isValid)
        assertEquals(200000.0, (finTurn2.parameters["amountTomans"] as Number).toDouble(), 0.01)
    }

    @Test
    fun testVoiceSpeaker_CleanForSpeech() {
        val raw = "📋 هشدار: افت ۳.۵٪ در راندمان تخم‌گذاری گله! [بررسی شود] ✅"
        val cleaned = VoiceSpeaker.cleanForSpeech(raw)
        assertFalse(cleaned.contains("📋"))
        assertFalse(cleaned.contains("✅"))
        assertFalse(cleaned.contains("["))
        assertFalse(cleaned.contains("]"))
        assertFalse(cleaned.contains(":"))
        assertTrue(cleaned.contains("هشدار"))
        assertTrue(cleaned.contains("تخم‌گذاری"))
    }

    @Test
    fun testVoiceIntentParser_CompoundActions_Triple() {
        val spoken = "امروز ۵۰ تا تخم مرغ، ۲ تا تلفات و ۳۰ کیلو دان ثبت کن"
        val card = VoiceIntentParser.parseVoiceCommand(spoken, mockFlocks, mockContacts)

        assertEquals(VoiceIntentType.COMPOUND_ACTION, card.intentType)
        assertTrue(card.isValid)
        assertEquals(3, card.subActions.size)

        val subTypes = card.subActions.map { it.intentType }
        assertTrue(subTypes.contains(VoiceIntentType.RECORD_EGG))
        assertTrue(subTypes.contains(VoiceIntentType.RECORD_MORTALITY))
        assertTrue(subTypes.contains(VoiceIntentType.RECORD_FEED))

        val eggSub = card.subActions.first { it.intentType == VoiceIntentType.RECORD_EGG }
        assertEquals(50, eggSub.parameters["count"])

        val mortSub = card.subActions.first { it.intentType == VoiceIntentType.RECORD_MORTALITY }
        assertEquals(2, mortSub.parameters["count"])

        val feedSub = card.subActions.first { it.intentType == VoiceIntentType.RECORD_FEED }
        assertEquals(30.0, (feedSub.parameters["amount"] as Number).toDouble(), 0.01)
    }

    @Test
    fun testVoiceIntentParser_CompoundActions_DoubleWithoutComma() {
        val spoken = "ثبت ۵۰ تا تخم مرغ و ۲ تلفات"
        val card = VoiceIntentParser.parseVoiceCommand(spoken, mockFlocks, mockContacts)

        assertEquals(VoiceIntentType.COMPOUND_ACTION, card.intentType)
        assertTrue(card.isValid)
        assertEquals(2, card.subActions.size)

        val subTypes = card.subActions.map { it.intentType }
        assertTrue(subTypes.contains(VoiceIntentType.RECORD_EGG))
        assertTrue(subTypes.contains(VoiceIntentType.RECORD_MORTALITY))
    }

    @Test
    fun testVoiceIntentParser_LiveLayingRateQuery() {
        val mockEggLogs = listOf(
            EggProduction(
                id = 1L,
                flockId = 1L,
                flockName = "گله تخمگذار ۱",
                healthyCount = 850,
                totalCount = 850,
                layingRatePercent = 85.0,
                dateJalaliStr = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString(),
                timestampMillis = System.currentTimeMillis()
            )
        )

        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "راندمان تخم‌گذاری چقدره؟",
            flocks = mockFlocks,
            contacts = mockContacts,
            eggProductions = mockEggLogs
        )
        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue(card.isValid)
        assertTrue(card.title.contains("راندمان"))
        assertTrue(card.primaryMetric.contains("۸۵"))
    }

    @Test
    fun testVoiceIntentParser_FlockBreedCount_UserAudioCase1() {
        val amrockFlock = Flock(
            id = 1L,
            name = "گله شماره ۱",
            species = BirdSpecies.CHICKEN,
            breed = "آمروک",
            birthEpochMillis = System.currentTimeMillis() - (25L * 7 * 24 * 3600 * 1000),
            birdCount = 50
        )
        val farmContext = FarmLiveContext(
            flocks = listOf(amrockFlock)
        )

        // سناریوی صوتی اول کاربر: «در گله یک چند آمروک داریم»
        val card1 = VoiceIntentParser.parseVoiceCommand(
            spokenText = "در گله یک چند آمروک داریم",
            flocks = listOf(amrockFlock),
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card1.intentType)
        assertTrue(card1.isValid)
        assertEquals(1L, card1.flockId)
        assertEquals(50, card1.parameters["count"])
        assertEquals("آمروک", card1.parameters["breed"])
        val spoken1 = card1.parameters["spokenSummary"] as? String
        assertNotNull(spoken1)
        assertTrue("Spoken text must mention breed Amrock", spoken1!!.contains("آمروک"))
        assertTrue("Spoken text must state count 50", spoken1.contains("۵۰"))
    }

    @Test
    fun testVoiceIntentParser_FlockBreedCount_UserAudioCase2() {
        val amrockFlock = Flock(
            id = 1L,
            name = "گله شماره ۱",
            species = BirdSpecies.CHICKEN,
            breed = "آمروک",
            birthEpochMillis = System.currentTimeMillis() - (25L * 7 * 24 * 3600 * 1000),
            birdCount = 50
        )
        val farmContext = FarmLiveContext(
            flocks = listOf(amrockFlock)
        )

        // سناریوی صوتی دوم کاربر: «چند مرغ آمروک در گله شماره یک داریم»
        val card2 = VoiceIntentParser.parseVoiceCommand(
            spokenText = "چند مرغ آمروک در گله شماره یک داریم",
            flocks = listOf(amrockFlock),
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card2.intentType)
        assertTrue(card2.isValid)
        assertEquals(1L, card2.flockId)
        assertEquals(50, card2.parameters["count"])
        val spoken2 = card2.parameters["spokenSummary"] as? String
        assertNotNull(spoken2)
        assertTrue(spoken2!!.contains("آمروک"))
        assertTrue(spoken2.contains("۵۰"))
    }

    @Test
    fun testVoiceIntentParser_OverallFlocksStatus_UserAudioCase3() {
        val flocksList = listOf(
            Flock(
                id = 1L,
                name = "گله تخمگذار ۱",
                species = BirdSpecies.CHICKEN,
                breed = "آمروک",
                birthEpochMillis = System.currentTimeMillis() - (20L * 7 * 24 * 3600 * 1000),
                birdCount = 50
            ),
            Flock(
                id = 2L,
                name = "سالن ۲ گوشتی",
                species = BirdSpecies.CHICKEN,
                breed = "راس ۳۰۸",
                birthEpochMillis = System.currentTimeMillis() - (5L * 7 * 24 * 3600 * 1000),
                birdCount = 100
            )
        )
        val todayStr = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
        val eggLogs = listOf(
            EggProduction(
                id = 1L,
                flockId = 1L,
                flockName = "گله تخمگذار ۱",
                healthyCount = 45,
                totalCount = 45,
                layingRatePercent = 90.0,
                dateJalaliStr = todayStr,
                timestampMillis = System.currentTimeMillis()
            )
        )
        val farmContext = FarmLiveContext(
            flocks = flocksList,
            eggProductions = eggLogs
        )

        // سناریوی سوم کاربر: «وضعیت گله‌ها چگونه است»
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "وضعیت گله‌ها چگونه است",
            flocks = flocksList,
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue(card.isValid)
        assertTrue("Card title must be overall status", card.title.contains("وضعیت گله‌های فارم"))
        assertEquals(2, card.parameters["totalFlocks"])
        assertEquals(150, card.parameters["totalBirds"])
        assertEquals(45, card.parameters["todayEggs"])
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue("Spoken text must report total birds", spoken!!.contains("۱۵۰"))
        assertTrue("Spoken text must report 2 flocks", spoken.contains("۲"))
    }

    @Test
    fun testVoiceIntentParser_WarehouseStockQuery() {
        val warehouseItems = listOf(
            WarehouseItem(
                id = 1L,
                name = "دان پلیت تخمگذار",
                category = WarehouseCategory.FEED,
                currentStock = 250.0,
                unit = "کیلوگرم",
                pricePerUnit = 20000.0,
                minAlertThreshold = 50.0
            )
        )
        val farmContext = FarmLiveContext(
            flocks = mockFlocks,
            warehouseItems = warehouseItems
        )

        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "موجودی دان در انبار چقدر است",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue(card.isValid)
        assertTrue(card.title.contains("انبار"))
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue(spoken!!.contains("۲۵۰ کیلوگرم"))
    }

    @Test
    fun testVoiceIntentParser_MedicationWithdrawalQuery() {
        val flocksWithWithdrawal = listOf(
            Flock(
                id = 1L,
                name = "سالن ۱",
                species = BirdSpecies.CHICKEN,
                breed = "آمروک",
                birthEpochMillis = System.currentTimeMillis() - (10L * 7 * 24 * 3600 * 1000),
                birdCount = 100,
                medicationWithdrawalEndMillis = System.currentTimeMillis() + (3L * 24 * 3600 * 1000),
                activeMedicationName = "انروسین"
            )
        )
        val farmContext = FarmLiveContext(
            flocks = flocksWithWithdrawal
        )

        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "کدام گله دوره پرهیز فعال دارد",
            flocks = flocksWithWithdrawal,
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue(card.isValid)
        assertTrue(card.primaryMetric.contains("پرهیز"))
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue(spoken!!.contains("انروسین"))
    }

    @Test
    fun testVoiceIntentParser_FlockBreedsList_ExactUserAudioQuery() {
        val farmContext = FarmLiveContext(flocks = mockFlocks, contacts = mockContacts)
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "گله ۱ چه نژادهایی دارد",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue("Card should be valid", card.isValid)
        assertTrue("Title should mention breeds of flock 1", card.title.contains("نژادهای"))
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue("Spoken should mention breed Leghorn", spoken!!.contains("لگهورن"))
        assertTrue("Spoken should mention flock 1 name", spoken.contains("گله تخمگذار ۱"))
    }

    @Test
    fun testVoiceIntentParser_FlockBreedsList_Colloquial() {
        val farmContext = FarmLiveContext(flocks = mockFlocks, contacts = mockContacts)
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "نژادهای سالن یک چیه",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue(card.isValid)
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue(spoken!!.contains("لگهورن"))
    }

    @Test
    fun testVoiceIntentParser_FlockBreedsList_MultiBreedSubgroups() {
        val mixedFlock = Flock(
            id = 10L,
            name = "سالن ۱ ترکیبی",
            species = BirdSpecies.CHICKEN,
            breed = "آمروک",
            birdCount = 80,
            birthEpochMillis = System.currentTimeMillis() - (15L * 7 * 24 * 3600 * 1000),
            subGroups = listOf(
                FlockSubGroup(
                    species = BirdSpecies.CHICKEN,
                    breed = "آمروک",
                    birdCount = 50,
                    ageDays = 105
                ),
                FlockSubGroup(
                    species = BirdSpecies.CHICKEN,
                    breed = "مرندی",
                    birdCount = 30,
                    ageDays = 105
                )
            )
        )
        val farmContext = FarmLiveContext(flocks = listOf(mixedFlock), contacts = mockContacts)
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "گله ۱ چه نژادهایی دارد",
            flocks = listOf(mixedFlock),
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue(card.isValid)
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue("Should mention Amrock", spoken!!.contains("آمروک"))
        assertTrue("Should mention Marandi", spoken.contains("مرندی"))
        assertEquals(2, card.parameters["breedCount"])
        assertEquals(80, card.parameters["totalBirds"])
    }

    @Test
    fun testVoiceIntentParser_BreedAbsence_NegativeConfirmation() {
        // Flock 1 only has Leghorn. User asks: «آیا در گله یک نژاد راس داریم؟»
        val farmContext = FarmLiveContext(flocks = mockFlocks, contacts = mockContacts)
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "آیا در گله یک نژاد راس داریم؟",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue(card.isValid)
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue("Spoken response must explicitly start with negative confirmation 'خیر'", spoken!!.contains("خیر"))
        assertTrue("Spoken response must explicitly say 'وجود ندارد'", spoken.contains("وجود ندارد"))
        assertTrue("Should mention active breed Leghorn", spoken.contains("لگهورن"))
        assertEquals("Count of breed Ross in flock 1 must be 0", 0, card.parameters["count"])
    }

    @Test
    fun testVoiceIntentParser_BreedPresence_PositiveConfirmation() {
        // Flock 2 has 2000 Ross 308. User asks: «در گله دو چند تا راس داریم؟»
        val farmContext = FarmLiveContext(flocks = mockFlocks, contacts = mockContacts)
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "در گله دو چند تا راس داریم؟",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue(card.isValid)
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue("Should confirm positively", spoken!!.contains("بله") || spoken.contains("۲۰۰۰"))
        assertEquals(2000, card.parameters["count"])
    }

    @Test
    fun testVoiceIntentParser_FlockAgeQuery() {
        val farmContext = FarmLiveContext(flocks = mockFlocks, contacts = mockContacts)

        val card1 = VoiceIntentParser.parseVoiceCommand(
            spokenText = "سن گله یک چقدره",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext
        )
        assertEquals(VoiceIntentType.QUERY_STATS, card1.intentType)
        assertTrue(card1.isValid)
        assertTrue(card1.title.contains("سن"))
        val spoken1 = card1.parameters["spokenSummary"] as? String
        assertNotNull(spoken1)
        assertTrue(spoken1!!.contains("هفته") && spoken1.contains("روز"))

        val card2 = VoiceIntentParser.parseVoiceCommand(
            spokenText = "گله ۲ چند وقتشه",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext
        )
        assertEquals(VoiceIntentType.QUERY_STATS, card2.intentType)
        assertTrue(card2.isValid)
        val spoken2 = card2.parameters["spokenSummary"] as? String
        assertNotNull(spoken2)
        assertTrue(spoken2!!.contains("هفته"))
    }

    @Test
    fun testVoiceIntentParser_AllFarmBreeds() {
        val farmContext = FarmLiveContext(flocks = mockFlocks, contacts = mockContacts)
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "چه نژادهایی در فارم داریم",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue(card.isValid)
        assertTrue(card.title.contains("سرشماری"))
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue(spoken!!.contains("لگهورن"))
        assertTrue(spoken.contains("راس"))
        assertTrue(spoken.contains("گلپایگانی"))
    }

    @Test
    fun testVoiceIntentParser_WarehouseInventoryList() {
        val warehouseItems = listOf(
            WarehouseItem(
                id = 1L,
                name = "پلت تخمگذار",
                category = WarehouseCategory.FEED,
                currentStock = 500.0,
                unit = "کیلوگرم",
                pricePerUnit = 22000.0
            ),
            WarehouseItem(
                id = 2L,
                name = "مولتی ویتامین الکترولیت",
                category = WarehouseCategory.VITAMIN,
                currentStock = 10.0,
                unit = "لیتر",
                pricePerUnit = 150000.0
            )
        )
        val farmContext = FarmLiveContext(flocks = mockFlocks, warehouseItems = warehouseItems)
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "چه اقلامی در انبار داریم",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue(card.isValid)
        assertTrue(card.title.contains("انبار"))
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue(spoken!!.contains("پلت تخمگذار"))
        assertEquals(2, card.parameters["itemCount"])
    }

    @Test
    fun testVoiceIntentParser_FlockDetails_ExactUserAudioQuery() {
        val farmContext = FarmLiveContext(flocks = mockFlocks, contacts = mockContacts)
        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = "جزئیات گله یک را بگو",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext
        )

        assertEquals(VoiceIntentType.QUERY_STATS, card.intentType)
        assertTrue("Card must be valid", card.isValid)
        assertTrue("Title should contain 'جزئیات' or 'شناسنامه'", card.title.contains("جزئیات") || card.title.contains("شناسنامه"))
        assertEquals("Flock ID must be 1", 1L, card.flockId)
        val spoken = card.parameters["spokenSummary"] as? String
        assertNotNull(spoken)
        assertTrue("Spoken should mention breed Leghorn", spoken!!.contains("لگهورن"))
        assertTrue("Spoken should mention population", spoken.contains("۱۰۰۰") || spoken.contains("1000"))
    }

    @Test
    fun testVoiceIntentParser_FlockDetails_VariousPhrasings() {
        val farmContext = FarmLiveContext(flocks = mockFlocks, contacts = mockContacts)

        val queries = listOf(
            "جزییات سالن ۲" to 2L,
            "مشخصات گله یک چیه" to 1L,
            "شناسنامه گله ۱" to 1L,
            "پرونده سالن یک" to 1L,
            "درباره گله یک بگو" to 1L,
            "گله ۱ رو توضیح بده" to 1L,
            "از سالن ۲ چه خبر" to 2L,
            "گله یک رو بگو" to 1L,
            "سالن ۱ رو نشون بده" to 1L
        )

        for ((query, expectedFlockId) in queries) {
            val card = VoiceIntentParser.parseVoiceCommand(
                spokenText = query,
                flocks = mockFlocks,
                contacts = mockContacts,
                farmContext = farmContext
            )

            assertEquals("Query '$query' should be QUERY_STATS", VoiceIntentType.QUERY_STATS, card.intentType)
            assertTrue("Query '$query' should be valid", card.isValid)
            assertEquals("Query '$query' should target flock $expectedFlockId", expectedFlockId, card.flockId)
            val spoken = card.parameters["spokenSummary"] as? String
            assertNotNull("Query '$query' should have spokenSummary", spoken)
            assertTrue("Spoken summary should contain flock details", spoken!!.contains("جزئیات") || spoken.contains("وضعیت") || spoken.contains("مشخصات"))
        }
    }

    @Test
    fun testCancellationBugFix_CostAndDisambiguation() {
        // ۱. بررسی عدم لغو اشتباهی عبارات حاوی کلمه «هزینه»
        assertFalse("200 هزارتومن هزینه نباید لغو تشخیص داده شود", VoiceIntentParser.isCancellation("200 هزارتومن هزینه"))
        assertFalse("هزینه 200 هزارتومن نباید لغو تشخیص داده شود", VoiceIntentParser.isCancellation("هزینه 200 هزارتومن"))
        assertFalse("۲۰۰ هزار تومان هزینه نباید لغو تشخیص داده شود", VoiceIntentParser.isCancellation("۲۰۰ هزار تومان هزینه"))

        // ۲. بررسی عدم لغو اشتباهی کلمات حاوی «نه» یا «رد» (شانه، دانه، کردیم)
        assertFalse("۲ شانه تخم مرغ نباید لغو تشخیص داده شود", VoiceIntentParser.isCancellation("۲ شانه تخم مرغ"))
        assertFalse("۳۰ دانه تخم مرغ نباید لغو تشخیص داده شود", VoiceIntentParser.isCancellation("۳۰ دانه تخم مرغ"))
        assertFalse("امروز پنجاه تا تخم جمع کردیم توی سالن ۲ نباید لغو شود", VoiceIntentParser.isCancellation("امروز پنجاه تا تخم جمع کردیم توی سالن ۲"))

        // ۳. بررسی تفکیک عدد ۹ (نه تا) از کلمه رد و لغو
        assertFalse("نه تا تلفات در سالن ۱ نباید لغو تشخیص داده شود", VoiceIntentParser.isCancellation("نه تا تلفات در سالن ۱"))
        assertFalse("نهصد تا تخم مرغ نباید لغو تشخیص داده شود", VoiceIntentParser.isCancellation("نهصد تا تخم مرغ"))

        // ۴. بررسی انصراف صریح (Explicit Dismiss)
        assertFalse("دستور هزینه نباید انصراف صریح باشد", VoiceIntentParser.isExplicitDismiss("200 هزارتومن هزینه"))
        assertFalse("دستور هزینه 200 هزارتومن نباید انصراف صریح باشد", VoiceIntentParser.isExplicitDismiss("هزینه 200 هزارتومن"))
        assertTrue("کلمه لغو باید انصراف صریح باشد", VoiceIntentParser.isExplicitDismiss("لغو"))
        assertTrue("کلمه کنسل باید انصراف صریح باشد", VoiceIntentParser.isExplicitDismiss("کنسل"))
        assertTrue("کلمه انصراف باید انصراف صریح باشد", VoiceIntentParser.isExplicitDismiss("انصراف"))
        assertTrue("عبارت لغوش کن باید انصراف صریح باشد", VoiceIntentParser.isExplicitDismiss("لغوش کن"))
        assertTrue("عبارت ببند باید انصراف صریح باشد", VoiceIntentParser.isExplicitDismiss("پنجره رو ببند"))

        // ۵. بررسی کلمات لغو در مرحله تایید نهایی
        assertTrue("کلمه لغو در مرحله تایید باید لغو تلقی شود", VoiceIntentParser.isCancellation("لغو"))
        assertTrue("کلمه نه به تنهایی باید لغو تلقی شود", VoiceIntentParser.isCancellation("نه"))
        assertTrue("کلمه خیر باید لغو تلقی شود", VoiceIntentParser.isCancellation("خیر"))
        assertTrue("کلمه کنسل باید لغو تلقی شود", VoiceIntentParser.isCancellation("کنسل"))
        assertTrue("عبارت ثبت نکن باید لغو تلقی شود", VoiceIntentParser.isCancellation("ثبت نکن"))

        // ۶. بررسی کلیدواژه‌های تایید و عدم تداخل با کلمات شماره و دوباره
        assertFalse("شماره ۲ نباید تایید تلقی شود", VoiceIntentParser.isConfirmation("شماره ۲"))
        assertFalse("دوباره نباید تایید تلقی شود", VoiceIntentParser.isConfirmation("دوباره"))
        assertTrue("تایید باید تایید تلقی شود", VoiceIntentParser.isConfirmation("تایید"))
        assertTrue("بله باید تایید تلقی شود", VoiceIntentParser.isConfirmation("بله"))
        assertTrue("اره باید تایید تلقی شود", VoiceIntentParser.isConfirmation("اره"))
        assertTrue("درسته باید تایید تلقی شود", VoiceIntentParser.isConfirmation("درسته"))
        assertTrue("حله باید تایید تلقی شود", VoiceIntentParser.isConfirmation("حله"))

        // ۷. تفسیر صحیح مالی دستور 200 هزارتومن هزینه
        val card1 = VoiceIntentParser.parseVoiceCommand("200 هزارتومن هزینه", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, card1.intentType)
        assertTrue("باید دستور معتبر باشد", card1.isValid)
        assertEquals(200000.0, (card1.parameters["amountTomans"] as? Number)?.toDouble() ?: 0.0, 0.01)
        assertEquals(TransactionType.EXPENSE, card1.parameters["type"])

        val card2 = VoiceIntentParser.parseVoiceCommand("هزینه 200 هزارتومن", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, card2.intentType)
        assertTrue("باید دستور معتبر باشد", card2.isValid)
        assertEquals(200000.0, (card2.parameters["amountTomans"] as? Number)?.toDouble() ?: 0.0, 0.01)
        assertEquals(TransactionType.EXPENSE, card2.parameters["type"])

        // ۸. تفسیر صحیح عدد ۹ به عنوان تلفات
        val cardMort = VoiceIntentParser.parseVoiceCommand("نه تا تلفات در سالن ۱", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_MORTALITY, cardMort.intentType)
        assertTrue("باید دستور معتبر باشد", cardMort.isValid)
        assertEquals(9, (cardMort.parameters["count"] as? Number)?.toInt())
    }

    @Test
    fun testWorkerMode_ForbiddenCommands() {
        val workerRole = UserRole.WORKER

        // ۱. ثبت هزینه در حالت کارگر باید غیرمجاز باشد
        val costCard1 = VoiceIntentParser.parseVoiceCommand("200 هزارتومن هزینه", mockFlocks, mockContacts, userRole = workerRole)
        assertFalse("در حالت کارگر ثبت هزینه باید نامعتبر باشد", costCard1.isValid)
        assertTrue("عنوان کارت باید حاوی دسترسی غیر مجاز باشد", costCard1.title.contains("دسترسی غیر مجاز"))
        assertEquals("دسترسی غیر مجاز", costCard1.primaryMetric)
        assertTrue("پیام خطا باید دسترسی غیر مجاز باشد", costCard1.errorMessage!!.contains("دسترسی غیر مجاز"))
        assertEquals(true, costCard1.parameters["unauthorized"])

        val costCard2 = VoiceIntentParser.parseVoiceCommand("هزینه 200 هزارتومن", mockFlocks, mockContacts, userRole = workerRole)
        assertFalse("در حالت کارگر ثبت هزینه باید نامعتبر باشد", costCard2.isValid)
        assertTrue("عنوان کارت باید حاوی دسترسی غیر مجاز باشد", costCard2.title.contains("دسترسی غیر مجاز"))

        val costCard3 = VoiceIntentParser.parseVoiceCommand("ثبت هزینه ۵۰۰ هزار تومان دان", mockFlocks, mockContacts, userRole = workerRole)
        assertFalse("ثبت هزینه دان برای کارگر باید نامعتبر باشد", costCard3.isValid)
        assertTrue(costCard3.title.contains("دسترسی غیر مجاز"))

        val incCard = VoiceIntentParser.parseVoiceCommand("یک میلیون تومان درآمد فروش تخم مرغ", mockFlocks, mockContacts, userRole = workerRole)
        assertFalse("ثبت درآمد برای کارگر باید نامعتبر باشد", incCard.isValid)
        assertTrue(incCard.title.contains("دسترسی غیر مجاز"))

        // ۲. ایجاد گله جدید در حالت کارگر باید غیرمجاز باشد
        val flockCard = VoiceIntentParser.parseVoiceCommand("یک گله آمروک اضافه کن", mockFlocks, mockContacts, userRole = workerRole)
        assertFalse("ایجاد گله برای کارگر باید غیرمجاز باشد", flockCard.isValid)
        assertTrue(flockCard.title.contains("دسترسی غیر مجاز"))

        // ۳. ناوبری به صفحات مالی و تنظیمات برای کارگر غیرمجاز است
        val navFinCard = VoiceIntentParser.parseVoiceCommand("برو به بخش مالی", mockFlocks, mockContacts, userRole = workerRole)
        assertFalse("ناوبری به بخش مالی برای کارگر غیرمجاز است", navFinCard.isValid)
        assertTrue(navFinCard.title.contains("دسترسی غیر مجاز"))

        val navSettingsCard = VoiceIntentParser.parseVoiceCommand("صفحه تنظیمات را باز کن", mockFlocks, mockContacts, userRole = workerRole)
        assertFalse("ناوبری به تنظیمات برای کارگر غیرمجاز است", navSettingsCard.isValid)
        assertTrue(navSettingsCard.title.contains("دسترسی غیر مجاز"))

        // ۴. استعلامات مالی در حالت کارگر باید غیرمجاز باشد
        val finQueryCard = VoiceIntentParser.parseVoiceCommand("سود این ماه چقدر است؟", mockFlocks, mockContacts, userRole = workerRole)
        assertFalse("استعلام سود برای کارگر باید غیرمجاز باشد", finQueryCard.isValid)
        assertTrue(finQueryCard.title.contains("دسترسی غیر مجاز"))
    }

    @Test
    fun testWorkerMode_PermittedRoutineCommands() {
        val workerRole = UserRole.WORKER
        val farmContext = FarmLiveContext(
            flocks = mockFlocks,
            contacts = mockContacts,
            warehouseItems = listOf(
                WarehouseItem(
                    id = 1L,
                    name = "دان آماده",
                    category = WarehouseCategory.FEED,
                    currentStock = 500.0,
                    unit = "کیلوگرم"
                )
            )
        )

        // ۱. ثبت تخم‌مرغ
        val eggCard = VoiceIntentParser.parseVoiceCommand("ثبت ۲۰۰ تخم مرغ برای گله ۱", mockFlocks, mockContacts, userRole = workerRole)
        assertTrue("ثبت تخم مرغ برای کارگر باید مجاز باشد", eggCard.isValid)
        assertEquals(VoiceIntentType.RECORD_EGG, eggCard.intentType)
        assertEquals(200, (eggCard.parameters["count"] as? Number)?.toInt())

        // ۲. ثبت تلفات
        val mortCard = VoiceIntentParser.parseVoiceCommand("ثبت ۳ تلفات در گله ۱", mockFlocks, mockContacts, userRole = workerRole)
        assertTrue("ثبت تلفات برای کارگر باید مجاز باشد", mortCard.isValid)
        assertEquals(VoiceIntentType.RECORD_MORTALITY, mortCard.intentType)
        assertEquals(3, (mortCard.parameters["count"] as? Number)?.toInt())

        // ۳. ثبت دان مصرفی
        val feedCard = VoiceIntentParser.parseVoiceCommand("برای سالن ۱ پنجاه کیلو دان بریز", mockFlocks, mockContacts, userRole = workerRole)
        assertTrue("ثبت دان برای کارگر باید مجاز باشد", feedCard.isValid)
        assertEquals(VoiceIntentType.RECORD_FEED, feedCard.intentType)

        // ۴. ثبت مصرف آب
        val waterCard = VoiceIntentParser.parseVoiceCommand("مصرف اب گله ۲ سیصد لیتر", mockFlocks, mockContacts, userRole = workerRole)
        assertTrue("ثبت آب برای کارگر باید مجاز باشد", waterCard.isValid)
        assertEquals(VoiceIntentType.RECORD_WATER, waterCard.intentType)

        // ۵. ثبت دارو / ویتامین
        val medCard = VoiceIntentParser.parseVoiceCommand("ثبت مصرف ویتامین در سالن ۱", mockFlocks, mockContacts, userRole = workerRole)
        assertTrue("ثبت دارو برای کارگر باید مجاز باشد", medCard.isValid)
        assertEquals(VoiceIntentType.RECORD_MEDICATION, medCard.intentType)

        // ۶. تشخیص بیماری
        val diagCard = VoiceIntentParser.parseVoiceCommand("طیورم خس خس سینه دارن و اسهال سفید، چه بیماری دارن؟", mockFlocks, mockContacts, userRole = workerRole)
        assertTrue("تشخیص بیماری برای کارگر باید مجاز باشد", diagCard.isValid)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, diagCard.intentType)

        // ۷. تماس با مخاطبین فارم (دامپزشک و غیره)
        val callCard = VoiceIntentParser.parseVoiceCommand("با دکتر دارابی تماس بگیر", mockFlocks, mockContacts, userRole = workerRole)
        assertTrue("تماس با دکتر برای کارگر باید مجاز باشد", callCard.isValid)
        assertEquals(VoiceIntentType.CALL_CONTACT, callCard.intentType)

        // ۸. استعلام موجودی انبار (اقلام، دان)
        val warehouseQuery = VoiceIntentParser.parseVoiceCommand(
            spokenText = "چقدر دان در انبار داریم؟",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext,
            userRole = workerRole
        )
        assertTrue("استعلام دان انبار برای کارگر باید مجاز باشد", warehouseQuery.isValid)
        assertEquals(VoiceIntentType.QUERY_STATS, warehouseQuery.intentType)

        // ۹. استعلام سن سالن
        val ageQuery = VoiceIntentParser.parseVoiceCommand(
            spokenText = "سن گله ۱ چقدره؟",
            flocks = mockFlocks,
            contacts = mockContacts,
            farmContext = farmContext,
            userRole = workerRole
        )
        assertTrue("استعلام سن برای کارگر باید مجاز باشد", ageQuery.isValid)
        assertEquals(VoiceIntentType.QUERY_STATS, ageQuery.intentType)

        // ۱۰. ناوبری به انبار فارم
        val navWhCard = VoiceIntentParser.parseVoiceCommand("برو به انبار", mockFlocks, mockContacts, userRole = workerRole)
        assertTrue("ناوبری به انبار برای کارگر باید مجاز باشد", navWhCard.isValid)
        assertEquals(VoiceIntentType.NAVIGATE_SCREEN, navWhCard.intentType)
    }

    @Test
    fun testFarmConversationalAiEngine_IndependentQuestionAnswering() {
        val flocksList = mockFlocks
        var session = AiSessionState()

        // نوبت اول: کاربر مشخصاً از سالن ۱ سوال می‌پرسد
        val turn1 = FarmConversationalAiEngine.processTurn(
            spokenText = "وضعیت سالن ۱ چطوره؟",
            currentState = session,
            flocks = flocksList,
            contacts = mockContacts
        )
        assertFalse("شیت نباید بسته شود", turn1.shouldDismissSheet)
        assertEquals(1L, turn1.actionCard?.flockId)
        session = turn1.updatedSessionState

        // نوبت دوم: کاربر یک سوال عمومی جدید می‌پرسد بدون ذکر نام سالن
        // پاسخ نباید به سالن ۱ مقید شود و باید به صورت مستقل پاسخ داده شود
        val turn2 = FarmConversationalAiEngine.processTurn(
            spokenText = "تلفات چقدر بود؟",
            currentState = session,
            flocks = flocksList,
            contacts = mockContacts
        )
        // پاسخ مستقل باید بدون تحمیل سالن ۱ تولید شود
        assertFalse("شیت نباید بسته شود", turn2.shouldDismissSheet)
        assertTrue("پاسخ باید مرتبط با تلفات باشد", turn2.aiResponseText.contains("تلفات"))
    }

    @Test
    fun testFarmConversationalAiEngine_ConfirmationAndCancellation() {
        val sampleCard = VoiceActionCardData(
            intentType = VoiceIntentType.RECORD_EGG,
            title = "ثبت جمع‌آوری تخم‌مرغ",
            iconRes = R.drawable.ic_farm_egg,
            flockId = 1L,
            flockName = "سالن ۱",
            primaryMetric = "۲۰۰ عدد",
            details = listOf("گله" to "سالن ۱", "تعداد" to "۲۰۰"),
            systemEffect = "ثبت در پایگاه داده",
            rawText = "۲۰۰ تا تخم مرغ برای سالن ۱",
            isValid = true,
            parameters = mapOf("flockId" to 1L, "count" to 200)
        )
        val sessionWithPending = AiSessionState(
            activeFlockId = 1L,
            activeFlockName = "سالن ۱",
            pendingActionCard = sampleCard
        )

        // سناریو تایید صوتی: «آره ثبتش کن»
        val confirmResult = FarmConversationalAiEngine.processTurn(
            spokenText = "آره ثبتش کن",
            currentState = sessionWithPending,
            flocks = mockFlocks,
            contacts = mockContacts
        )
        assertNotNull(confirmResult.actionCard)
        assertTrue("باید اعلام اعمال تغییرات کند", confirmResult.aiResponseText.contains("ثبت") || confirmResult.aiResponseText.contains("تغییرات"))

        // سناریو لغو صوتی: «نه لغوش کن»
        val cancelResult = FarmConversationalAiEngine.processTurn(
            spokenText = "نه لغوش کن",
            currentState = sessionWithPending,
            flocks = mockFlocks,
            contacts = mockContacts
        )
        assertNull(cancelResult.updatedSessionState.pendingActionCard)
        assertTrue("باید پیام لغو و سوال پیگیرانه اعلام کند", cancelResult.aiResponseText.contains("لغو شد"))
    }

    @Test
    fun testFarmConversationalAiEngine_PoultryAdvisoryQuestions() {
        val session = AiSessionState()

        DefaultAiQuestionAnswerBridge.setApiHandler { q, _ ->
            when {
                q.contains("دما") -> "دمای استاندارد دوره آغازین جوجه ۳۲ تا ۳۴ درجه سانتی‌گراد است."
                q.contains("استرس") -> "پروتکل مهار استرس گرمایی طیور با الکترولیت و هوادهی."
                q.contains("دان") -> "علت کاهش مصرف دان می‌تواند استرس یا کیفیت آب باشد."
                else -> "پاسخ مقایسه نژادهای راس و کاب گوشتی."
            }
        }

        try {
            // ۱. سوال دمای جوجه
            val tempTurn = FarmConversationalAiEngine.processTurn(
                spokenText = "دمای مطلوب سالن برای جوجه چقدره؟",
                currentState = session,
                flocks = mockFlocks,
                contacts = mockContacts
            )
            assertEquals("ONLINE_API_KNOWLEDGE", tempTurn.updatedSessionState.activeTopic)
            assertTrue("باید دمای ۳۲ تا ۳۴ درجه را ذکر کند", tempTurn.aiResponseText.contains("۳۲") || tempTurn.aiResponseText.contains("۳۴"))

            // ۲. سوال استرس گرمایی
            val heatTurn = FarmConversationalAiEngine.processTurn(
                spokenText = "راهکارهای کاهش استرس گرمایی طیور چیه؟",
                currentState = session,
                flocks = mockFlocks,
                contacts = mockContacts
            )
            assertEquals("ONLINE_API_KNOWLEDGE", heatTurn.updatedSessionState.activeTopic)
            assertTrue(heatTurn.aiResponseText.contains("استرس گرمایی"))

            // ۳. سوال کاهش مصرف دان
            val feedTurn = FarmConversationalAiEngine.processTurn(
                spokenText = "چرا مرغ‌ها دان نمی‌خورند؟",
                currentState = session,
                flocks = mockFlocks,
                contacts = mockContacts
            )
            assertEquals("ONLINE_API_KNOWLEDGE", feedTurn.updatedSessionState.activeTopic)
            assertTrue(feedTurn.aiResponseText.contains("کاهش مصرف دان") || feedTurn.aiResponseText.contains("آب"))

            // ۴. مقایسه نژادها
            val breedTurn = FarmConversationalAiEngine.processTurn(
                spokenText = "تفاوت نژاد راس و کاب چیه؟",
                currentState = session,
                flocks = mockFlocks,
                contacts = mockContacts
            )
            assertEquals("ONLINE_API_KNOWLEDGE", breedTurn.updatedSessionState.activeTopic)
            assertTrue(breedTurn.aiResponseText.contains("راس") || breedTurn.aiResponseText.contains("کاب"))
        } finally {
            DefaultAiQuestionAnswerBridge.resetToDefault()
        }
    }

    @Test
    fun testFarmConversationalAiEngine_FarewellIntent() {
        val session = AiSessionState()
        val farewellTurn = FarmConversationalAiEngine.processTurn(
            spokenText = "خیلی ممنون خداحافظ",
            currentState = session,
            flocks = mockFlocks,
            contacts = mockContacts
        )
        assertTrue("باید درخواست بستن شیت ارسال شود", farewellTurn.shouldDismissSheet)
        assertFalse("نباید پس از خداحافظی دوباره گوش دهد", farewellTurn.autoListenNextTurn)
        assertTrue("باید پاسخ خداحافظی محترمانه بدهد", farewellTurn.aiResponseText.contains("خداحافظ") || farewellTurn.aiResponseText.contains("خدانگهدار") || farewellTurn.aiResponseText.contains("خسته نباشید"))
    }

    @Test
    fun testFarmConversationalAiEngine_WorkerModeProtection() {
        val session = AiSessionState()
        val workerRole = UserRole.WORKER

        // کارگر سعی می‌کند هزینه ثبت کند
        val expenseTurn = FarmConversationalAiEngine.processTurn(
            spokenText = "۲۰۰ هزار تومن هزینه ثبت کن",
            currentState = session,
            flocks = mockFlocks,
            contacts = mockContacts,
            userRole = workerRole
        )
        assertTrue("پاسخ باید حاکی از عدم دسترسی کارگر باشد", expenseTurn.aiResponseText.contains("دسترسی غیر مجاز") || expenseTurn.actionCard?.isValid == false)

        // کارگر رکورد روتین سالن ثبت می‌کند: مجاز
        val eggTurn = FarmConversationalAiEngine.processTurn(
            spokenText = "۲۰۰ عدد تخم مرغ برای سالن ۱ ثبت کن",
            currentState = session,
            flocks = mockFlocks,
            contacts = mockContacts,
            userRole = workerRole
        )
        assertTrue(eggTurn.actionCard?.isValid == true)
        assertEquals(VoiceIntentType.RECORD_EGG, eggTurn.actionCard?.intentType)
    }

    @Test
    fun testHistoricalRootCause_WaterDeprivationEggDrop() {
        val testFlock = mockFlocks.first()
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        // 100kg feed charges
        val feedCharges = listOf(
            FeedWaterCharge(1L, testFlock.id, ChargeItemType.FEED, 100.0, now - (oneDay * 1), "1405/02/10"),
            FeedWaterCharge(2L, testFlock.id, ChargeItemType.FEED, 100.0, now - (oneDay * 2), "1405/02/09"),
            FeedWaterCharge(3L, testFlock.id, ChargeItemType.FEED, 100.0, now - (oneDay * 3), "1405/02/08")
        )
        // 110L water charges (Ratio = 1.1 < 1.4 -> Water deprivation!)
        val waterCharges = listOf(
            FeedWaterCharge(4L, testFlock.id, ChargeItemType.WATER, 110.0, now - (oneDay * 1), "1405/02/10"),
            FeedWaterCharge(5L, testFlock.id, ChargeItemType.WATER, 110.0, now - (oneDay * 2), "1405/02/09"),
            FeedWaterCharge(6L, testFlock.id, ChargeItemType.WATER, 110.0, now - (oneDay * 3), "1405/02/08")
        )

        val context = FarmLiveContext(
            flocks = listOf(testFlock),
            allFarmCharges = feedCharges + waterCharges
        )
    }

    @Test
    fun testLocalPoultryAiAgent_TurnProcessing() {
        val testFlock = mockFlocks.first()
        val context = FarmLiveContext(flocks = mockFlocks, contacts = mockContacts)
        val session = AiSessionState(activeFlockId = testFlock.id, activeFlockName = testFlock.name)

        // ۱. استعلام علمی هوش مصنوعی توسط ایجنت
        val agentTurnBreed = LocalPoultryAiAgent.processAgentTurn(
            spokenText = "اطلاعات استاندارد نژاد کاب ۵۰۰",
            currentState = session,
            farmContext = context
        )
        assertEquals(AgentExecutionMode.AI_API_KNOWLEDGE, agentTurnBreed.executionMode)

        // ۲. استعلام آماری فارم
        val agentTurnStats = LocalPoultryAiAgent.processAgentTurn(
            spokenText = "سن گله ۱ چقدره؟",
            currentState = session,
            farmContext = context
        )
        assertEquals(AgentExecutionMode.FARM_OMNISCIENCE, agentTurnStats.executionMode)
    }

    @Test
    fun testCancellationAndConfirmationDisambiguation() {
        // ۱. نباید به اشتباه لغو شناسایی شوند
        assertFalse("هزینه نباید لغو باشد", VoiceIntentParser.isCancellation("200 هزارتومن هزینه"))
        assertFalse("هزینه نباید لغو باشد", VoiceIntentParser.isCancellation("هزینه 200 هزارتومن"))
        assertFalse("شانه نباید لغو باشد", VoiceIntentParser.isCancellation("۲ شانه تخم مرغ"))
        assertFalse("دانه نباید لغو باشد", VoiceIntentParser.isCancellation("۳۰ دانه تخم مرغ"))
        assertFalse("نه تا نباید لغو باشد", VoiceIntentParser.isCancellation("نه تا تلفات ثبت کن"))
        assertFalse("نهصد نباید لغو باشد", VoiceIntentParser.isCancellation("نهصد کیلو دان"))
        assertFalse("کردیم نباید لغو باشد", VoiceIntentParser.isCancellation("تلفات ثبت کردیم"))

        // ۲. کلمات صریح لغو باید لغو شناخته شوند
        assertTrue("لغو باید لغو شناخته شود", VoiceIntentParser.isCancellation("لغو"))
        assertTrue("کنسل باید لغو شناخته شود", VoiceIntentParser.isCancellation("کنسل"))
        assertTrue("انصراف باید لغو شناخته شود", VoiceIntentParser.isCancellation("انصراف بده"))
        assertTrue("نه باید لغو شناخته شود", VoiceIntentParser.isCancellation("نه"))
        assertTrue("خیر باید لغو شناخته شود", VoiceIntentParser.isCancellation("خیر"))
        assertTrue("رد باید لغو شناخته شود", VoiceIntentParser.isCancellation("رد کن"))

        // ۳. کلمات شبیه تایید نباید به اشتباه تایید شناخته شوند
        assertFalse("شماره نباید تایید باشد", VoiceIntentParser.isConfirmation("شماره تلفن"))
        assertFalse("دوباره نباید تایید باشد", VoiceIntentParser.isConfirmation("دوباره بگو"))

        // ۴. تاییدهای صریح
        assertTrue("بله باید تایید باشد", VoiceIntentParser.isConfirmation("بله"))
        assertTrue("تایید باید تایید باشد", VoiceIntentParser.isConfirmation("تایید کن"))
        assertTrue("اره باید تایید باشد", VoiceIntentParser.isConfirmation("اره"))
        assertTrue("درسته باید تایید باشد", VoiceIntentParser.isConfirmation("درسته"))
    }

    @Test
    fun testExpenseVoiceCommandsParsing() {
        val card1 = VoiceIntentParser.parseVoiceCommand("200 هزارتومن هزینه", mockFlocks, mockContacts, userRole = UserRole.MANAGER)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, card1.intentType)
        assertTrue(card1.isValid)
        assertEquals(200000.0, (card1.parameters["amount"] as? Number)?.toDouble() ?: 0.0, 0.01)
        assertEquals(TransactionType.EXPENSE, card1.parameters["transactionType"])

        val card2 = VoiceIntentParser.parseVoiceCommand("هزینه 200 هزارتومن", mockFlocks, mockContacts, userRole = UserRole.MANAGER)
        assertEquals(VoiceIntentType.RECORD_FINANCIAL, card2.intentType)
        assertTrue(card2.isValid)
        assertEquals(200000.0, (card2.parameters["amount"] as? Number)?.toDouble() ?: 0.0, 0.01)
        assertEquals(TransactionType.EXPENSE, card2.parameters["transactionType"])
    }

    @Test
    fun testWorkerRoleSecurityRestrictions() {
        // ۱. ثبت هزینه در حالت کارگر باید غیرمجاز باشد
        val expenseCard = VoiceIntentParser.parseVoiceCommand("200 هزارتومن هزینه", mockFlocks, mockContacts, userRole = UserRole.WORKER)
        assertFalse("کارگر نباید بتواند هزینه ثبت کند", expenseCard.isValid)
        assertEquals("دسترسی غیر مجاز", expenseCard.primaryMetric)
        assertEquals(true, expenseCard.parameters["unauthorized"])

        val expenseCard2 = VoiceIntentParser.parseVoiceCommand("هزینه 200 هزارتومن", mockFlocks, mockContacts, userRole = UserRole.WORKER)
        assertFalse(expenseCard2.isValid)
        assertEquals("دسترسی غیر مجاز", expenseCard2.primaryMetric)

        // ۲. ایجاد گله در حالت کارگر باید غیرمجاز باشد
        val createFlockCard = VoiceIntentParser.parseVoiceCommand("ایجاد گله جدید سالن ۴", mockFlocks, mockContacts, userRole = UserRole.WORKER)
        assertFalse("کارگر نباید بتواند گله بسازد", createFlockCard.isValid)
        assertEquals("دسترسی غیر مجاز", createFlockCard.primaryMetric)

        // ۳. رفتن به بخش مالی یا تنظیمات برای کارگر غیرمجاز است
        val navCard = VoiceIntentParser.parseVoiceCommand("برو به بخش مالی", mockFlocks, mockContacts, userRole = UserRole.WORKER)
        assertFalse("ناوبری مالی برای کارگر غیرمجاز است", navCard.isValid)
        assertEquals("دسترسی غیر مجاز", navCard.primaryMetric)

        // ۴. استعلام مالی (سود و زیان فارم) برای کارگر غیرمجاز است
        val finQueryCard = VoiceIntentParser.parseVoiceCommand("سود و زیان فارم چقدره؟", mockFlocks, mockContacts, userRole = UserRole.WORKER)
        assertFalse("استعلام مالی برای کارگر غیرمجاز است", finQueryCard.isValid)
        assertEquals("دسترسی غیر مجاز", finQueryCard.primaryMetric)

        // ۵. عملیات مجاز برای کارگر
        val eggCard = VoiceIntentParser.parseVoiceCommand("ثبت ۲۰۰ تخم مرغ برای گله ۱", mockFlocks, mockContacts, userRole = UserRole.WORKER)
        assertTrue("ثبت تخم‌مرغ برای کارگر مجاز است", eggCard.isValid)
        assertEquals(VoiceIntentType.RECORD_EGG, eggCard.intentType)

        val mortCard = VoiceIntentParser.parseVoiceCommand("۵ تا تلفات در سالن ۲", mockFlocks, mockContacts, userRole = UserRole.WORKER)
        assertTrue("ثبت تلفات برای کارگر مجاز است", mortCard.isValid)
        assertEquals(VoiceIntentType.RECORD_MORTALITY, mortCard.intentType)

        val feedCard = VoiceIntentParser.parseVoiceCommand("۵۰ کیلو دان به سالن ۲ دادیم", mockFlocks, mockContacts, userRole = UserRole.WORKER)
        assertTrue("ثبت دان برای کارگر مجاز است", feedCard.isValid)
        assertEquals(VoiceIntentType.RECORD_FEED, feedCard.intentType)

        val diagCard = VoiceIntentParser.parseVoiceCommand("مرغ‌ها بی‌حال شدن و اسهال سفید دارن", mockFlocks, mockContacts, userRole = UserRole.WORKER)
        assertTrue("تشخیص بیماری برای کارگر مجاز است", diagCard.isValid)
        assertEquals(VoiceIntentType.DIAGNOSE_DISEASE, diagCard.intentType)
    }

    @Test
    fun testSuperVoiceIntelligence_CompoundMultiActionBatchExecution() {
        val command = "امروز سالن ۱: ۱۲۰ تا تخم‌مرغ جمع کردیم، ۴۰ کیلو دان دادیم و ۲ تا هم تلفات داشتیم"
        val card = VoiceIntentParser.parseVoiceCommand(command, mockFlocks, mockContacts)

        assertEquals(VoiceIntentType.COMPOUND_ACTION, card.intentType)
        assertTrue(card.isValid)
        @Suppress("UNCHECKED_CAST")
        val subActions = card.parameters["subActions"] as? List<VoiceActionCardData>
        assertNotNull(subActions)
        assertEquals(3, subActions!!.size)

        val eggAction = subActions.find { it.intentType == VoiceIntentType.RECORD_EGG }
        assertNotNull(eggAction)
        assertEquals(1L, eggAction!!.flockId)
        assertEquals(120, (eggAction.parameters["count"] as? Number)?.toInt())

        val feedAction = subActions.find { it.intentType == VoiceIntentType.RECORD_FEED }
        assertNotNull(feedAction)
        assertEquals(1L, feedAction!!.flockId)
        assertEquals(40.0, (feedAction.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0, 0.01)

        val mortAction = subActions.find { it.intentType == VoiceIntentType.RECORD_MORTALITY }
        assertNotNull(mortAction)
        assertEquals(1L, mortAction!!.flockId)
        assertEquals(2, (mortAction.parameters["count"] as? Number)?.toInt())
    }

    @Test
    fun testSuperVoiceIntelligence_TemporalAndRelativeDates() {
        // ۱. دیروز
        val cardYesterday = VoiceIntentParser.parseVoiceCommand("دیروز ۵۰ تا تخم مرغ سالن ۲", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_EGG, cardYesterday.intentType)
        assertTrue(cardYesterday.isValid)
        assertEquals(2L, cardYesterday.flockId)
        assertEquals(50, (cardYesterday.parameters["count"] as? Number)?.toInt())
        val dateStr1 = cardYesterday.parameters["dateJalaliStr"] as? String
        assertNotNull(dateStr1)
        val epoch1 = (cardYesterday.parameters["epochMillis"] as? Number)?.toLong() ?: 0L
        val diffHours1 = (System.currentTimeMillis() - epoch1) / (1000 * 3600)
        assertTrue("دیروز باید بین ۲۰ الی ۲۸ ساعت قبل باشد", diffHours1 in 20..28)

        // ۲. پریروز
        val cardDayBefore = VoiceIntentParser.parseVoiceCommand("پریروز ۳ تا تلفات سالن ۱", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_MORTALITY, cardDayBefore.intentType)
        assertEquals(1L, cardDayBefore.flockId)
        assertEquals(3, (cardDayBefore.parameters["count"] as? Number)?.toInt())
        val epoch2 = (cardDayBefore.parameters["epochMillis"] as? Number)?.toLong() ?: 0L
        val diffHours2 = (System.currentTimeMillis() - epoch2) / (1000 * 3600)
        assertTrue("پریروز باید بین ۴۴ الی ۵۲ ساعت قبل باشد", diffHours2 in 44..52)

        // ۳. سه روز پیش
        val card3Days = VoiceIntentParser.parseVoiceCommand("سه روز پیش ۲۰۰ کیلو دان دادیم سالن ۲", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FEED, card3Days.intentType)
        assertEquals(2L, card3Days.flockId)
        assertEquals(200.0, (card3Days.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0, 0.01)

        // ۴. نوبت صبح و عصر
        val cardShift = VoiceIntentParser.parseVoiceCommand("نوبت صبح ۷۰ تا تخم مرغ سالن ۱", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_EGG, cardShift.intentType)
        assertEquals("صبح", cardShift.parameters["shift"])
    }

    @Test
    fun testSuperVoiceIntelligence_InteractiveClarificationDialogue() {
        // سناریو ۱: تخم‌مرغ ناقص -> سوال تکمیلی -> پاسخ کاربر
        val session1 = AiSessionState()
        val turn1 = FarmConversationalAiEngine.processTurn(
            spokenText = "تخم‌مرغ ثبت کن",
            currentState = session1,
            flocks = mockFlocks,
            contacts = mockContacts
        )
        assertNotNull(turn1.actionCard)
        assertTrue("باید نیاز به اطلاعات تکمیلی داشته باشد", turn1.actionCard!!.needsMoreInfo)
        assertTrue(turn1.aiResponseText.contains("سالن") || turn1.aiResponseText.contains("تعداد"))

        // نوبت دوم: پاسخ کاربر با مشخص کردن سالن و تعداد
        val turn2 = FarmConversationalAiEngine.processTurn(
            spokenText = "سالن ۱، صد و بیست تا",
            currentState = turn1.updatedSessionState,
            flocks = mockFlocks,
            contacts = mockContacts
        )
        assertNotNull(turn2.actionCard)
        assertFalse("کارت نهایی نباید نیاز به اطلاعات بیشتر داشته باشد", turn2.actionCard!!.needsMoreInfo)
        assertEquals(VoiceIntentType.RECORD_EGG, turn2.actionCard!!.intentType)
        assertEquals(1L, turn2.actionCard!!.flockId)
        assertEquals(120, (turn2.actionCard!!.parameters["count"] as? Number)?.toInt())

        // سناریو ۲: تلفات ناقص -> سوال تکمیلی -> پاسخ کاربر
        val turnMort1 = FarmConversationalAiEngine.processTurn(
            spokenText = "امروز تلفات داشتیم",
            currentState = session1,
            flocks = mockFlocks,
            contacts = mockContacts
        )
        assertTrue(turnMort1.actionCard!!.needsMoreInfo)

        val turnMort2 = FarmConversationalAiEngine.processTurn(
            spokenText = "سالن ۲، پنج تا",
            currentState = turnMort1.updatedSessionState,
            flocks = mockFlocks,
            contacts = mockContacts
        )
        assertEquals(VoiceIntentType.RECORD_MORTALITY, turnMort2.actionCard!!.intentType)
        assertEquals(2L, turnMort2.actionCard!!.flockId)
        assertEquals(5, (turnMort2.actionCard!!.parameters["count"] as? Number)?.toInt())
    }

    @Test
    fun testSuperVoiceIntelligence_SetScheduleVoiceCommands() {
        val scheduleCard1 = VoiceIntentParser.parseVoiceCommand("برای سالن ۱ فردا ساعت ۹ صبح یادآور واکسن نیوکاسل بذار", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.SET_SCHEDULE, scheduleCard1.intentType)
        assertTrue(scheduleCard1.isValid)
        assertEquals(1L, scheduleCard1.flockId)
        assertEquals("VACCINE", scheduleCard1.parameters["category"])
        assertEquals(9, scheduleCard1.parameters["hour"])
        assertEquals(0, scheduleCard1.parameters["minute"])

        val scheduleCard2 = VoiceIntentParser.parseVoiceCommand("ساعت ۸ صبح یادآور بررسی دانخوریها بذار", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.SET_SCHEDULE, scheduleCard2.intentType)
        assertTrue(scheduleCard2.isValid)
        assertEquals(8, scheduleCard2.parameters["hour"])
        assertEquals(0, scheduleCard2.parameters["minute"])
    }

    @Test
    fun testSuperVoiceIntelligence_RecordObservationVoiceCommands() {
        val obsCard1 = VoiceIntentParser.parseVoiceCommand(
            "یادداشت سالن ۱: رطوبت بستر بالاست و پد سلولزی نیاز به سرویس دارد",
            mockFlocks,
            mockContacts,
            userRole = UserRole.WORKER
        )
        assertEquals(VoiceIntentType.RECORD_OBSERVATION, obsCard1.intentType)
        assertTrue(obsCard1.isValid)
        assertEquals(1L, obsCard1.flockId)
        assertTrue((obsCard1.parameters["note"] as? String)?.contains("رطوبت بستر") == true)

        val obsCard2 = VoiceIntentParser.parseVoiceCommand(
            "گزارش شیفت: تسمه فن شماره ۴ شل شده باید سفت شود",
            mockFlocks,
            mockContacts
        )
        assertEquals(VoiceIntentType.RECORD_OBSERVATION, obsCard2.intentType)
        assertTrue(obsCard2.isValid)
        assertEquals("SHIFT_REPORT", obsCard2.parameters["category"])
    }

    @Test
    fun testSuperVoiceIntelligence_CalculateInstantMetricsVoiceCommands() {
        // ۱. ضریب تبدیل
        val fcrCard = VoiceIntentParser.parseVoiceCommand("ضریب تبدیل سالن ۱ چنده؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.CALCULATE_METRIC, fcrCard.intentType)
        assertTrue(fcrCard.isValid)
        assertEquals(1L, fcrCard.flockId)
        assertEquals("FCR", fcrCard.parameters["metric"])

        // ۲. نسبت آب به دان
        val ratioCard = VoiceIntentParser.parseVoiceCommand("نسبت آب به دان سالن ۱ درسته؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.CALCULATE_METRIC, ratioCard.intentType)
        assertTrue(ratioCard.isValid)
        assertEquals("WATER_FEED_RATIO", ratioCard.parameters["metric"])

        // ۳. دوز داروی تایلوزین
        val doseCard = VoiceIntentParser.parseVoiceCommand("دوز تایلوزین برای ۲۰۰۰ مرغ چقدره؟", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.CALCULATE_METRIC, doseCard.intentType)
        assertTrue(doseCard.isValid)
        assertEquals("DRUG_DOSE", doseCard.parameters["metric"])
        assertEquals(2000, doseCard.parameters["birdCount"])
    }

    @Test
    fun testSuperVoiceIntelligence_ProactiveAnomalyAlerts() {
        // ۱. هشدار جهش تلفات (تلفات بالای ۱۰ قطعه)
        val mortCard = VoiceIntentParser.parseVoiceCommand("سالن ۱ پانزده تا تلفات داشتیم", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_MORTALITY, mortCard.intentType)
        assertNotNull(mortCard.parameters["anomalyAlert"])
        assertTrue((mortCard.parameters["anomalyAlert"] as String).contains("جهش"))

        // ۲. هشدار دوره پرهیز دارو
        val medCard = VoiceIntentParser.parseVoiceCommand("ثبت داروی تایلوزین برای سالن ۲", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_MEDICATION, medCard.intentType)
        assertNotNull(medCard.parameters["withdrawalAlert"])
        assertTrue((medCard.parameters["withdrawalAlert"] as String).contains("پرهیز"))
    }

    @Test
    fun testSuperVoiceIntelligence_LocalSlangUnitsAndFuzzyMatching() {
        // کیسه دان (هر کیسه = ۵۰ کیلوگرم)
        val feedBag1 = VoiceIntentParser.parseVoiceCommand("یه کیسه دان برای سالن ۲", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FEED, feedBag1.intentType)
        assertEquals(50.0, (feedBag1.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0, 0.01)

        // نیم کیسه دان (۲۵ کیلوگرم)
        val feedBagHalf = VoiceIntentParser.parseVoiceCommand("نیم کیسه دان سالن ۱", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_FEED, feedBagHalf.intentType)
        assertEquals(25.0, (feedBagHalf.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0, 0.01)

        // کارتن تخم‌مرغ (هر کارتن = ۱۸۰ عدد)
        val eggCarton = VoiceIntentParser.parseVoiceCommand("یه کارتن تخم مرغ سالن ۱", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_EGG, eggCarton.intentType)
        assertEquals(180, (eggCarton.parameters["count"] as? Number)?.toInt())

        // شانه تخم‌مرغ (هر شانه = ۳۰ عدد)
        val eggTray = VoiceIntentParser.parseVoiceCommand("دو شانه تخم مرغ سالن ۲", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_EGG, eggTray.intentType)
        assertEquals(60, (eggTray.parameters["count"] as? Number)?.toInt())

        // تطبیق فازی سالن ("سولن یک" به سالن ۱)
        val fuzzyFlock = VoiceIntentParser.parseVoiceCommand("سولن یک پنجاه تا تخم مرغ", mockFlocks, mockContacts)
        assertEquals(VoiceIntentType.RECORD_EGG, fuzzyFlock.intentType)
        assertEquals(1L, fuzzyFlock.flockId)
    }


    @Test
    fun testPersianVoiceNormalizer_DecimalAndWordIntegrity() {
        // ۱. نیمچه (پولِت) نباید به 0.5چه تبدیل شود
        val pulletText = PersianVoiceNormalizer.normalize("قیمت نیمچه گوشتی و نیمچه تخمگذار چقدر است")
        assertTrue("نیمچه باید دست نخورده باقی بماند", pulletText.contains("نیمچه گوشتی") && pulletText.contains("نیمچه تخمگذار"))
        assertFalse("نیمچه نباید به 0.5چه تبدیل شود", pulletText.contains("0.5چه"))

        // ۲. کلمه بدون نباید به بدان تبدیل شود
        val withoutBedding = PersianVoiceNormalizer.normalize("پرورش طیور بدون بستر در قفس")
        assertTrue("بدون باید حفظ شود", withoutBedding.contains("بدون بستر"))
        assertFalse("بدون نباید به بدان تبدیل شود", withoutBedding.contains("بدان بستر"))

        // ۳. کلمات اعشاری مرکب
        val compoundOneAndHalf = PersianVoiceNormalizer.normalize("یک و نیم کیلو دان مصرف شد")
        assertTrue("یک و نیم باید به 1.5 تبدیل شود", compoundOneAndHalf.contains("1.5 کیلو دان") || compoundOneAndHalf.contains("1.5"))

        val compoundTwoAndHalf = PersianVoiceNormalizer.normalize("دو و نیم لیتر مولتی ویتامین")
        assertTrue("دو و نیم باید به 2.5 تبدیل شود", compoundTwoAndHalf.contains("2.5 لیتر") || compoundTwoAndHalf.contains("2.5"))

        // ۴. تبدیل عامیانه دون مستقل به دان
        val feedBag = PersianVoiceNormalizer.normalize("دو کیسه دون دادیم")
        assertTrue("کیسه دون باید به کیسه دان تبدیل شود", feedBag.contains("کیسه دان") && feedBag.contains("دان دادیم"))
    }

    @Test
    fun testLocalPoultryAiAgent_SynchronizedSemanticIntents() {
        val dummyState = com.example.poultryapp.voice.AiSessionState(
            activeFlockId = null,
            activeFlockName = null,
            turns = emptyList()
        )
        val dummyContext = com.example.poultryapp.engine.FarmLiveContext(
            flocks = mockFlocks,
            contacts = mockContacts
        )

        val pathTurn = com.example.poultryapp.voice.LocalPoultryAiAgent.processAgentTurn(
            spokenText = "درمان شانکر و پنیرک کبوتر چیست",
            currentState = dummyState,
            farmContext = dummyContext
        )
        assertEquals(com.example.poultryapp.voice.AgentExecutionMode.AI_API_KNOWLEDGE, pathTurn.executionMode)

        val herbalTurn = com.example.poultryapp.voice.LocalPoultryAiAgent.processAgentTurn(
            spokenText = "خواص سرکه سیب در آب مرغداری",
            currentState = dummyState,
            farmContext = dummyContext
        )
        assertEquals(com.example.poultryapp.voice.AgentExecutionMode.AI_API_KNOWLEDGE, herbalTurn.executionMode)
    }

    @Test
    fun testDisambiguationOptionsWhenQueryIsAmbiguous() {
        val dummyState = AiSessionState()
        val dummyContext = FarmLiveContext(flocks = mockFlocks, contacts = mockContacts)

        // ۱. کاربر فقط می‌گوید «ثبت» بدون ذکر موضوع
        val recordTurn = FarmConversationalAiEngine.processTurn(
            spokenText = "ثبت",
            currentState = dummyState,
            farmContext = dummyContext
        )
        assertNotNull("کارت ابهام‌زدایی ثبت نباید نال باشد", recordTurn.actionCard)
        assertTrue("باید دارای گزینه‌های انتخابی باشد", recordTurn.actionCard?.quickReplies?.isNotEmpty() == true)
        assertTrue("گزینه‌ها باید شامل ثبت تخم‌مرغ باشد", recordTurn.actionCard?.quickReplies?.any { it.contains("تخم") } == true)
        assertTrue("گزینه‌ها باید شامل ثبت دان باشد", recordTurn.actionCard?.quickReplies?.any { it.contains("دان") } == true)

        // ۲. کاربر فقط می‌گوید «بررسی سالن»
        val hallTurn = FarmConversationalAiEngine.processTurn(
            spokenText = "بررسی سالن",
            currentState = dummyState,
            farmContext = dummyContext
        )
        assertNotNull("کارت ابهام‌زدایی سالن نباید نال باشد", hallTurn.actionCard)
        assertTrue("گزینه‌ها باید شامل بررسی دمای گله‌ها باشد", hallTurn.actionCard?.quickReplies?.any { it.contains("دما") } == true)

        // ۳. کاربر فقط می‌گوید «دان»
        val feedTurn = FarmConversationalAiEngine.processTurn(
            spokenText = "دان",
            currentState = dummyState,
            farmContext = dummyContext
        )
        assertNotNull("کارت ابهام‌زدایی دان نباید نال باشد", feedTurn.actionCard)
        assertTrue("گزینه‌ها باید شامل ثبت مصرف دان باشد", feedTurn.actionCard?.quickReplies?.any { it.contains("مصرف دان") } == true)
    }

    @Test
    fun testFlockTemperatureAdjustmentDoesNotTriggerBreedCatalog() {
        val blackStarFlock = Flock(
            id = 1L,
            name = "گله بلک استار",
            species = BirdSpecies.CHICKEN,
            breed = "بلک استار",
            birthEpochMillis = System.currentTimeMillis() - (150L * 24 * 3600 * 1000),
            birdCount = 1000
        )
        val dummyContext = FarmLiveContext(flocks = listOf(blackStarFlock), contacts = emptyList(), eggProductions = emptyList())
        val dummyState = AiSessionState(turns = emptyList())

        // ۱. روتینگ معنایی جمله «تنظیم دمای گله بلک استار»
        val routed = com.example.poultryapp.engine.FarmIntentSemanticRouter.routeIntent(
            norm = PersianVoiceNormalizer.normalize("تنظیم دمای گله بلک استار"),
            spokenText = "تنظیم دمای گله بلک استار",
            matchedFlock = blackStarFlock
        )
        assertEquals("باید به نیت OPERATIONAL_RECORD هدایت شود", com.example.poultryapp.engine.FarmSemanticIntent.OPERATIONAL_RECORD, routed)

        // ۲. پردازش نوبت «تنظیم دمای گله بلک استار» بدون عدد (ارائه چیپ‌های انتخابی استاندارد آسایش)
        val turnNoNum = FarmConversationalAiEngine.processTurn(
            spokenText = "تنظیم دمای گله بلک استار",
            currentState = dummyState,
            flocks = listOf(blackStarFlock),
            contacts = emptyList(),
            farmContext = dummyContext
        )
        assertNotNull(turnNoNum.actionCard)
        assertTrue("عنوان کارت باید تنظیم دمای سالن باشد", turnNoNum.actionCard!!.title.contains("تنظیم دمای سالن"))
        assertTrue("باید گزینه‌های انتخابی دما ارائه دهد", turnNoNum.actionCard!!.quickReplies.any { it.contains("درجه") })

        // ۳. پردازش نوبت «تنظیم دمای گله بلک استار روی ۲۲ درجه» (تولید کارت اقدام SET_FLOCK_TEMPERATURE)
        val turnWithNum = FarmConversationalAiEngine.processTurn(
            spokenText = "تنظیم دمای گله بلک استار روی ۲۲ درجه",
            currentState = dummyState,
            flocks = listOf(blackStarFlock),
            contacts = emptyList(),
            farmContext = dummyContext
        )
        assertNotNull(turnWithNum.actionCard)
        assertEquals("باید کارت از نوع SET_FLOCK_TEMPERATURE باشد", VoiceIntentType.SET_FLOCK_TEMPERATURE, turnWithNum.actionCard!!.intentType)
        assertEquals(22.0, turnWithNum.actionCard!!.parameters["tempCelsius"])
        assertTrue("کارت باید اقدام تغییردهنده وضعیت باشد تا تایید/انصراف بخواهد", turnWithNum.actionCard!!.isMutatingAction)
    }

    @Test
    fun testFlockFeedAndWaterTrackingCommands() {
        val flock1 = Flock(
            id = 1L,
            name = "گله ۱",
            species = BirdSpecies.CHICKEN,
            breed = "راس ۳۰۸",
            birthEpochMillis = System.currentTimeMillis() - (28L * 24 * 3600 * 1000),
            birdCount = 4950,
            notes = "سالن شماره یک",
            isFeedEnabled = false,
            isWaterEnabled = true
        )
        val dummyContext = FarmLiveContext(
            flocks = listOf(flock1),
            contacts = emptyList(),
            eggProductions = emptyList()
        )
        val dummyState = AiSessionState(turns = emptyList())

        // ۱. بررسی مفسر VoiceIntentParser: نباید نیت شارژ دان (نیازمند وزن) صادر شود
        val parsed = VoiceIntentParser.parseVoiceCommand(
            spokenText = "پایش دان گله ۱ فعال شود",
            flocks = listOf(flock1),
            contacts = emptyList(),
            farmContext = dummyContext
        )
        assertNotEquals("نباید نیت شارژ دان (RECORD_FEED) تشخیص داده شود", VoiceIntentType.RECORD_FEED, parsed.intentType)
        assertEquals("باید نیت TOGGLE_TRACKING تشخیص داده شود", VoiceIntentType.TOGGLE_TRACKING, parsed.intentType)
        assertEquals("FEED", parsed.parameters["trackingType"])
        assertEquals(true, parsed.parameters["isEnabled"])

        // ۲. بررسی روتر معنایی FarmIntentSemanticRouter
        val norm = PersianVoiceNormalizer.normalize("پایش دان گله ۱ فعال شود")
        val routed = com.example.poultryapp.engine.FarmIntentSemanticRouter.routeIntent(norm, "پایش دان گله ۱ فعال شود", flock1)
        assertEquals("باید به نیت OPERATIONAL_RECORD هدایت شود", com.example.poultryapp.engine.FarmSemanticIntent.OPERATIONAL_RECORD, routed)

        // ۳. پردازش فعال‌سازی در موتور مکالمه FarmConversationalAiEngine (وقتی دان غیرفعال است)
        val turnEnable = FarmConversationalAiEngine.processTurn(
            spokenText = "پایش دان گله ۱ فعال شود",
            currentState = dummyState,
            flocks = listOf(flock1),
            contacts = emptyList(),
            farmContext = dummyContext
        )
        assertNotNull(turnEnable.actionCard)
        assertEquals(VoiceIntentType.TOGGLE_TRACKING, turnEnable.actionCard!!.intentType)
        assertTrue("عنوان کارت باید فعال‌سازی پایش دان باشد", turnEnable.actionCard!!.title.contains("فعال‌سازی پایش دان"))
        assertTrue("باید به عنوان کارت تغییردهنده داده علامت‌گذاری شود", turnEnable.actionCard!!.isMutatingAction)
        assertTrue("متن هوش مصنوعی باید آماده فعال‌سازی پایش دان را اعلام کند", turnEnable.aiResponseText.contains("پایش هوشمند دان"))

        // ۴. پردازش غیرفعال‌سازی پایش آب (وقتی آب از قبل فعال است)
        val turnDisableWater = FarmConversationalAiEngine.processTurn(
            spokenText = "پایش آب گله ۱ غیرفعال شود",
            currentState = dummyState,
            flocks = listOf(flock1),
            contacts = emptyList(),
            farmContext = dummyContext
        )
        assertNotNull(turnDisableWater.actionCard)
        assertEquals(VoiceIntentType.TOGGLE_TRACKING, turnDisableWater.actionCard!!.intentType)
        assertEquals(false, turnDisableWater.actionCard!!.parameters["isEnabled"])
        assertEquals("WATER", turnDisableWater.actionCard!!.parameters["trackingType"])

        // ۵. استعلام وضعیت پایش سالن
        val turnQuery = FarmConversationalAiEngine.processTurn(
            spokenText = "وضعیت پایش گله ۱",
            currentState = dummyState,
            flocks = listOf(flock1),
            contacts = emptyList(),
            farmContext = dummyContext
        )
        assertNotNull(turnQuery.actionCard)
        assertTrue("پاسخ باید وضعیت پایش دان و آب را شرح دهد", turnQuery.aiResponseText.contains("پایش دان") && turnQuery.aiResponseText.contains("پایش آب"))
    }

    @Test
    fun testConversationalComprehension_CoreferenceAndColloquialUnits() {
        val flock1 = Flock(
            id = 1L,
            name = "گله ۱",
            breed = "بلک استار",
            birdCount = 1000,
            birthEpochMillis = System.currentTimeMillis() - (70L * 24 * 3600 * 1000),
            species = BirdSpecies.CHICKEN
        )
        val dummyContext = FarmLiveContext(flocks = listOf(flock1))

        // ۱. نوبت اول: کاربر نام گله را صریحاً می‌گوید
        val turn1 = FarmConversationalAiEngine.processTurn(
            spokenText = "وضعیت دمای گله ۱ چطوره؟",
            currentState = AiSessionState(),
            flocks = listOf(flock1),
            contacts = emptyList(),
            farmContext = dummyContext
        )
        assertEquals(1L, turn1.updatedSessionState.activeFlockId)
        assertEquals("گله ۱", turn1.updatedSessionState.activeFlockName)

        // ۲. نوبت دوم: کاربر با ضمیر بدون تکرار نام گله می‌پرسد (حل ضمیر و تداوم موضوع)
        val turn2 = FarmConversationalAiEngine.processTurn(
            spokenText = "تلفاتش چنده؟",
            currentState = turn1.updatedSessionState,
            flocks = listOf(flock1),
            contacts = emptyList(),
            farmContext = dummyContext
        )
        assertEquals("باید به گله ۱ پیوند بخورد", 1L, turn2.updatedSessionState.activeFlockId)

        // ۳. نوبت سوم: دستور شارژ دان با ضمیر «براش»
        val turn3 = FarmConversationalAiEngine.processTurn(
            spokenText = "براش ۵۰ کیلو دان بزن",
            currentState = turn2.updatedSessionState,
            flocks = listOf(flock1),
            contacts = emptyList(),
            farmContext = dummyContext
        )
        assertNotNull(turn3.actionCard)
        assertEquals(1L, turn3.actionCard!!.flockId)
        assertEquals("گله ۱", turn3.actionCard!!.flockName)

        // ۴. استخراج واحد عامیانه «نیم تن دان»
        val cardHalfTon = VoiceIntentParser.parseVoiceCommand("شارژ نیم تن دان برای گله ۱", listOf(flock1), emptyList())
        assertEquals(VoiceIntentType.RECORD_FEED, cardHalfTon.intentType)
        assertTrue("نیم تن باید ۵۰۰ کیلوگرم شناسایی شود", cardHalfTon.primaryMetric.contains("۵۰۰"))

        // ۵. استخراج واحد عامیانه «۲ کیسه دان»
        val cardBags = VoiceIntentParser.parseVoiceCommand("شارژ ۲ کیسه دان برای گله ۱", listOf(flock1), emptyList())
        assertEquals(VoiceIntentType.RECORD_FEED, cardBags.intentType)
        assertTrue("۲ کیسه باید ۱۰۰ کیلوگرم شناسایی شود", cardBags.primaryMetric.contains("۱۰۰"))
    }
}



