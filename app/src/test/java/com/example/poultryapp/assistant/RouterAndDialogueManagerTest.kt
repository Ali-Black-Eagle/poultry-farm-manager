package com.example.poultryapp.assistant

import com.example.poultryapp.assistant.core.dialogue.DialogueManager
import com.example.poultryapp.assistant.core.dialogue.DialogueResponse
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.RouterDecision
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.router.AssistantRouter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RouterAndDialogueManagerTest {

    private lateinit var router: AssistantRouter
    private lateinit var dialogueManager: DialogueManager

    @Before
    fun setUp() {
        router = AssistantRouter()
        dialogueManager = DialogueManager(router)
    }

    @Test
    fun testAddEggsRoutingPersianAndEnglish() {
        val d1 = router.route("امروز ۱۲۰ تا تخم مرغ اضافه کن")
        assertTrue(d1 is RouterDecision.Execute)
        assertEquals(IntentId.ADD_EGGS, (d1 as RouterDecision.Execute).intent)
        assertEquals(120, d1.slots[SlotId.COUNT]?.typedValue)

        val d2 = router.route("add 95 eggs today")
        assertTrue(d2 is RouterDecision.Execute)
        assertEquals(IntentId.ADD_EGGS, (d2 as RouterDecision.Execute).intent)
        assertEquals(95, d2.slots[SlotId.COUNT]?.typedValue)

        val d3 = router.route("۵ شانه امروز")
        assertTrue(d3 is RouterDecision.Execute)
        assertEquals(IntentId.ADD_EGGS, (d3 as RouterDecision.Execute).intent)
        assertEquals(150, d3.slots[SlotId.COUNT]?.typedValue)
    }

    @Test
    fun testAddTransactionRouting() {
        val d1 = router.route("هزینه دان ۱ میلیون و ۵۰۰ هزار تومان ثبت کن")
        assertTrue(d1 is RouterDecision.Execute)
        assertEquals(IntentId.ADD_TRANSACTION, (d1 as RouterDecision.Execute).intent)
        assertEquals(1500000.0, (d1.slots[SlotId.AMOUNT]?.typedValue as Number).toDouble(), 0.001)

        val d2 = router.route("add expense 500 for feed")
        assertTrue(d2 is RouterDecision.Execute)
        assertEquals(IntentId.ADD_TRANSACTION, (d2 as RouterDecision.Execute).intent)
    }

    @Test
    fun testGeneralQuestionGuardDoesNotTriggerAddEggs() {
        // Must NEVER trigger ADD_EGGS despite containing 'تخم' or 'egg'
        val q1 = router.route("چرا مرغ‌ها تخم نمی‌گذارند؟")
        assertTrue("Expected AskOnline or GENERAL_QUESTION, got $q1", q1 is RouterDecision.AskOnline)

        val q2 = router.route("how do i increase egg production")
        assertTrue("Expected AskOnline or GENERAL_QUESTION, got $q2", q2 is RouterDecision.AskOnline)

        val q3 = router.route("علت شکستن پوسته تخم مرغ چیست")
        assertTrue("Expected AskOnline or GENERAL_QUESTION, got $q3", q3 is RouterDecision.AskOnline)
    }

    @Test
    fun testQueriesAndUtilities() {
        val qEggs = router.route("امروز چند تا تخم داشتیم؟")
        assertTrue(qEggs is RouterDecision.Execute)
        assertEquals(IntentId.QUERY_EGGS, (qEggs as RouterDecision.Execute).intent)

        val qFinance = router.route("این ماه چقدر خرج کردم؟")
        assertTrue(qFinance is RouterDecision.Execute)
        assertEquals(IntentId.QUERY_FINANCE, (qFinance as RouterDecision.Execute).intent)

        val undo = router.route("برگردون اشتباه شد")
        assertTrue(undo is RouterDecision.Execute)
        assertEquals(IntentId.UNDO_LAST, (undo as RouterDecision.Execute).intent)

        val help = router.route("راهنما")
        assertTrue(help is RouterDecision.Execute)
        assertEquals(IntentId.HELP, (help as RouterDecision.Execute).intent)

        val greet = router.route("سلام")
        assertTrue(greet is RouterDecision.Execute)
        assertEquals(IntentId.GREETING, (greet as RouterDecision.Execute).intent)
    }

    @Test
    fun testDialogueSlotFillingWorkflow() {
        // User says "ثبت تخم مرغ" with missing count
        val resp1 = dialogueManager.processInput("ثبت تخم مرغ", isVoice = false)
        assertTrue(resp1 is DialogueResponse.PromptSlot)
        assertEquals(SlotId.COUNT, (resp1 as DialogueResponse.PromptSlot).slot)

        // User follows up with just "120"
        val resp2 = dialogueManager.processInput("120", isVoice = false)
        assertTrue(resp2 is DialogueResponse.ExecuteDirect)
        assertEquals(IntentId.ADD_EGGS, (resp2 as DialogueResponse.ExecuteDirect).intent)
        assertEquals(120, resp2.slots[SlotId.COUNT]?.typedValue)
    }

    @Test
    fun testDialogueCancellation() {
        dialogueManager.processInput("ثبت تخم مرغ", isVoice = false)
        val cancelResp = dialogueManager.processInput("لغو کن", isVoice = false)
        assertTrue(cancelResp is DialogueResponse.Message)
        assertTrue((cancelResp as DialogueResponse.Message).textFa.contains("لغو شد"))
    }
}

