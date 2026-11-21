package com.example.poultryapp.assistant

import com.example.poultryapp.assistant.core.dialogue.DialogueManager
import com.example.poultryapp.assistant.core.dialogue.DialogueResponse
import com.example.poultryapp.assistant.core.entities.EntityResolver
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.router.AssistantRouter
import com.example.poultryapp.data.local.BirdSaleLog
import com.example.poultryapp.data.local.BirdSlaughterLog
import com.example.poultryapp.data.local.FlockExitAnalytics
import com.example.poultryapp.data.local.FlockMortalityLog
import com.example.poultryapp.data.local.MortalityReason
import org.junit.Assert.*
import org.junit.Test

class BirdExitSeparationTest {

    @Test
    fun testMortalityReasonExcludesLiveSaleAndAllReasonsAreBiological() {
        val reasonNames = MortalityReason.entries.map { it.name }
        assertFalse("MortalityReason must not include LIVE_SALE", "LIVE_SALE" in reasonNames)
        assertTrue("All MortalityReason entries must represent biological mortality", MortalityReason.entries.all { it.isTrueMortality })
    }

    @Test
    fun testFlockExitAnalyticsCumulativeMortalityDenominatorAndYield() {
        // 1000 placed birds: 50 biological mortality, 600 sold, 200 slaughtered, 150 still alive
        val mortalityLogs = listOf(
            FlockMortalityLog(id = 1, flockId = 1, flockName = "سالن ۱", count = 30, reason = MortalityReason.DISEASE, dateJalaliStr = "1405/08/10"),
            FlockMortalityLog(id = 2, flockId = 1, flockName = "سالن ۱", count = 20, reason = MortalityReason.CULLING, dateJalaliStr = "1405/08/15")
        )
        val sales = listOf(
            BirdSaleLog(id = 1, flockId = 1, flockName = "سالن ۱", birdCount = 600, birdAgeDays = 40, liveWeightKg = 1440.0, totalRevenueTomans = 120_000_000.0, dateJalaliStr = "1405/08/18")
        )
        val slaughters = listOf(
            BirdSlaughterLog(id = 1, flockId = 1, flockName = "سالن ۱", birdCount = 200, birdAgeDays = 42, liveWeightKg = 500.0, carcassWeightKg = 365.0, dateJalaliStr = "1405/08/20")
        )

        val filteredBio = FlockExitAnalytics.filterBiologicalMortality(mortalityLogs)
        assertEquals(2, filteredBio.size)

        // Mortality rate MUST be 50 / (150 + 50 + 600 + 200) = 5.0%, NOT 50 / 200 = 25%
        val rate = FlockExitAnalytics.calculateCumulativeMortalityRatePercent(
            currentLiveBirds = 150,
            mortalityLogs = mortalityLogs,
            birdSales = sales,
            birdSlaughters = slaughters
        )
        assertEquals(5.0, rate, 0.001)

        // Carcass yield check: 365 / 500 = 73.0%
        assertEquals(73.0, slaughters.first().carcassYieldPercent!!, 0.001)
        assertNull(FlockExitAnalytics.calculateCarcassYieldPercent(0.0, 100.0))
        assertNull(FlockExitAnalytics.calculateCarcassYieldPercent(200.0, null))
    }

    @Test
    fun testCategoryResolverSkipsSlaughterAndDialogueConfirmationRules() {
        assertNull(
            "Slaughter command must not map to financial CHICKEN_SALES category",
            EntityResolver.resolveCategory("کشتار ۱۰۰ مرغ با وزن زنده ۲۴۰ کیلو")
        )

        val router = AssistantRouter()
        val singleFlock = listOf(1L to "سالن ۱")

        // Bird sale WITH money -> always shows confirmation card even via text
        val dmWithMoney = DialogueManager(router)
        val confirmResp = dmWithMoney.processInput("۵۰ مرغ فروختم ۱۵ میلیون", isVoice = false, availableFlocks = singleFlock)
        assertTrue("Expected ConfirmAction when sale includes money, got $confirmResp", confirmResp is DialogueResponse.ConfirmAction)
        assertEquals(IntentId.SELL_BIRDS, (confirmResp as DialogueResponse.ConfirmAction).intent)

        // Bird sale WITHOUT money via text -> ExecuteDirect
        val dmNoMoney = DialogueManager(router)
        val directSale = dmNoMoney.processInput("فروش ۵۰ مرغ", isVoice = false, availableFlocks = singleFlock)
        assertTrue("Expected ExecuteDirect for text sale without money, got $directSale", directSale is DialogueResponse.ExecuteDirect)

        // Slaughter via text -> ExecuteDirect; via voice -> ConfirmAction
        val dmSlaughterText = DialogueManager(router)
        val directSlaughter = dmSlaughterText.processInput("کشتار ۴۰ مرغ", isVoice = false, availableFlocks = singleFlock)
        assertTrue("Expected ExecuteDirect for text slaughter, got $directSlaughter", directSlaughter is DialogueResponse.ExecuteDirect)

        val dmSlaughterVoice = DialogueManager(router)
        val confirmSlaughter = dmSlaughterVoice.processInput("کشتار ۴۰ مرغ", isVoice = true, availableFlocks = singleFlock)
        assertTrue("Expected ConfirmAction for voice slaughter, got $confirmSlaughter", confirmSlaughter is DialogueResponse.ConfirmAction)
        val confirmSlaughterAction = confirmSlaughter as DialogueResponse.ConfirmAction
        assertEquals(IntentId.SLAUGHTER_BIRDS, confirmSlaughterAction.intent)
        assertEquals(40, confirmSlaughterAction.slots[SlotId.COUNT]?.typedValue)

        // English multi-flock bird sale & slaughter checks
        val twoEnglishFlocks = listOf(1L to "flock 1", 2L to "flock 2")
        val enSale = router.route("sold 40 birds from flock 1 for 500", twoEnglishFlocks)
        assertTrue("Expected Execute for EN bird sale with flock 1, got $enSale", enSale is com.example.poultryapp.assistant.core.nlu.RouterDecision.Execute)
        val enSaleExec = enSale as com.example.poultryapp.assistant.core.nlu.RouterDecision.Execute
        assertEquals(IntentId.SELL_BIRDS, enSaleExec.intent)
        assertEquals(1L, enSaleExec.slots[SlotId.FLOCK_ID]?.typedValue)
        assertEquals(40, enSaleExec.slots[SlotId.COUNT]?.typedValue)
        assertEquals(500.0, (enSaleExec.slots[SlotId.SALE_AMOUNT_TOMANS]?.typedValue as Number).toDouble(), 0.001)

        val enSlaughter = router.route("slaughter 60 birds live weight 150 kg carcass 110 kg", twoEnglishFlocks)
        assertTrue("Expected AskSlot(FLOCK_ID) for EN multi-flock slaughter, got $enSlaughter", enSlaughter is com.example.poultryapp.assistant.core.nlu.RouterDecision.AskSlot)
        val enSlaughterAsk = enSlaughter as com.example.poultryapp.assistant.core.nlu.RouterDecision.AskSlot
        assertEquals(IntentId.SLAUGHTER_BIRDS, enSlaughterAsk.partial.intent)
        assertEquals(SlotId.FLOCK_ID, enSlaughterAsk.slot)
        assertEquals(60, enSlaughterAsk.partial.slots[SlotId.COUNT]?.typedValue)
        assertEquals(150.0, (enSlaughterAsk.partial.slots[SlotId.LIVE_WEIGHT_KG]?.typedValue as Number).toDouble(), 0.001)
        assertEquals(110.0, (enSlaughterAsk.partial.slots[SlotId.CARCASS_WEIGHT_KG]?.typedValue as Number).toDouble(), 0.001)
    }
}

