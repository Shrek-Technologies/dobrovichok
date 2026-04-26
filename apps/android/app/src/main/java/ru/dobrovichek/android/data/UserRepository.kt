package ru.dobrovichek.android.data

import ru.dobrovichek.android.util.PersonNameFormat

class UserRepository(private val userApi: UserApi) {
    suspend fun syncMyProfileAfterAuth(session: UserSession) {
        runCatching {
            userApi.updateMyProfile(
                UpdateMyProfilePayload(
                    firstName = session.firstName.trim(),
                    lastName = session.lastName.trim(),
                    patronymic = session.patronymic?.trim()?.takeIf { it.isNotEmpty() },
                    phone = session.phone.trim()
                )
            )
        }
    }

    suspend fun getVolunteerContact(volunteerId: String): Pair<String?, String?> {
        val p = userApi.getVolunteerProfile(volunteerId)
        val byParts = PersonNameFormat.volunteerForWard(p.firstName, p.lastName).takeIf { it.isNotEmpty() }
        val display = byParts
            ?: p.fullName?.trim()?.takeIf { it.isNotEmpty() }
        val phone = p.phone?.trim()?.takeIf { it.isNotEmpty() }
        return display to phone
    }
}
