package com.example.poultryapp

import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.engine.FarmLiveContext
import com.example.poultryapp.voice.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MultiTurnSlotFillingTest {

    private lateinit var mockFlocks: List<Flock>
    private lateinit var mockContext: FarmLiveContext

    @Before
    fun setUp() {
        mockFlocks = listOf(
            Flock(
                id = 1L,
                name = "سالن ۱ گوشتی",
                species = BirdSpecies.CHICKEN,
                breed = "راس ۳۰۸",
                birthEpochMillis = System.currentTimeMillis() - 35L * 24 * 3600 * 1000,
                birdCount = 10000
            ),
            Flock(
                id = 2L,
                name = "گله ۲ تخمگذار",
                species = BirdSpecies.CHICKEN,
                breed = "لگهورن",
                birthEpochMillis = System.currentTimeMillis() - 150L * 24 * 3600 * 1000,
                birdCount = 5000
            )
        )
        mockContext = FarmLiveContext(flocks = mockFlocks)
    }

    @Test
    fun testCorrectionMidSentence_FeedRecording() {
        val input = "ثبت دان سالن ۱ دویست کیلو نه بکنش دویست و پنجاه کیلو"
        val state = AiSessionState()

        val turnResult = FarmConversationalAiEngine.processTurn(
            spokenText = input,
            currentState = state,
            flocks = mockFlocks,
            contacts = emptyList(),
            farmContext = mockContext
        )

        assertNotNull("Should create action card", turnResult.actionCard)
        assertEquals(VoiceIntentType.RECORD_FEED, turnResult.actionCard?.intentType)

        val amount = (turnResult.actionCard?.parameters?.get("amount") as? Double)
            ?: (turnResult.actionCard?.parameters?.get("amountKg") as? Double)
        assertEquals("Amount must be corrected to 250 kg instead of 200 kg", 250.0, amount ?: 0.0, 0.01)
    }

    @Test
    fun testMultiTurn_PromptAndFill() {
        val state0 = AiSessionState()

        // Turn 1: User says incomplete command
        val turn1 = FarmConversationalAiEngine.processTurn(
            spokenText = "ثبت دان",
            currentState = state0,
            flocks = mockFlocks,
            contacts = emptyList(),
            farmContext = mockContext
        )

        assertTrue("Turn 1 card should ask for more info or indicate incomplete data",
            turn1.actionCard?.needsMoreInfo == true || turn1.aiResponseText.contains("کیلو") || turn1.aiResponseText.contains("سالن"))

        // Turn 2: User provides missing information
        val state1 = turn1.updatedSessionState
        val turn2 = FarmConversationalAiEngine.processTurn(
            spokenText = "سالن ۲ سیصد کیلو",
            currentState = state1,
            flocks = mockFlocks,
            contacts = emptyList(),
            farmContext = mockContext
        )
        assertNotNull("Turn 2 should successfully generate action card", turn2.actionCard)
        assertEquals(VoiceIntentType.RECORD_FEED, turn2.actionCard?.intentType)
        val amount = (turn2.actionCard?.parameters?.get("amount") as? Double)
            ?: (turn2.actionCard?.parameters?.get("amountKg") as? Double)
        assertEquals(300.0, amount ?: 0.0, 0.01)
    }

    @Test
    fun testWhatIfScenarioIntegration() {
        val state = AiSessionState()
        val query = "اگر برق سالن قطع بشه فن ها خاموش بشن چی میشه؟"

        val turn = FarmConversationalAiEngine.processTurn(
            spokenText = query,
            currentState = state,
            flocks = mockFlocks,
            contacts = emptyList(),
            farmContext = mockContext
        )

        // سوالات سناریویی غیرعملیاتی مستقیماً به درگاه API آنلاین هدایت می‌شوند و کارت عملگر ایجاد نمی‌کنند
        assertEquals("ONLINE_API_KNOWLEDGE", turn.updatedSessionState.activeTopic)
        assertNull("Non-agentic advisory queries should not create mutating action cards", turn.actionCard)
        assertTrue(turn.aiResponseText.isNotBlank())
    }

    @Test
    fun testSemanticSearchIntegration() {
        val state = AiSessionState()
        val query = "پرنده ها دچار اسهال خونی شدن و مدفوعشون قرمزه چیکارشون کنم"

        val turn = FarmConversationalAiEngine.processTurn(
            spokenText = query,
            currentState = state,
            flocks = mockFlocks,
            contacts = emptyList(),
            farmContext = mockContext
        )

        // سوالات بالینی تخصصی به درگاه API ارجاع داده می‌شوند
        assertEquals("ONLINE_API_KNOWLEDGE", turn.updatedSessionState.activeTopic)
        assertNull("Knowledge diagnostic questions should route to AI API bridge", turn.actionCard)
        assertTrue(turn.aiResponseText.isNotBlank())
    }
}
