package ru.dobrovichek.android.data

import kotlinx.coroutines.delay
import retrofit2.HttpException
import ru.dobrovichek.android.util.PersonNameFormat

class UserRepository(private val userApi: UserApi) {
    suspend fun registerDevice(fcmToken: String) {
        userApi.registerDevice(RegisterDevicePayload(fcmToken = fcmToken))
    }

    suspend fun unregisterDevice() {
        userApi.registerDevice(RegisterDevicePayload(fcmToken = null))
    }

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

    suspend fun submitVolunteerRating(volunteerId: String, requestId: String, score: Int) {
        val payload = CreateVolunteerRatingPayload(requestId = requestId, score = score)
        var waitMs = 350L
        repeat(15) { attempt ->
            try {
                userApi.createVolunteerRating(volunteerId, payload)
                return
            } catch (e: HttpException) {
                if (e.code() != 409) throw e
                val body = e.response()?.errorBody()?.use { it.string() }.orEmpty()
                if (body.contains("already exists", ignoreCase = true)) throw e
                if (attempt == 14) throw e
                delay(waitMs)
                waitMs = minOf(waitMs * 4 / 3, 2_000L)
            }
        }
    }

    suspend fun getVolunteerFirstNameForRating(volunteerId: String): String {
        val p = userApi.getVolunteerProfile(volunteerId)
        val first = p.firstName?.trim()?.takeIf { it.isNotEmpty() }
        if (first != null) return first
        val fromFull = p.fullName?.trim()?.substringBefore(' ')?.trim()?.takeIf { it.isNotEmpty() }
        return fromFull ?: "волонтёра"
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
