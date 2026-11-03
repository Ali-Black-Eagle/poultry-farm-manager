package com.example.poultryapp

import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.UserRole
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.voice.PersianVoiceNormalizer
import com.example.poultryapp.voice.PoultryPhoneticCorrector
import com.example.poultryapp.voice.VoiceActionCardData
import com.example.poultryapp.voice.VoiceIntentParser
import com.example.poultryapp.voice.VoiceIntentType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * سوئیت آزمون‌های چالش تخاصمی و استرس مایلستون ۲
 * (Milestone 2 Adversarial Challenge & Boundary Stress Test Suite)
 *
 * تمرکز بر ۳ چالش کلیدی و مرزهای حساس:
 * ۱. چالش آسیت در برابر اسید خوراکی/اسیدیفایر/اسیدوز
 * ۲. چالش حفاظت ۱۰۰٪ تاریخ‌های جلالی در برابر اسلش اعشاری
 * ۳. چالش تفکیک دقیق شماره سالن از مقادیر عددی دان (مانند «دویست») و سالن‌های چندرقمی (تا ۱۰۰)
 */
class Milestone2AdversarialChallengeTest {

    private lateinit var mockFlocks: List<Flock>

    @Before
    fun setUp() {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 3600 * 1000

        mockFlocks = (1..100).map { id ->
            Flock(
                id = id.toLong(),
                name = "سالن $id",
                species = if (id % 2 == 0) BirdSpecies.CHICKEN else BirdSpecies.TURKEY,
                breed = when {
                    id == 1 -> "بلک استار"
                    id == 2 -> "راس ۳۰۸"
                    id == 25 -> "آرین"
                    id == 98 -> "کاب ۵۰۰"
                    else -> "سویه بومی"
                },
                birthEpochMillis = now - (id * 2L) * dayMillis,
                birdCount = 1000 + id * 50,
                customTempCelsius = 22.0
            )
        }
    }

    // =========================================================================
    // چالش ۱: آسیت در برابر اسیدهای خوراکی و اسیدیفایر
    // =========================================================================

    @Test
    fun test01_AscitesAcousticCorrection_DirectReplacement() {
        // ۱. واژه با «ت» مستقیماً باید به آسیت تبدیل شود
        val input1 = "علائم بیماری اسیت"
        val corrected1 = PoultryPhoneticCorrector.correctSpeechErrors(input1)
        assertTrue("باید اسیت با ت به آسیت تبدیل شود: $corrected1", corrected1.contains("آسیت"))
        assertFalse("نباید کلمه اسیت باقی بماند: $corrected1", corrected1.contains("اسیت"))

        val input2 = "درمان بیماری اسیت مرغ"
        val corrected2 = PoultryPhoneticCorrector.correctSpeechErrors(input2)
        assertTrue("باید به آسیت تبدیل شود", corrected2.contains("آسیت"))

        val input3 = "تلفات ناشی از اسیت در گله"
        val corrected3 = PoultryPhoneticCorrector.correctSpeechErrors(input3)
        assertTrue("باید به آسیت تبدیل شود", corrected3.contains("آسیت"))
    }

    @Test
    fun test02_DietaryAcidsAndAcidifiers_NeverCorruptedToAscites() {
        // ۲. ترکیباتی که حاوی اسید خوراکی، اسیدیفایر یا اسیدوز هستند حتی در کنار علائم بالینی نباید تخریب شوند

        // الف) اسید ارگانیک
        val inputOrganic = "شکم باد کرده و اسید ارگانیک"
        val correctedOrganic = PoultryPhoneticCorrector.correctSpeechErrors(inputOrganic)
        assertTrue("باید اسید ارگانیک حفظ شود: $correctedOrganic", correctedOrganic.contains("اسید ارگانیک"))
        assertFalse("نباید به آسیت تبدیل شود: $correctedOrganic", correctedOrganic.contains("آسیت"))

        // ب) اسیدوز
        val inputAcidosis = "شکم باد کرده و اسیدوز در گله"
        val correctedAcidosis = PoultryPhoneticCorrector.correctSpeechErrors(inputAcidosis)
        assertTrue("باید اسیدوز حفظ شود: $correctedAcidosis", correctedAcidosis.contains("اسیدوز"))
        assertFalse("اسیدوز نباید تبدیل به آسیت شود: $correctedAcidosis", correctedAcidosis.contains("آسیت"))

        // ج) اسیدیفایر
        val inputAcidifier = "شکم باد کرده و اسیدیفایر در آبخوری"
        val correctedAcidifier = PoultryPhoneticCorrector.correctSpeechErrors(inputAcidifier)
        assertTrue("باید اسیدیفایر حفظ شود: $correctedAcidifier", correctedAcidifier.contains("اسیدیفایر"))
        assertFalse("اسیدیفایر نباید تبدیل به آسیت شود: $correctedAcidifier", correctedAcidifier.contains("آسیت"))

        // د) سایر اسیدهای خوراکی جیره (سیتریک، چرب، آمینه، بوتیریک، فرمیک، فسفریک)
        val inputCitric = "اب اورده شکم و مصرف اسید سیتریک"
        val correctedCitric = PoultryPhoneticCorrector.correctSpeechErrors(inputCitric)
        assertTrue("باید اسید سیتریک حفظ شود", correctedCitric.contains("اسید سیتریک"))
        assertFalse("نباید آسیت شود", correctedCitric.contains("آسیت"))

        val inputAmino = "کبودی لاشه و بالانس اسید آمینه و اسیدهای چرب"
        val correctedAmino = PoultryPhoneticCorrector.correctSpeechErrors(inputAmino)
        assertTrue("باید اسید آمینه حفظ شود", correctedAmino.contains("اسید آمینه"))
        assertFalse("نباید آسیت شود", correctedAmino.contains("آسیت"))
    }

    @Test
    fun test03_AscitesPathologyContext_GuardsAndTrigger() {
        // ۳. اگر واژه «اسید» بدون پسوند خوراکی در بافت بالینی آسیت بیاید، باید به آسیت تصحیح شود
        val inputTrueAscites = "شکم جوجه ها باد کرده و اب اورده و اسید گرفتن"
        val correctedTrue = PoultryPhoneticCorrector.correctSpeechErrors(inputTrueAscites)
        assertTrue("باید اسید ناشی از خطای شنیداری به آسیت تبدیل شود: $correctedTrue", correctedTrue.contains("آسیت"))

        // ۴. اگر واژه «اسید» بدون بافت بالینی بیاید، نباید دستکاری شود
        val inputPlainAcid = "قیمت یک لیتر اسید چنده؟"
        val correctedPlain = PoultryPhoneticCorrector.correctSpeechErrors(inputPlainAcid)
        assertFalse("بدون علائم بالینی نباید تبدیل به آسیت شود: $correctedPlain", correctedPlain.contains("آسیت"))
    }

    // =========================================================================
    // چالش ۲: پایداری تاریخ‌های جلالی و تفکیک از اسلش اعشاری
    // =========================================================================

    @Test
    fun test04_JalaliDates_Preserved100Percent() {
        // ۱. تاریخ‌های جلالی چهاررقمی و دورقمی
        val d1 = "1403/08/10"
        val norm1 = PersianVoiceNormalizer.normalize(d1)
        assertEquals("تاریخ جلالی ۱۴۰۳/۰۸/۱۰ باید ۱۰۰٪ بدون تغییر حفظ شود", "1403/08/10", norm1)

        val d2 = "03/08/10"
        val norm2 = PersianVoiceNormalizer.normalize(d2)
        assertEquals("تاریخ جلالی دورقمی ۰۳/۰۸/۱۰ باید حفظ شود", "03/08/10", norm2)

        val d3 = "1402/12/29"
        val norm3 = PersianVoiceNormalizer.normalize(d3)
        assertEquals("تاریخ جلالی ۱۴۰۲/۱۲/۲۹ باید حفظ شود", "1402/12/29", norm3)

        // ارقام فارسی
        val dPersian = "۱۴۰۳/۰۸/۱۰"
        val normPersian = PersianVoiceNormalizer.normalize(dPersian)
        assertEquals("ارقام فارسی باید به انگلیسی تبدیل شده ولی اسلش تاریخ حفظ شود", "1403/08/10", normPersian)

        val dShortPersian = "۰۳/۰۸/۱۰"
        val normShortPersian = PersianVoiceNormalizer.normalize(dShortPersian)
        assertEquals("تاریخ کوتاه با ارقام فارسی باید حفظ شود", "03/08/10", normShortPersian)
    }

    @Test
    fun test05_DecimalSlashAndMomayyez_CorrectlyNormalized() {
        // ۲. اعشار با اسلش باید به نقطه تبدیل شود
        val frac1 = "2/5"
        val norm1 = PersianVoiceNormalizer.normalize(frac1)
        assertEquals("کسر اعشاری ۲/۵ باید به 2.5 تبدیل شود", "2.5", norm1)

        val frac2 = "0/125"
        val norm2 = PersianVoiceNormalizer.normalize(frac2)
        assertEquals("کسر اعشاری ۰/۱۲۵ باید به 0.125 تبدیل شود", "0.125", norm2)

        // با ارقام فارسی
        val fracPersian = "۱۲/۵"
        val normPersian = PersianVoiceNormalizer.normalize(fracPersian)
        assertEquals("۱۲/۵ فارسی باید به 12.5 تبدیل شود", "12.5", normPersian)

        // ممیز فارسی ٫
        val momayyez = "0٫5"
        val normMomayyez = PersianVoiceNormalizer.normalize(momayyez)
        assertEquals("ممیز فارسی باید به نقطه اعشار تبدیل شود", "0.5", normMomayyez)

        val momayyezPersian = "۲٫۷۵"
        val normMomayyezPersian = PersianVoiceNormalizer.normalize(momayyezPersian)
        assertEquals("۲٫۷۵ باید به 2.75 تبدیل شود", "2.75", normMomayyezPersian)
    }

    @Test
    fun test06_JalaliDateAndDecimalCoexistingInSingleUtterance() {
        // ۳. حضور همزمان تاریخ و عدد اعشاری در یک جمله
        val utterance = "در تاریخ 1403/08/10 مقدار 2/5 کیلو دان مصرف شد"
        val norm = PersianVoiceNormalizer.normalize(utterance)
        assertTrue("تاریخ جلالی باید بدون دستکاری حفظ شود: $norm", norm.contains("1403/08/10"))
        assertTrue("کسر ۲/۵ باید به 2.5 تبدیل شود: $norm", norm.contains("2.5"))
        assertFalse("تاریخ نباید به نقطه تبدیل شود: $norm", norm.contains("1403.08.10"))
    }

    // =========================================================================
    // چالش ۳: استخراج سالن و عدم به دام افتادن مقادیر دان («دویست»)
    // =========================================================================

    @Test
    fun test07_HallExtraction_DoesNotCaptureDovistAsHallNumber() {
        // ۱. عبارت «برای سالن دویست کیلو دان ثبت کن»
        // کلمه «دویست» مقدار دان است و نباید به عنوان سالن ۲۰۰ استخراج شود.
        val command = "برای سالن دویست کیلو دان ثبت کن"

        val card = VoiceIntentParser.parseVoiceCommand(
            spokenText = command,
            userRole = UserRole.MANAGER,
            flocks = mockFlocks,
            contacts = emptyList()
        )

        assertNotNull("کارت عملیاتی باید ساخته شود", card)
        assertEquals("باید قصد ثبت دان تشخیص داده شود", VoiceIntentType.RECORD_FEED, card.intentType)

        // مقدار دان باید ۲۰۰ استخراج شده باشد
        val feedAmount = (card.parameters["amountKg"] as? Number)?.toDouble()
            ?: (card.parameters["amount"] as? Number)?.toDouble()
        assertEquals("مقدار دان باید ۲۰۰ کیلوگرم استخراج شود", 200.0, feedAmount ?: 0.0, 0.01)

        // بررسی شماره سالن
        // سالن نباید سالن ۲۰۰ باشد! سالن در ورودی صریح نیست یا گله پیش‌فرض انتخاب می‌شود
        assertNotEquals("شماره سالن نباید ۲۰۰ باشد!", 200L, card.flockId)
    }

    @Test
    fun test08_HallExtraction_ParseHallNumberString_DirectTesting() {
        // تست مستقیم تابع parseHallNumberString برای ۱ تا ۱۰۰ و اعداد نامعتبر
        assertEquals(1L, VoiceIntentParser.parseHallNumberString("یک"))
        assertEquals(1L, VoiceIntentParser.parseHallNumberString("اول"))
        assertEquals(2L, VoiceIntentParser.parseHallNumberString("دو"))
        assertEquals(10L, VoiceIntentParser.parseHallNumberString("ده"))
        assertEquals(20L, VoiceIntentParser.parseHallNumberString("بیست"))
        assertEquals(25L, VoiceIntentParser.parseHallNumberString("بیست و پنج"))
        assertEquals(50L, VoiceIntentParser.parseHallNumberString("پنجاه"))
        assertEquals(98L, VoiceIntentParser.parseHallNumberString("نود و هشت"))
        assertEquals(100L, VoiceIntentParser.parseHallNumberString("صد"))

        // ارقام عددی
        assertEquals(98L, VoiceIntentParser.parseHallNumberString("98"))
        assertEquals(100L, VoiceIntentParser.parseHallNumberString("100"))

        // مواردی که نباید شماره سالن شناخته شوند
        assertNull("دویست نباید شماره سالن شناخته شود", VoiceIntentParser.parseHallNumberString("دویست"))
        assertNull("سیصد نباید شماره سالن شناخته شود", VoiceIntentParser.parseHallNumberString("سیصد"))
        assertNull("هزار نباید شماره سالن شناخته شود", VoiceIntentParser.parseHallNumberString("هزار"))
        assertNull("۱۰۱ نباید شماره سالن شناخته شود", VoiceIntentParser.parseHallNumberString("101"))
        assertNull("۰ نباید شماره سالن شناخته شود", VoiceIntentParser.parseHallNumberString("0"))
    }

    @Test
    fun test09_HallExtraction_Salon98_MultiDigitAndCompound() {
        // ۲. تست «سالن نود و هشت»
        val commandWord = "ثبت ۲۰۰ کیلو دان سالن نود و هشت"
        val cardWord = VoiceIntentParser.parseVoiceCommand(
            spokenText = commandWord,
            userRole = UserRole.MANAGER,
            flocks = mockFlocks,
            contacts = emptyList()
        )

        assertNotNull(cardWord)
        assertEquals(VoiceIntentType.RECORD_FEED, cardWord.intentType)
        assertEquals("سالن نود و هشت باید به شناسه ۹۸ متصل شود", 98L, cardWord.flockId)

        // تست ارقام: «سالن ۹۸»
        val commandDigit = "ثبت ۵ تا تلفات در سالن ۹۸"
        val cardDigit = VoiceIntentParser.parseVoiceCommand(
            spokenText = commandDigit,
            userRole = UserRole.MANAGER,
            flocks = mockFlocks,
            contacts = emptyList()
        )

        assertNotNull(cardDigit)
        println("DEBUG_TEST09_ACTUAL: cardDigit.flockId=${cardDigit.flockId}, name=${cardDigit.flockName}")
        assertEquals(VoiceIntentType.RECORD_MORTALITY, cardDigit.intentType)
        assertEquals("سالن ۹۸ باید به شناسه ۹۸ متصل شود", 98L, cardDigit.flockId)
    }

    // =========================================================================
    // چالش تکمیلی: مرزهای ترکیبی و تست‌های استرس منفی
    // =========================================================================

    @Test
    fun test10_CompoundPackagingAndUnits_AdversarialIntegrity() {
        // ارزیابی کارتن، شانه، گونی
        val cartonCmd = "۱۰ کارتن تخم مرغ سالن ۲۵ ثبت کن"
        val cartonCard = VoiceIntentParser.parseVoiceCommand(
            spokenText = cartonCmd,
            userRole = UserRole.MANAGER,
            flocks = mockFlocks,
            contacts = emptyList()
        )
        assertNotNull(cartonCard)
        println("DEBUG_TEST10_ACTUAL: cartonCard.flockId=${cartonCard.flockId}, name=${cartonCard.flockName}")
        assertEquals(VoiceIntentType.RECORD_EGG, cartonCard.intentType)
        assertEquals(25L, cartonCard.flockId)
        val cartonAmount = (cartonCard.parameters["count"] as? Number)?.toDouble() ?: 0.0
        assertEquals("۱۰ کارتن باید ۱۸۰۰ عدد تخم‌مرغ باشد", 1800.0, cartonAmount, 0.01)

        val sackCmd = "۴ کیسه دان برای سالن نود و هشت ثبت کن"
        val sackCard = VoiceIntentParser.parseVoiceCommand(
            spokenText = sackCmd,
            userRole = UserRole.MANAGER,
            flocks = mockFlocks,
            contacts = emptyList()
        )
        assertNotNull(sackCard)
        assertEquals(VoiceIntentType.RECORD_FEED, sackCard.intentType)
        assertEquals(98L, sackCard.flockId)
        val sackAmount = (sackCard.parameters["amountKg"] as? Number)?.toDouble() ?: 0.0
        assertEquals("۴ کیسه دان ۵۰ کیلویی باید ۲۰۰ کیلوگرم باشد", 200.0, sackAmount, 0.01)
    }
}
