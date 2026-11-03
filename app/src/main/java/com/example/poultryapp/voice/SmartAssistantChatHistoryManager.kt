package com.example.poultryapp.voice

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * مدل داده هر نوبت ذخیره‌شده در تاریخچه چت
 */
data class SavedChatTurn(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER" یا "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * مدل داده هر نشست کامل چت در تاریخچه
 */
data class SavedChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val previewText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val activeFlockId: Long? = null,
    val activeFlockName: String? = null,
    val turns: List<SavedChatTurn> = emptyList()
)

/**
 * مبدل سبک و بدون وابستگی تاریخچه چت بر پایه JSON محلی اندروید
 */
object ChatHistoryCodec {
    private const val MAX_HISTORY_SESSIONS = 10

    fun serializeSessions(sessions: List<SavedChatSession>): String {
        val jsonArray = JSONArray()
        // حداکثر ۱۰ نشست اخیر
        val trimmed = sessions.sortedByDescending { it.timestamp }.take(MAX_HISTORY_SESSIONS)
        for (session in trimmed) {
            val sessionObj = JSONObject().apply {
                put("id", session.id)
                put("title", session.title)
                put("previewText", session.previewText)
                put("timestamp", session.timestamp)
                if (session.activeFlockId != null) put("activeFlockId", session.activeFlockId)
                if (session.activeFlockName != null) put("activeFlockName", session.activeFlockName)

                val turnsArray = JSONArray()
                for (turn in session.turns) {
                    val turnObj = JSONObject().apply {
                        put("id", turn.id)
                        put("sender", turn.sender)
                        put("text", turn.text)
                        put("timestamp", turn.timestamp)
                    }
                    turnsArray.put(turnObj)
                }
                put("turns", turnsArray)
            }
            jsonArray.put(sessionObj)
        }
        return jsonArray.toString()
    }

    fun deserializeSessions(json: String?): List<SavedChatSession> {
        if (json.isNullOrBlank()) return emptyList()
        val result = mutableListOf<SavedChatSession>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "گفتگو")
                val previewText = obj.optString("previewText", "")
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                val activeFlockId = if (obj.has("activeFlockId")) obj.getLong("activeFlockId") else null
                val activeFlockName = if (obj.has("activeFlockName")) obj.getString("activeFlockName") else null

                val turnsList = mutableListOf<SavedChatTurn>()
                val turnsArray = obj.optJSONArray("turns")
                if (turnsArray != null) {
                    for (j in 0 until turnsArray.length()) {
                        val turnObj = turnsArray.getJSONObject(j)
                        turnsList.add(
                            SavedChatTurn(
                                id = turnObj.optString("id", UUID.randomUUID().toString()),
                                sender = turnObj.optString("sender", "USER"),
                                text = turnObj.optString("text", ""),
                                timestamp = turnObj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }

                result.add(
                    SavedChatSession(
                        id = id,
                        title = title,
                        previewText = previewText,
                        timestamp = timestamp,
                        activeFlockId = activeFlockId,
                        activeFlockName = activeFlockName,
                        turns = turnsList
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return result.sortedByDescending { it.timestamp }.take(MAX_HISTORY_SESSIONS)
    }

    /**
     * استخراج عنوان هوشمند و کوتاه از اولین پیام کاربر
     */
    fun extractTitle(turns: List<ConversationTurn>): String {
        val firstUserTurn = turns.firstOrNull { it.sender == TurnSender.USER }
        val raw = firstUserTurn?.text?.trim() ?: "گفتگوی دستیار هوشمند"
        return if (raw.length > 36) {
            raw.substring(0, 36) + "..."
        } else {
            raw
        }
    }
}

/**
 * مدیر ماندگاری تاریخچه چت‌های اخیر در SharedPreferences
 */
object SmartAssistantChatHistoryManager {
    private const val PREFS_NAME = "smart_assistant_chat_history_prefs"
    private const val KEY_SESSIONS_JSON = "key_chat_sessions_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * دریافت لیست ۱۰ گفتگوی اخیر
     */
    fun getRecentSessions(context: Context): List<SavedChatSession> {
        val json = getPrefs(context).getString(KEY_SESSIONS_JSON, null)
        return ChatHistoryCodec.deserializeSessions(json)
    }

    /**
     * ذخیره یا به‌روزرسانی نشست چت جاری
     */
    fun saveSession(context: Context, session: SavedChatSession) {
        val existing = getRecentSessions(context).toMutableList()
        val index = existing.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            existing[index] = session
        } else {
            existing.add(0, session)
        }
        val serialized = ChatHistoryCodec.serializeSessions(existing)
        getPrefs(context).edit().putString(KEY_SESSIONS_JSON, serialized).apply()
    }

    /**
     * حذف یک نشست خاص از تاریخچه
     */
    fun deleteSession(context: Context, sessionId: String) {
        val existing = getRecentSessions(context).filter { it.id != sessionId }
        val serialized = ChatHistoryCodec.serializeSessions(existing)
        getPrefs(context).edit().putString(KEY_SESSIONS_JSON, serialized).apply()
    }

    /**
     * پاک‌سازی کامل تاریخچه
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().remove(KEY_SESSIONS_JSON).apply()
    }
}
