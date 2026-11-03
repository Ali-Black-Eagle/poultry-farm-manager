package com.example.poultryapp

import com.example.poultryapp.voice.*
import org.junit.Assert.*
import org.junit.Test

class VoiceAssistantChatHistoryTest {

    @Test
    fun testSerializationAndTrimmingToMaxTen() {
        val sessions = (1..15).map { i ->
            SavedChatSession(
                id = "session-$i",
                title = "چت شماره $i",
                previewText = "پیش‌نمایش متن $i",
                timestamp = 1000L * i,
                activeFlockId = 1L,
                activeFlockName = "سالن ۱",
                turns = listOf(
                    SavedChatTurn(id = "turn-u-$i", sender = "USER", text = "سوال کاربر $i", timestamp = 1000L * i),
                    SavedChatTurn(id = "turn-a-$i", sender = "AI", text = "پاسخ هوش مصنوعی $i", timestamp = 1000L * i + 10)
                )
            )
        }

        val json = ChatHistoryCodec.serializeSessions(sessions)
        assertNotNull(json)
        assertTrue(json.contains("session-15"))

        val deserialized = ChatHistoryCodec.deserializeSessions(json)
        // باید دقیقاً به ۱۰ چت اخیر محدود شده باشد
        assertEquals(10, deserialized.size)
        // باید به ترتیب نزولی زمان باشد (جدیدترین اول)
        assertEquals("session-15", deserialized[0].id)
        assertEquals("session-14", deserialized[1].id)
        assertEquals("session-6", deserialized[9].id)

        // بررسی درستی اطلاعات چت اول
        val top = deserialized[0]
        assertEquals("چت شماره 15", top.title)
        assertEquals("سالن ۱", top.activeFlockName)
        assertEquals(2, top.turns.size)
        assertEquals("سوال کاربر 15", top.turns[0].text)
        assertEquals("USER", top.turns[0].sender)
        assertEquals("پاسخ هوش مصنوعی 15", top.turns[1].text)
        assertEquals("AI", top.turns[1].sender)
    }

    @Test
    fun testTitleExtraction() {
        val turns = listOf(
            ConversationTurn(
                sender = TurnSender.AI,
                text = "سلام! چطور کمکتون کنم؟"
            ),
            ConversationTurn(
                sender = TurnSender.USER,
                text = "وضعیت دمای سالن ۲ چطوره؟"
            )
        )
        val title = ChatHistoryCodec.extractTitle(turns)
        assertEquals("وضعیت دمای سالن ۲ چطوره؟", title)
    }
}
