package ru.dobrovichek.android.data

class RequestRepository(
    private val requestApi: RequestApi
) {
    suspend fun createRequest(
        category: String,
        urgency: String,
        address: String,
        apartment: String,
        comment: String,
        latitude: Double,
        longitude: Double
    ): String {
        val description = buildString {
            append("Категория: ").append(category)
            append("\nСрочность: ").append(urgency)
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
                contactPhone = "+79990000000",
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
