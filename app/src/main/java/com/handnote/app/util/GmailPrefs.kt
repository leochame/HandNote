package com.handnote.app.util

import android.content.Context
import android.content.SharedPreferences

object GmailPrefs {
    private const val PREFS_NAME = "gmail_ai_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getGeminiApiKey(context: Context): String? {
        return prefs(context).getString(KEY_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun setGeminiApiKey(context: Context, apiKey: String?) {
        prefs(context).edit().putString(KEY_GEMINI_API_KEY, apiKey ?: "").apply()
    }
}

