package com.example.poultryapp.assistant.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AssistantDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "assistant_local.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var instance: AssistantDatabaseHelper? = null

        fun getInstance(context: Context): AssistantDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AssistantDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE assistant_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                createdAt INTEGER NOT NULL,
                role TEXT NOT NULL,
                text TEXT NOT NULL,
                lang TEXT NOT NULL,
                intent TEXT,
                source TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE assistant_action_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                createdAt INTEGER NOT NULL,
                intent TEXT NOT NULL,
                paramsJson TEXT NOT NULL,
                resultToken TEXT,
                status TEXT NOT NULL,
                undone INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE assistant_learned_phrases (
                normalizedText TEXT PRIMARY KEY,
                intent TEXT NOT NULL,
                slotsTemplateJson TEXT NOT NULL,
                useCount INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE assistant_aliases (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL,
                aliasNormalized TEXT NOT NULL,
                targetId INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrations handled incrementally
    }

    // Messages operations
    fun insertMessage(msg: AssistantMessageEntity): Long {
        val cv = ContentValues().apply {
            put("createdAt", msg.createdAt)
            put("role", msg.role)
            put("text", msg.text)
            put("lang", msg.lang)
            put("intent", msg.intent)
            put("source", msg.source)
        }
        return writableDatabase.insert("assistant_messages", null, cv)
    }

    fun getAllMessages(): List<AssistantMessageEntity> {
        val list = mutableListOf<AssistantMessageEntity>()
        val cursor = readableDatabase.query("assistant_messages", null, null, null, null, null, "createdAt ASC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    AssistantMessageEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt")),
                        role = c.getString(c.getColumnIndexOrThrow("role")),
                        text = c.getString(c.getColumnIndexOrThrow("text")),
                        lang = c.getString(c.getColumnIndexOrThrow("lang")),
                        intent = c.getString(c.getColumnIndexOrThrow("intent")),
                        source = c.getString(c.getColumnIndexOrThrow("source"))
                    )
                )
            }
        }
        return list
    }

    fun clearMessages() {
        writableDatabase.delete("assistant_messages", null, null)
    }

    // Action log operations
    fun insertActionLog(log: AssistantActionLogEntity): Long {
        val cv = ContentValues().apply {
            put("createdAt", log.createdAt)
            put("intent", log.intent)
            put("paramsJson", log.paramsJson)
            put("resultToken", log.resultToken)
            put("status", log.status)
            put("undone", if (log.undone) 1 else 0)
        }
        return writableDatabase.insert("assistant_action_log", null, cv)
    }

    fun markActionUndone(id: Long) {
        val cv = ContentValues().apply { put("undone", 1) }
        writableDatabase.update("assistant_action_log", cv, "id = ?", arrayOf(id.toString()))
    }

    fun getLastAction(): AssistantActionLogEntity? {
        val cursor = readableDatabase.query(
            "assistant_action_log",
            null,
            "undone = 0",
            null,
            null,
            null,
            "createdAt DESC",
            "1"
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                AssistantActionLogEntity(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt")),
                    intent = c.getString(c.getColumnIndexOrThrow("intent")),
                    paramsJson = c.getString(c.getColumnIndexOrThrow("paramsJson")),
                    resultToken = c.getString(c.getColumnIndexOrThrow("resultToken")),
                    status = c.getString(c.getColumnIndexOrThrow("status")),
                    undone = c.getInt(c.getColumnIndexOrThrow("undone")) == 1
                )
            } else null
        }
    }

    // Learned phrases
    fun saveLearnedPhrase(phrase: AssistantLearnedPhraseEntity) {
        val cv = ContentValues().apply {
            put("normalizedText", phrase.normalizedText)
            put("intent", phrase.intent)
            put("slotsTemplateJson", phrase.slotsTemplateJson)
            put("useCount", phrase.useCount)
            put("createdAt", phrase.createdAt)
        }
        writableDatabase.insertWithOnConflict(
            "assistant_learned_phrases",
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun clearLearnedPhrases() {
        writableDatabase.delete("assistant_learned_phrases", null, null)
    }

    fun exportToJson(): org.json.JSONObject {
        val root = org.json.JSONObject()
        val db = readableDatabase

        // 1. messages
        val msgArr = org.json.JSONArray()
        val msgCursor = db.rawQuery("SELECT * FROM assistant_messages ORDER BY createdAt ASC", null)
        msgCursor.use { c ->
            while (c.moveToNext()) {
                val o = org.json.JSONObject().apply {
                    put("id", c.getLong(c.getColumnIndexOrThrow("id")))
                    put("createdAt", c.getLong(c.getColumnIndexOrThrow("createdAt")))
                    put("role", c.getString(c.getColumnIndexOrThrow("role")))
                    put("text", c.getString(c.getColumnIndexOrThrow("text")))
                    put("lang", c.getString(c.getColumnIndexOrThrow("lang")))
                    if (!c.isNull(c.getColumnIndexOrThrow("intent"))) put("intent", c.getString(c.getColumnIndexOrThrow("intent")))
                    put("source", c.getString(c.getColumnIndexOrThrow("source")))
                }
                msgArr.put(o)
            }
        }
        root.put("assistantMessages", msgArr)

        // 2. action logs
        val logArr = org.json.JSONArray()
        val logCursor = db.rawQuery("SELECT * FROM assistant_action_log ORDER BY createdAt ASC", null)
        logCursor.use { c ->
            while (c.moveToNext()) {
                val o = org.json.JSONObject().apply {
                    put("id", c.getLong(c.getColumnIndexOrThrow("id")))
                    put("createdAt", c.getLong(c.getColumnIndexOrThrow("createdAt")))
                    put("intent", c.getString(c.getColumnIndexOrThrow("intent")))
                    put("paramsJson", c.getString(c.getColumnIndexOrThrow("paramsJson")))
                    if (!c.isNull(c.getColumnIndexOrThrow("resultToken"))) put("resultToken", c.getString(c.getColumnIndexOrThrow("resultToken")))
                    put("status", c.getString(c.getColumnIndexOrThrow("status")))
                    put("undone", c.getInt(c.getColumnIndexOrThrow("undone")))
                }
                logArr.put(o)
            }
        }
        root.put("assistantActionLog", logArr)

        // 3. learned phrases
        val phraseArr = org.json.JSONArray()
        val phraseCursor = db.rawQuery("SELECT * FROM assistant_learned_phrases", null)
        phraseCursor.use { c ->
            while (c.moveToNext()) {
                val o = org.json.JSONObject().apply {
                    put("normalizedText", c.getString(c.getColumnIndexOrThrow("normalizedText")))
                    put("intent", c.getString(c.getColumnIndexOrThrow("intent")))
                    put("slotsTemplateJson", c.getString(c.getColumnIndexOrThrow("slotsTemplateJson")))
                    put("useCount", c.getInt(c.getColumnIndexOrThrow("useCount")))
                    put("createdAt", c.getLong(c.getColumnIndexOrThrow("createdAt")))
                }
                phraseArr.put(o)
            }
        }
        root.put("assistantLearnedPhrases", phraseArr)

        return root
    }

    fun importFromJson(root: org.json.JSONObject) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (root.has("assistantMessages")) {
                val arr = root.getJSONArray("assistantMessages")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("id", o.getLong("id"))
                        put("createdAt", o.getLong("createdAt"))
                        put("role", o.getString("role"))
                        put("text", o.getString("text"))
                        put("lang", o.getString("lang"))
                        if (o.has("intent") && !o.isNull("intent")) put("intent", o.getString("intent"))
                        put("source", o.getString("source"))
                    }
                    db.insertWithOnConflict("assistant_messages", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }

            if (root.has("assistantActionLog")) {
                val arr = root.getJSONArray("assistantActionLog")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("id", o.getLong("id"))
                        put("createdAt", o.getLong("createdAt"))
                        put("intent", o.getString("intent"))
                        put("paramsJson", o.getString("paramsJson"))
                        if (o.has("resultToken") && !o.isNull("resultToken")) put("resultToken", o.getString("resultToken"))
                        put("status", o.getString("status"))
                        put("undone", o.getInt("undone"))
                    }
                    db.insertWithOnConflict("assistant_action_log", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }

            if (root.has("assistantLearnedPhrases")) {
                val arr = root.getJSONArray("assistantLearnedPhrases")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("normalizedText", o.getString("normalizedText"))
                        put("intent", o.getString("intent"))
                        put("slotsTemplateJson", o.getString("slotsTemplateJson"))
                        put("useCount", o.getInt("useCount"))
                        put("createdAt", o.getLong("createdAt"))
                    }
                    db.insertWithOnConflict("assistant_learned_phrases", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

