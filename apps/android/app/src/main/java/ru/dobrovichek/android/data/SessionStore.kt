package ru.dobrovichek.android.data

import android.content.Context
import ru.dobrovichek.android.BuildConfig
import java.util.UUID

data class UserSession(
    val userId: String,
    val role: String,
    val fullName: String,
    val phone: String
)

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun load(): UserSession? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val role = prefs.getString(KEY_ROLE, null) ?: return null
        val fullName = prefs.getString(KEY_FULL_NAME, "") ?: ""
        val phone = prefs.getString(KEY_PHONE, "") ?: ""
        return UserSession(userId, role, fullName, phone)
    }

    fun save(session: UserSession) {
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ROLE, session.role)
            .putString(KEY_FULL_NAME, session.fullName)
            .putString(KEY_PHONE, session.phone)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun currentOrFallbackWard(): UserSession {
        return load() ?: UserSession(
            userId = UUID.randomUUID().toString(),
            role = "WARD",
            fullName = "Гость",
            phone = BuildConfig.APPLICATION_ID
        )
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_ROLE = "role"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_PHONE = "phone"
    }
}
