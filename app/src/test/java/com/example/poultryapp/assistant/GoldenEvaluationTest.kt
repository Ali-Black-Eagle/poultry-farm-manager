package com.example.poultryapp.assistant

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.RouterDecision
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.router.AssistantRouter
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class GoldenEvaluationTest {

    private lateinit var router: AssistantRouter

    @Before
    fun setUp() {
        router = AssistantRouter()
    }

    @Test
    fun testPersianGoldenCorpus() {
        val file = File("src/main/assets/assistant/golden/fa.json")
        assertTrue("Golden FA asset file must exist", file.exists())
        val jsonArray = JSONArray(file.readText())

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val text = item.getString("text")
            val expectedIntent = IntentId.valueOf(item.getString("expectedIntent"))

            val decision = router.route(text)
            if (expectedIntent == IntentId.GENERAL_QUESTION) {
                assertTrue("For '$text', expected AskOnline but got $decision", decision is RouterDecision.AskOnline)
            } else {
                assertTrue("For '$text', expected Execute but got $decision", decision is RouterDecision.Execute)
                val exec = decision as RouterDecision.Execute
                assertEquals("For '$text', intent mismatch", expectedIntent, exec.intent)

                if (item.has("expectedSlots")) {
                    val slotsObj = item.getJSONObject("expectedSlots")
                    if (slotsObj.has("COUNT")) {
                        assertEquals("For '$text', COUNT slot mismatch", slotsObj.getInt("COUNT"), exec.slots[SlotId.COUNT]?.typedValue)
                    }
                    if (slotsObj.has("AMOUNT")) {
                        assertEquals("For '$text', AMOUNT slot mismatch", slotsObj.getDouble("AMOUNT"), (exec.slots[SlotId.AMOUNT]?.typedValue as Number).toDouble(), 0.001)
                    }
                }
            }
        }
    }

    @Test
    fun testEnglishGoldenCorpus() {
        val file = File("src/main/assets/assistant/golden/en.json")
        assertTrue("Golden EN asset file must exist", file.exists())
        val jsonArray = JSONArray(file.readText())

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val text = item.getString("text")
            val expectedIntent = IntentId.valueOf(item.getString("expectedIntent"))

            val decision = router.route(text)
            if (expectedIntent == IntentId.GENERAL_QUESTION) {
                assertTrue("For '$text', expected AskOnline but got $decision", decision is RouterDecision.AskOnline)
            } else {
                assertTrue("For '$text', expected Execute but got $decision", decision is RouterDecision.Execute)
                val exec = decision as RouterDecision.Execute
                assertEquals("For '$text', intent mismatch", expectedIntent, exec.intent)

                if (item.has("expectedSlots")) {
                    val slotsObj = item.getJSONObject("expectedSlots")
                    if (slotsObj.has("COUNT")) {
                        assertEquals("For '$text', COUNT slot mismatch", slotsObj.getInt("COUNT"), exec.slots[SlotId.COUNT]?.typedValue)
                    }
                    if (slotsObj.has("AMOUNT")) {
                        assertEquals("For '$text', AMOUNT slot mismatch", slotsObj.getDouble("AMOUNT"), (exec.slots[SlotId.AMOUNT]?.typedValue as Number).toDouble(), 0.001)
                    }
                }
            }
        }
    }
}

