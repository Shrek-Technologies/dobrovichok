package ru.dobrovichek.android.data

class UserRepository(private val userApi: UserApi) {
    /** Сохраняет ФИО и телефон из identity в user-service (иначе профиль волонтёра пустой). */
    suspend fun syncMyProfileAfterAuth(fullName: String, phone: String) {
        runCatching {
            userApi.updateMyProfile(
                UpdateMyProfilePayload(fullName = fullName.trim(), phone = phone.trim())
            )
        }
    }

    suspend fun getVolunteerContact(volunteerId: String): Pair<String?, String?> {
        val p = userApi.getVolunteerProfile(volunteerId)
        val name = p.fullName?.trim()?.takeIf { it.isNotEmpty() }
        val phone = p.phone?.trim()?.takeIf { it.isNotEmpty() }
        return name to phone
    }
}

