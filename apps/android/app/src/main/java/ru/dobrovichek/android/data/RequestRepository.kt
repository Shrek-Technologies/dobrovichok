package ru.dobrovichek.android.data

class RequestRepository(
    private val requestApi: RequestApi
) {
    suspend fun createRequest(
        category: String,
        urgency: String,
        address: String,
        apartment: String,
        comment: String
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
                    latitude = 60.0092,
                    longitude = 30.3578
                )
            )
        )
        return response.id
    }

    suspend fun cancelRequest(requestId: String) {
        requestApi.cancelRequest(requestId)
    }
}
