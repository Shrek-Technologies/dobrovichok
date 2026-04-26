package ru.dobrovichek.android.data

import ru.dobrovichek.android.util.PersonNameFormat

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore
) {
    suspend fun register(
        firstName: String,
        lastName: String,
        patronymic: String?,
        phone: String,
        password: String,
        role: String
    ): UserSession {
        val response = authApi.register(
            RegisterPayload(
                firstName = firstName,
                lastName = lastName,
                patronymic = patronymic?.takeIf { it.isNotBlank() },
                phone = phone,
                password = password,
                role = role
            )
        )
        return response.toSession().also(sessionStore::save)
    }

    suspend fun login(phone: String, password: String): UserSession {
        val response = authApi.login(LoginPayload(phone = phone, password = password))
        return response.toSession().also(sessionStore::save)
    }

    fun currentSession(): UserSession? = sessionStore.load()

    fun logout() = sessionStore.clear()

    private fun AuthResponseDto.toSession(): UserSession {
        val pat = patronymic?.takeIf { it.isNotBlank() }
        return UserSession(
            userId = userId,
            role = role,
            firstName = firstName,
            lastName = lastName,
            patronymic = pat,
            fullName = fullName.ifBlank { PersonNameFormat.fullFormal(firstName, pat, lastName) },
            phone = phone
        )
    }
}
