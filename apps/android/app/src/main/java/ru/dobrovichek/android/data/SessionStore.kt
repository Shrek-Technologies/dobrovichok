package ru.dobrovichek.android.data

import android.content.Context
import ru.dobrovichek.android.BuildConfig
import ru.dobrovichek.android.util.PersonNameFormat
import java.util.UUID

data class UserSession(
    val userId: String,
    val role: String,
    val firstName: String,
    val lastName: String,
    val patronymic: String?,
    val fullName: String,
    val phone: String
)

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun load(): UserSession? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val role = prefs.getString(KEY_ROLE, null) ?: return null
        var firstName = prefs.getString(KEY_FIRST_NAME, null).orEmpty()
        var lastName = prefs.getString(KEY_LAST_NAME, null).orEmpty()
        val patronymic = prefs.getString(KEY_PATRONYMIC, null)?.takeIf { it.isNotBlank() }
        val legacyFull = prefs.getString(KEY_FULL_NAME, null).orEmpty()
        if (firstName.isBlank() && lastName.isBlank() && legacyFull.isNotBlank()) {
            val parts = legacyFull.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            firstName = parts.getOrNull(0).orEmpty()
            lastName = parts.getOrNull(1).orEmpty()
        }
        val phone = prefs.getString(KEY_PHONE, "") ?: ""
        val fullName = PersonNameFormat.fullFormal(firstName, patronymic, lastName)
            .ifBlank { legacyFull.ifBlank { "$firstName $lastName".trim() } }
        return UserSession(userId, role, firstName, lastName, patronymic, fullName, phone)
    }

    fun save(session: UserSession) {
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ROLE, session.role)
            .putString(KEY_FIRST_NAME, session.firstName)
            .putString(KEY_LAST_NAME, session.lastName)
            .putString(KEY_PATRONYMIC, session.patronymic)
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
            firstName = "Гость",
            lastName = "",
            patronymic = null,
            fullName = "Гость",
            phone = BuildConfig.APPLICATION_ID
        )
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_ROLE = "role"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_PATRONYMIC = "patronymic"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_PHONE = "phone"
    }
}
