package ru.dobrovichek.android.data

class RequestRepository(
    private val requestApi: RequestApi
) {
    suspend fun createRequest(
        category: String,
        urgency: String,
        preferredTime: String?,
        address: String,
        apartment: String,
        comment: String,
        latitude: Double,
        longitude: Double,
        wardFirstName: String,
        wardLastName: String,
        wardPatronymic: String?,
        contactPhone: String
    ): String {
        val description = buildString {
            append("Категория: ").append(category)
            append("\nСрочность: ").append(urgency)
            preferredTime?.trim()?.takeIf { it.isNotEmpty() }?.let {
                append("\nУдобное время: ").append(it)
            }
            append("\nАдрес: ").append(address)
            if (apartment.isNotBlank()) {
                append(", кв. ").append(apartment)
            }
            if (comment.isNotBlank()) {
                append("\nКомментарий: ").append(comment)
            }
        }

        val response = requestApi.createRequest(
            CreateRequestPayload(
                description = description,
                contactPhone = contactPhone,
                wardFirstName = wardFirstName,
                wardLastName = wardLastName,
                wardPatronymic = wardPatronymic?.takeIf { it.isNotBlank() },
                location = GeoPointDto(
                    latitude = latitude,
                    longitude = longitude
                )
            )
        )
        return response.id
    }

    suspend fun cancelRequest(requestId: String) {
        requestApi.cancelRequest(requestId)
    }

    suspend fun completeRequest(requestId: String) {
        requestApi.completeRequest(requestId)
    }

    suspend fun abandonVolunteer(requestId: String) {
        requestApi.abandonVolunteer(requestId)
    }

    suspend fun findNearby(latitude: Double, longitude: Double, radiusKm: Double = 1.0): List<RequestSummaryDto> {
        return requestApi.findNearby(latitude = latitude, longitude = longitude, radiusKm = radiusKm)
    }

    suspend fun acceptRequest(requestId: String) {
        requestApi.acceptRequest(requestId)
    }

    suspend fun getRequest(requestId: String): RequestResponseDto {
        return requestApi.getById(requestId)
    }
}
