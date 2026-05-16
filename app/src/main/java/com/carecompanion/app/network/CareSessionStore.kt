package com.carecompanion.app.network

import android.content.Context

enum class CareRole { GUARDIAN, ELDER }

class CareSessionStore(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("care_session", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = p.getString(KEY_TOKEN, null)
        set(v) { p.edit().putString(KEY_TOKEN, v).apply() }

    var role: CareRole?
        get() = p.getString(KEY_ROLE, null)?.let { r ->
            runCatching { CareRole.valueOf(r) }.getOrNull()
        }
        set(v) { p.edit().putString(KEY_ROLE, v?.name).apply() }

    fun clear() {
        p.edit().remove(KEY_TOKEN).remove(KEY_ROLE).apply()
    }

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_ROLE = "role"
    }
}
