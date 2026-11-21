package com.example.poultryapp.assistant

import com.example.poultryapp.assistant.core.dialogue.DialogueManager
import com.example.poultryapp.assistant.core.dialogue.DialogueResponse
import com.example.poultryapp.assistant.core.dialogue.DialogueState
import com.example.poultryapp.assistant.core.entities.EntityResolver
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.RouterDecision
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.assistant.core.router.AssistantRouter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MultiFlockAssistantTest {

    private lateinit var router: AssistantRouter
    private lateinit var dialogueManager: DialogueManager

    private val threeFlocks = listOf(
        1L to "سالن ۱",
        2L to "سالن ۲",
        3L to "گله بومی"
    )

    private val singleFlock = listOf(
        1L to "سالن ۱"
    )

    @Before
    fun setUp() {
        router = AssistantRouter()
        dialogueManager = DialogueManager(router)
    }

    @Test
    fun testEntityResolverSingleMultiAllAndNumericGuard() {
        // Exact single flock match across Persian and ASCII digits
        val singleAscii = EntityResolver.resolveAllFlocks("ثبت تخم مرغ برای سالن 2", threeFlocks)
        assertEquals(1, singleAscii.size)
        assertEquals(2L, singleAscii[0].flockId)
        assertEquals("سالن ۲", singleAscii[0].flockName)

        // Shorthand conjunctions like "سالن ۱ و ۲"
        val multiConj = EntityResolver.resolveAllFlocks("تخم مرغ سالن ۱ و ۲", threeFlocks)
        assertEquals(listOf(1L, 2L), multiConj.map { it.flockId })

        // Explicit all-flocks phrase
        val explicitAll = EntityResolver.resolveAllFlocks("برای همه گله‌ها ثبت کن", threeFlocks)
        assertEquals(listOf(1L, 2L, 3L), explicitAll.map { it.flockId })

        // Bare "همه" only resolves when allowBareAllKeyword = true (in follow-up selection mode)
        val bareDisallowed = EntityResolver.resolveAllFlocks("همه", threeFlocks, allowBareAllKeyword = false)
        assertTrue(bareDisallowed.isEmpty())
        val bareAllowed = EntityResolver.resolveAllFlocks("همه", threeFlocks, allowBareAllKeyword = true)
        assertEquals(listOf(1L, 2L, 3L), bareAllowed.map { it.flockId })

        // Critical guard: standalone numbers in quantities MUST NOT false-match "سالن ۲"
        val numericTrapEggs = EntityResolver.resolveAllFlocks("۲ شانه تخم مرغ امروز ثبت کن", threeFlocks)
        assertTrue("Numeric count '۲' must not match flock 'سالن ۲'", numericTrapEggs.isEmpty())

        val numericTrapMortality = EntityResolver.resolveAllFlocks("تلفات ۲ قطعه", threeFlocks)
        assertTrue("Numeric mortality count '۲' must not match flock 'سالن ۲'", numericTrapMortality.isEmpty())
    }

    @Test
    fun testRouterNeverAutoSelectsFlockWhenMultipleFlocksExist() {
        // 1. ADD_EGGS without flock name -> must ask for flock selection
        val eggsDecision = router.route("امروز ۱۲۰ تا تخم مرغ ثبت کن", threeFlocks)
        assertTrue("Expected AskSlot for ADD_EGGS, got $eggsDecision", eggsDecision is RouterDecision.AskSlot)
        val askEggs = eggsDecision as RouterDecision.AskSlot
        assertEquals(SlotId.FLOCK_ID, askEggs.slot)
        assertEquals(IntentId.ADD_EGGS, askEggs.partial.intent)
        assertEquals(120, askEggs.partial.slots[SlotId.COUNT]?.typedValue)
        assertNull(askEggs.partial.slots[SlotId.FLOCK_ID])

        // 2. ADD_FEED_WATER without flock name -> must ask for flock selection
        val feedDecision = router.route("شارژ دان ۱۰۰ کیلو", threeFlocks)
        assertTrue("Expected AskSlot for ADD_FEED_WATER, got $feedDecision", feedDecision is RouterDecision.AskSlot)
        val askFeed = feedDecision as RouterDecision.AskSlot
        assertEquals(SlotId.FLOCK_ID, askFeed.slot)
        assertEquals(IntentId.ADD_FEED_WATER, askFeed.partial.intent)
        assertEquals(100, askFeed.partial.slots[SlotId.COUNT]?.typedValue)
        assertEquals("FEED", askFeed.partial.slots[SlotId.FEED_WATER_TYPE]?.typedValue)

        // 3. LOG_MORTALITY without flock name -> must ask for flock selection
        val mortDecision = router.route("تلفات ۵ قطعه به علت بیماری", threeFlocks)
        assertTrue("Expected AskSlot for LOG_MORTALITY, got $mortDecision", mortDecision is RouterDecision.AskSlot)
        val askMort = mortDecision as RouterDecision.AskSlot
        assertEquals(SlotId.FLOCK_ID, askMort.slot)
        assertEquals(IntentId.LOG_MORTALITY, askMort.partial.intent)
        assertEquals(5, askMort.partial.slots[SlotId.COUNT]?.typedValue)
    }

    @Test
    fun testRouterPreservesCountWhenFlockNameContainsNumbers() {
        val decision1 = router.route("ثبت ۱۲۰ تخم مرغ برای سالن ۲", threeFlocks)
        assertTrue("Expected Execute when single flock explicitly named, got $decision1", decision1 is RouterDecision.Execute)
        val exec1 = decision1 as RouterDecision.Execute
        assertEquals(IntentId.ADD_EGGS, exec1.intent)
        assertEquals(2L, exec1.slots[SlotId.FLOCK_ID]?.typedValue)
        assertEquals("سالن ۲", exec1.slots[SlotId.FLOCK_NAME]?.typedValue)
        assertEquals(120, exec1.slots[SlotId.COUNT]?.typedValue)

        // Reverse word order where "سالن ۲" comes BEFORE the actual egg count
        val decision2 = router.route("برای سالن ۲ امروز ۱۵۰ تخم مرغ ثبت کن", threeFlocks)
        assertTrue("Expected Execute when flock named before count, got $decision2", decision2 is RouterDecision.Execute)
        val exec2 = decision2 as RouterDecision.Execute
        assertEquals(IntentId.ADD_EGGS, exec2.intent)
        assertEquals(2L, exec2.slots[SlotId.FLOCK_ID]?.typedValue)
        assertEquals(150, exec2.slots[SlotId.COUNT]?.typedValue)

        // With water charge and flock digit before amount
        val waterDecision = router.route("برای سالن ۱ امروز ۲۰۰ لیتر آب شارژ کن", threeFlocks)
        assertTrue("Expected Execute for ADD_FEED_WATER, got $waterDecision", waterDecision is RouterDecision.Execute)
        val execWater = waterDecision as RouterDecision.Execute
        assertEquals(IntentId.ADD_FEED_WATER, execWater.intent)
        assertEquals(1L, execWater.slots[SlotId.FLOCK_ID]?.typedValue)
        assertEquals(200, execWater.slots[SlotId.COUNT]?.typedValue)
    }

    @Test
    fun testRouterMultipleFlocksNamedRequestsExactAllocations() {
        val d1 = router.route("ثبت ۱۲۰ تخم مرغ برای سالن ۱ و ۲", threeFlocks)
        assertTrue("Expected AskSlot(FLOCK_ALLOCATIONS), got $d1", d1 is RouterDecision.AskSlot)
        val ask1 = d1 as RouterDecision.AskSlot
        assertEquals(SlotId.FLOCK_ALLOCATIONS, ask1.slot)
        assertEquals(120, ask1.partial.slots[SlotId.COUNT]?.typedValue)
        val selected = ask1.partial.slots[SlotId.SELECTED_FLOCK_IDS]?.typedValue as? List<*>
        assertEquals(listOf(1L, 2L), selected)

        val dAll = router.route("شارژ دان ۱۰۰ کیلو برای همه گله‌ها", threeFlocks)
        assertTrue("Expected AskSlot(FLOCK_ALLOCATIONS), got $dAll", dAll is RouterDecision.AskSlot)
        val askAll = dAll as RouterDecision.AskSlot
        assertEquals(SlotId.FLOCK_ALLOCATIONS, askAll.slot)
        val selectedAll = askAll.partial.slots[SlotId.SELECTED_FLOCK_IDS]?.typedValue as? List<*>
        assertEquals(listOf(1L, 2L, 3L), selectedAll)
    }

    @Test
    fun testRouterAllowsDirectExecutionWhenSingleFlockExists() {
        val decision = router.route("امروز ۱۲۰ تا تخم مرغ ثبت کن", singleFlock)
        assertTrue("Single-flock farm should still execute directly, got $decision", decision is RouterDecision.Execute)
        val exec = decision as RouterDecision.Execute
        assertEquals(IntentId.ADD_EGGS, exec.intent)
        assertEquals(120, exec.slots[SlotId.COUNT]?.typedValue)
    }

    @Test
    fun testDialogueManagerSelectFlocksAndFollowUpTransitions() {
        // Start with ADD_EGGS and count, no flock -> enters SelectingFlocks with empty preselection
        val r1 = dialogueManager.processInput("امروز ۱۲۰ تا تخم مرغ ثبت کن", isVoice = false, availableFlocks = threeFlocks)
        assertTrue("Expected SelectFlocks, got $r1", r1 is DialogueResponse.SelectFlocks)
        val select1 = r1 as DialogueResponse.SelectFlocks
        assertEquals(IntentId.ADD_EGGS, select1.intent)
        assertTrue("Preselection must be empty by default", select1.preselectedFlockIds.isEmpty())
        assertEquals(120, select1.slots[SlotId.COUNT]?.typedValue)
        assertTrue(dialogueManager.state is DialogueState.SelectingFlocks)

        // Follow up with multiple flocks ("سالن ۱ و ۲") -> stays in SelectingFlocks with updated preselection
        val r2 = dialogueManager.processInput("سالن ۱ و ۲", isVoice = false, availableFlocks = threeFlocks)
        assertTrue("Expected SelectFlocks update for multiple flocks, got $r2", r2 is DialogueResponse.SelectFlocks)
        val select2 = r2 as DialogueResponse.SelectFlocks
        assertEquals(setOf(1L, 2L), select2.preselectedFlockIds)
        assertEquals(120, select2.slots[SlotId.COUNT]?.typedValue)

        // Follow up with single flock ("گله بومی") -> completes immediately with COUNT preserved
        val r3 = dialogueManager.processInput("گله بومی", isVoice = false, availableFlocks = threeFlocks)
        assertTrue("Expected ExecuteDirect after picking 1 flock, got $r3", r3 is DialogueResponse.ExecuteDirect)
        val exec = r3 as DialogueResponse.ExecuteDirect
        assertEquals(IntentId.ADD_EGGS, exec.intent)
        assertEquals(3L, exec.slots[SlotId.FLOCK_ID]?.typedValue)
        assertEquals(120, exec.slots[SlotId.COUNT]?.typedValue)
        assertTrue(dialogueManager.state is DialogueState.Idle)
    }

    @Test
    fun testDialogueManagerPostSlotFillingEnforcesFlockSelectionAndCancel() {
        // Follow-up starting without count AND without flock -> SelectFlocks first, then count can follow
        val r1 = dialogueManager.processInput("ثبت تخم مرغ", isVoice = false, availableFlocks = threeFlocks)
        assertTrue(r1 is DialogueResponse.SelectFlocks)

        // User picks a single flock by name when COUNT is still missing -> prompts for COUNT of that flock
        val r2 = dialogueManager.processInput("سالن ۲", isVoice = true, availableFlocks = threeFlocks)
        assertTrue("Expected PromptSlot(COUNT) after naming 1 flock without count, got $r2", r2 is DialogueResponse.PromptSlot)
        assertEquals(SlotId.COUNT, (r2 as DialogueResponse.PromptSlot).slot)

        // User supplies COUNT via voice -> ConfirmAction (voice + eggs confirmation gate) with flock preserved
        val r3 = dialogueManager.processInput("۹۵ تا", isVoice = true, availableFlocks = threeFlocks)
        assertTrue("Expected ConfirmAction, got $r3", r3 is DialogueResponse.ConfirmAction)
        val confirm = r3 as DialogueResponse.ConfirmAction
        assertEquals(2L, confirm.slots[SlotId.FLOCK_ID]?.typedValue)
        assertEquals(95, confirm.slots[SlotId.COUNT]?.typedValue)

        // Cancellation works cleanly from SelectingFlocks too
        val dm2 = DialogueManager(router)
        val beginResp = dm2.beginFlockSelection(
            intent = IntentId.LOG_MORTALITY,
            slots = mapOf(SlotId.COUNT to SlotValue("5", 5)),
            availableFlocks = threeFlocks
        )
        assertEquals(IntentId.LOG_MORTALITY, beginResp.intent)
        assertTrue(dm2.state is DialogueState.SelectingFlocks)

        val cancelResp = dm2.processInput("لغو", isVoice = false, availableFlocks = threeFlocks)
        assertTrue(cancelResp is DialogueResponse.Message)
        assertTrue(dm2.state is DialogueState.Idle)
    }

    @Test
    fun testMultiFlockSelectionEnforcedForBirdSaleAndSlaughter() {
        // 1. SELL_BIRDS without flock -> AskSlot(FLOCK_ID) preserving count and money
        val saleNoFlock = router.route("۵۰ مرغ فروختم ۱۵ میلیون تومان", threeFlocks)
        assertTrue("Expected AskSlot for SELL_BIRDS, got $saleNoFlock", saleNoFlock is RouterDecision.AskSlot)
        val askSale = saleNoFlock as RouterDecision.AskSlot
        assertEquals(SlotId.FLOCK_ID, askSale.slot)
        assertEquals(IntentId.SELL_BIRDS, askSale.partial.intent)
        assertEquals(50, askSale.partial.slots[SlotId.COUNT]?.typedValue)
        assertEquals(15000000.0, (askSale.partial.slots[SlotId.SALE_AMOUNT_TOMANS]?.typedValue as Number).toDouble(), 0.01)
        assertNull(askSale.partial.slots[SlotId.FLOCK_ID])

        // 2. SELL_BIRDS with explicit single flock -> Execute immediately with flock resolved
        val saleFlock1 = router.route("۵۰ مرغ سالن ۱ فروختم ۱۵ میلیون", threeFlocks)
        assertTrue("Expected Execute for named flock sale, got $saleFlock1", saleFlock1 is RouterDecision.Execute)
        val execSale1 = saleFlock1 as RouterDecision.Execute
        assertEquals(IntentId.SELL_BIRDS, execSale1.intent)
        assertEquals(1L, execSale1.slots[SlotId.FLOCK_ID]?.typedValue)
        assertEquals(50, execSale1.slots[SlotId.COUNT]?.typedValue)
        assertEquals(15000000.0, (execSale1.slots[SlotId.SALE_AMOUNT_TOMANS]?.typedValue as Number).toDouble(), 0.01)

        // 3. SELL_BIRDS with multiple flocks named -> AskSlot(FLOCK_ALLOCATIONS)
        val saleMulti = router.route("فروش ۶۰ مرغ از سالن ۱ و ۲ به مبلغ ۱۸ میلیون", threeFlocks)
        assertTrue("Expected AskSlot(FLOCK_ALLOCATIONS), got $saleMulti", saleMulti is RouterDecision.AskSlot)
        assertEquals(SlotId.FLOCK_ALLOCATIONS, (saleMulti as RouterDecision.AskSlot).slot)

        // 4. SLAUGHTER_BIRDS without flock -> DialogueManager SelectFlocks preserving weights
        val dmSlaughter = DialogueManager(router)
        val r1 = dmSlaughter.processInput(
            "کشتار ۱۰۰ مرغ با وزن زنده ۲۴۰ کیلو و وزن لاشه ۱۷۵ کیلو",
            isVoice = false,
            availableFlocks = threeFlocks
        )
        assertTrue("Expected SelectFlocks for SLAUGHTER_BIRDS, got $r1", r1 is DialogueResponse.SelectFlocks)
        val sel1 = r1 as DialogueResponse.SelectFlocks
        assertEquals(IntentId.SLAUGHTER_BIRDS, sel1.intent)
        assertEquals(100, sel1.slots[SlotId.COUNT]?.typedValue)
        assertEquals(240.0, (sel1.slots[SlotId.LIVE_WEIGHT_KG]?.typedValue as Number).toDouble(), 0.01)
        assertEquals(175.0, (sel1.slots[SlotId.CARCASS_WEIGHT_KG]?.typedValue as Number).toDouble(), 0.01)

        // Picking one flock in follow-up executes directly for text and preserves weights
        val r2 = dmSlaughter.processInput("گله بومی", isVoice = false, availableFlocks = threeFlocks)
        assertTrue("Expected ExecuteDirect after picking 1 flock, got $r2", r2 is DialogueResponse.ExecuteDirect)
        val execSl = r2 as DialogueResponse.ExecuteDirect
        assertEquals(IntentId.SLAUGHTER_BIRDS, execSl.intent)
        assertEquals(3L, execSl.slots[SlotId.FLOCK_ID]?.typedValue)
        assertEquals(100, execSl.slots[SlotId.COUNT]?.typedValue)
        assertEquals(240.0, (execSl.slots[SlotId.LIVE_WEIGHT_KG]?.typedValue as Number).toDouble(), 0.01)
        assertEquals(175.0, (execSl.slots[SlotId.CARCASS_WEIGHT_KG]?.typedValue as Number).toDouble(), 0.01)
    }
}

