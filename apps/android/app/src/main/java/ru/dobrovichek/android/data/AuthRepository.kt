package ru.dobrovichek.android.data

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore
) {
    suspend fun register(fullName: String, phone: String, password: String, role: String): UserSession {
        val response = authApi.register(
            RegisterPayload(
                fullName = fullName,
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
        return UserSession(
            userId = userId,
            role = role,
            fullName = fullName,
            phone = phone
        )
    }
}
